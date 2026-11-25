package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcCoworker;
import org.dromara.system.domain.OpcDesktopUserCoworkerTool;
import org.dromara.system.domain.OpcMcpGatewayConfig;
import org.dromara.system.domain.vo.OpcMcpAccessVo;
import org.dromara.system.domain.vo.OpcMcpAllowToolVo;
import org.dromara.system.mapper.OpcCoworkerMapper;
import org.dromara.system.mapper.OpcDesktopUserCoworkerToolMapper;
import org.dromara.system.service.IOpcDesktopMcpProxyService;
import org.dromara.system.service.IOpcMcpGatewayConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OpcDesktopMcpProxyServiceImpl implements IOpcDesktopMcpProxyService {

    private static final String DEFAULT_TENANT = "000000";
    private static final String PROXY_PATH = "/opc/desktop/mcp";

    private final OpcDesktopUserCoworkerToolMapper userCoworkerToolMapper;
    private final OpcCoworkerMapper coworkerMapper;
    private final IOpcMcpGatewayConfigService gatewayConfigService;

    @Override
    public OpcMcpAccessVo buildMcpAccess(Long userId) {
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcMcpAccessVo vo = new OpcMcpAccessVo();
            vo.setProxyUrl(PROXY_PATH);
            try {
                OpcMcpGatewayConfig cfg = gatewayConfigService.requireRuntimeConfig();
                vo.setVirtualServerId(cfg.getVirtualServerId());
            } catch (ServiceException ignored) {
                // 未配置网关时仍返回代理地址与空白名单
            }
            vo.setAllowTools(listAllowTools(userId));
            return vo;
        });
    }

    @Override
    public String proxy(Long userId, String body, String accept) {
        if (userId == null) {
            throw new ServiceException("未登录", 401);
        }
        if (StringUtils.isBlank(body)) {
            throw new ServiceException("请求体不能为空");
        }
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> doProxy(userId, body, accept));
    }

    private String doProxy(Long userId, String body, String accept) {
        Set<String> allowedToolNames = listAllowTools(userId).stream()
            .map(OpcMcpAllowToolVo::getToolName)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toCollection(HashSet::new));

        JSONObject req;
        try {
            req = JSONUtil.parseObj(body);
        } catch (Exception ex) {
            throw new ServiceException("MCP 请求体必须是 JSON");
        }
        String method = req.getStr("method");
        if ("tools/call".equals(method) || "tools/invoke".equals(method)) {
            String toolName = extractCallToolName(req);
            if (StringUtils.isBlank(toolName) || !allowedToolNames.contains(toolName)) {
                throw new ServiceException("未授权调用工具：" + StrUtil.blankToDefault(toolName, "?"), 403);
            }
        }

        OpcMcpGatewayConfig cfg = gatewayConfigService.requireRuntimeConfig();
        if (StringUtils.isBlank(cfg.getGatewayApiKey())) {
            throw new ServiceException("组织 Gateway Key 未配置");
        }
        String target = resolveMcpUrl(cfg);
        try (HttpResponse resp = HttpRequest.post(target)
            .header("Authorization", "Bearer " + cfg.getGatewayApiKey())
            .header("Content-Type", "application/json")
            .header("Accept", StrUtil.blankToDefault(accept, "application/json, text/event-stream"))
            .body(body)
            .timeout(60000)
            .execute()) {
            String respBody = resp.body();
            if (!resp.isOk()) {
                throw new ServiceException("网关返回 HTTP " + resp.getStatus() + ": " + StrUtil.maxLength(respBody, 300));
            }
            if ("tools/list".equals(method) && StringUtils.isNotBlank(respBody) && JSONUtil.isTypeJSON(respBody)) {
                return filterToolsList(respBody, allowedToolNames);
            }
            return respBody;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("转发 MCP 网关失败：" + ex.getMessage());
        }
    }

    private List<OpcMcpAllowToolVo> listAllowTools(Long userId) {
        List<OpcDesktopUserCoworkerTool> rows = userCoworkerToolMapper.selectList(
            Wrappers.<OpcDesktopUserCoworkerTool>lambdaQuery()
                .eq(OpcDesktopUserCoworkerTool::getUserId, userId));
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> coworkerIds = rows.stream()
            .map(OpcDesktopUserCoworkerTool::getCoworkerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> keyMap = coworkerIds.isEmpty() ? Map.of() :
            coworkerMapper.selectList(Wrappers.<OpcCoworker>lambdaQuery()
                    .in(OpcCoworker::getCoworkerId, coworkerIds)
                    .select(OpcCoworker::getCoworkerId, OpcCoworker::getPersonaKey))
                .stream()
                .collect(Collectors.toMap(OpcCoworker::getCoworkerId, OpcCoworker::getPersonaKey, (a, b) -> a));
        List<OpcMcpAllowToolVo> list = new ArrayList<>(rows.size());
        for (OpcDesktopUserCoworkerTool row : rows) {
            OpcMcpAllowToolVo vo = new OpcMcpAllowToolVo();
            vo.setCoworkerId(row.getCoworkerId());
            vo.setCoworkerKey(keyMap.get(row.getCoworkerId()));
            vo.setMcpName(row.getLogicalMcp());
            vo.setToolName(row.getToolName());
            list.add(vo);
        }
        return list;
    }

    private static String resolveMcpUrl(OpcMcpGatewayConfig cfg) {
        String base = StrUtil.blankToDefault(cfg.getGatewayBaseUrl(), "").trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (StringUtils.isNotBlank(cfg.getVirtualServerId())) {
            return base + "/servers/" + cfg.getVirtualServerId().trim() + "/mcp";
        }
        return base + "/mcp";
    }

    private static String extractCallToolName(JSONObject req) {
        JSONObject params = req.getJSONObject("params");
        if (params == null) {
            return null;
        }
        String name = params.getStr("name");
        if (StringUtils.isNotBlank(name)) {
            return name.trim();
        }
        JSONObject tool = params.getJSONObject("tool");
        if (tool != null && StringUtils.isNotBlank(tool.getStr("name"))) {
            return tool.getStr("name").trim();
        }
        return null;
    }

    private static String filterToolsList(String respBody, Set<String> allowedToolNames) {
        try {
            JSONObject root = JSONUtil.parseObj(respBody);
            JSONObject result = root.getJSONObject("result");
            if (result == null) {
                return respBody;
            }
            JSONArray tools = result.getJSONArray("tools");
            if (tools == null) {
                return respBody;
            }
            JSONArray filtered = new JSONArray();
            for (Object item : tools) {
                if (!(item instanceof JSONObject jo)) {
                    continue;
                }
                String name = firstNonBlank(jo, "name", "toolName", "tool_name");
                if (StringUtils.isNotBlank(name) && allowedToolNames.contains(name.trim())) {
                    filtered.add(jo);
                }
            }
            result.set("tools", filtered);
            root.set("result", result);
            return root.toString();
        } catch (Exception ex) {
            return respBody;
        }
    }

    private static String firstNonBlank(JSONObject jo, String... keys) {
        for (String key : keys) {
            String v = jo.getStr(key);
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return null;
    }
}
