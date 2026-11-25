package org.dromara.system.controller.opc;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.OpcDesktopHeartbeatBo;
import org.dromara.system.domain.bo.OpcDesktopLoginBo;
import org.dromara.system.domain.bo.OpcDesktopRegisterBo;
import org.dromara.system.domain.vo.OpcCoworkerVo;
import org.dromara.system.domain.vo.OpcDesktopCoworkerPackageVo;
import org.dromara.system.domain.vo.OpcDesktopUserVo;
import org.dromara.system.domain.vo.OpcIndustryAppVo;
import org.dromara.system.domain.vo.OpcMcpAccessVo;
import org.dromara.system.service.IOpcCoworkerService;
import org.dromara.system.service.IOpcDesktopDeviceService;
import org.dromara.system.service.IOpcDesktopMcpProxyService;
import org.dromara.system.service.IOpcDesktopUserService;
import org.dromara.system.service.IOpcIndustryAppService;
import org.dromara.system.service.impl.OpcDesktopUserServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 桌面端：注册/心跳/账号密码登录（与后台 sys_user 隔离）
 */
@SaIgnore
@RequiredArgsConstructor
@RestController
@RequestMapping("/opc/desktop")
public class OpcDesktopController extends BaseController {

    private final IOpcDesktopDeviceService desktopDeviceService;
    private final IOpcDesktopUserService desktopUserService;
    private final IOpcIndustryAppService industryAppService;
    private final IOpcCoworkerService coworkerService;
    private final IOpcDesktopMcpProxyService desktopMcpProxyService;

    /**
     * 设备注册，返回 deviceToken
     */
    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody OpcDesktopRegisterBo bo) {
        return R.ok(desktopDeviceService.register(bo));
    }

    /**
     * 设备心跳
     */
    @PostMapping("/heartbeat")
    public R<Void> heartbeat(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestBody(required = false) OpcDesktopHeartbeatBo bo
    ) {
        String token = resolveBearer(authorization);
        desktopDeviceService.heartbeat(token, bo == null ? new OpcDesktopHeartbeatBo() : bo);
        return R.ok();
    }

    /**
     * WorkBench 账号密码登录（独立桌面用户表，无验证码）
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody OpcDesktopLoginBo bo) {
        return R.ok(desktopUserService.login(bo));
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        desktopUserService.logout(resolveBearer(authorization));
        return R.ok();
    }

    /**
     * 当前登录用户
     */
    @GetMapping("/me")
    public R<OpcDesktopUserVo> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return R.ok(desktopUserService.currentUser(resolveBearer(authorization)));
    }

    /**
     * 行业应用清单（启用项全量返回；accessible = 登录且已订阅，public 除外）
     */
    @GetMapping("/industry-apps")
    public R<List<OpcIndustryAppVo>> industryApps(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = resolveBearer(authorization);
        boolean loggedIn = isDesktopLoggedIn(token);
        var appIds = loggedIn ? desktopUserService.listSubscribedAppIds(token) : java.util.Set.<Long>of();
        return R.ok(industryAppService.listForDesktop(loggedIn, appIds));
    }

    /**
     * 同事目录（启用项全量；accessible 按 visibility / 登录 / 订阅）
     */
    @GetMapping("/coworkers")
    public R<List<OpcCoworkerVo>> coworkers(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = resolveBearer(authorization);
        boolean loggedIn = isDesktopLoggedIn(token);
        Set<Long> coworkerIds = loggedIn ? desktopUserService.listSubscribedCoworkerIds(token) : Set.of();
        return R.ok(coworkerService.listForDesktop(loggedIn, coworkerIds));
    }

    /**
     * 下载同事包（需登录且 accessible）
     */
    @GetMapping("/coworkers/{personaKey}/package")
    public R<OpcDesktopCoworkerPackageVo> coworkerPackage(
        @PathVariable String personaKey,
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = resolveBearer(authorization);
        Long userId = desktopUserService.resolveUserId(token);
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        Set<Long> coworkerIds = desktopUserService.listSubscribedCoworkerIds(token);
        return R.ok(coworkerService.getPackageForDesktop(personaKey, userId, coworkerIds));
    }

    /**
     * MCP 访问元数据：proxyUrl + allowTools（不含网关 Key）
     */
    @GetMapping("/mcp-access")
    public R<OpcMcpAccessVo> mcpAccess(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        Long userId = desktopUserService.resolveUserId(resolveBearer(authorization));
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        return R.ok(desktopMcpProxyService.buildMcpAccess(userId));
    }

    /**
     * MCP 代理：桌面带登录 Token 调用；服务端校验授权后用组织 Key 转发网关
     */
    @PostMapping(value = "/mcp", consumes = MediaType.ALL_VALUE)
    public void mcpProxy(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestHeader(value = "Accept", required = false) String accept,
        @RequestBody(required = false) String body,
        HttpServletResponse response
    ) throws IOException {
        Long userId = desktopUserService.resolveUserId(resolveBearer(authorization));
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        String result = desktopMcpProxyService.proxy(userId, body, accept);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(result == null ? "{}" : result);
    }

    private boolean isDesktopLoggedIn(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            return false;
        }
        return RedisUtils.hasKey(OpcDesktopUserServiceImpl.USER_TOKEN_KEY_PREFIX + accessToken);
    }

    private String resolveBearer(String authorization) {
        if (StringUtils.isBlank(authorization)) {
            return null;
        }
        String value = authorization.trim();
        if (StringUtils.startsWithIgnoreCase(value, "Bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }
}
