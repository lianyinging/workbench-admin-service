package org.dromara.system.controller.opc;

import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.OpcDesktopHeartbeatBo;
import org.dromara.system.domain.bo.OpcDesktopLoginBo;
import org.dromara.system.domain.bo.OpcDesktopRegisterBo;
import org.dromara.system.domain.vo.OpcDesktopUserVo;
import org.dromara.system.domain.vo.OpcIndustryAppVo;
import org.dromara.system.service.IOpcDesktopDeviceService;
import org.dromara.system.service.IOpcDesktopUserService;
import org.dromara.system.service.IOpcIndustryAppService;
import org.dromara.system.service.impl.OpcDesktopUserServiceImpl;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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
