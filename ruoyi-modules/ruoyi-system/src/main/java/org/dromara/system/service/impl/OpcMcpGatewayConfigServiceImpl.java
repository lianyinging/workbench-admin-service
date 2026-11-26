package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.SortQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcCoworker;
import org.dromara.system.domain.OpcMcpGatewayConfig;
import org.dromara.system.domain.bo.OpcMcpGatewayConfigBo;
import org.dromara.system.domain.query.OpcMcpGatewayConfigQuery;
import org.dromara.system.domain.vo.McpGatewayToolItemVo;
import org.dromara.system.domain.vo.OpcMcpGatewayConfigVo;
import org.dromara.system.mapper.OpcCoworkerMapper;
import org.dromara.system.mapper.OpcMcpGatewayConfigMapper;
import org.dromara.system.service.IOpcMcpGatewayConfigService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpcMcpGatewayConfigServiceImpl extends ServiceImpl<OpcMcpGatewayConfigMapper, OpcMcpGatewayConfig>
    implements IOpcMcpGatewayConfigService {

    private static final String DEFAULT_TENANT = "000000";

    private final OpcCoworkerMapper coworkerMapper;

    public OpcMcpGatewayConfigServiceImpl(@Lazy OpcCoworkerMapper coworkerMapper) {
        this.coworkerMapper = coworkerMapper;
    }

    @Override
    public TableDataInfo<OpcMcpGatewayConfigVo> queryPageList(OpcMcpGatewayConfigQuery query) {
        return PageQuery.of(() -> queryList(query));
    }

    private List<OpcMcpGatewayConfigVo> queryList(OpcMcpGatewayConfigQuery query) {
        return SortQuery.of(() -> TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            LambdaQueryWrapper<OpcMcpGatewayConfig> lqw = Wrappers.lambdaQuery();
            lqw.like(StringUtils.isNotBlank(query.getConfigName()), OpcMcpGatewayConfig::getConfigName, query.getConfigName());
            lqw.eq(StringUtils.isNotBlank(query.getStatus()), OpcMcpGatewayConfig::getStatus, query.getStatus());
            lqw.orderByAsc(OpcMcpGatewayConfig::getSortOrder).orderByDesc(OpcMcpGatewayConfig::getConfigId);
            return list(lqw).stream().map(OpcMcpGatewayConfigVo::from).collect(Collectors.toList());
        }));
    }

    @Override
    public List<OpcMcpGatewayConfigVo> queryOptions() {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () ->
            list(Wrappers.<OpcMcpGatewayConfig>lambdaQuery()
                .eq(OpcMcpGatewayConfig::getStatus, "1")
                .orderByAsc(OpcMcpGatewayConfig::getSortOrder)
                .orderByDesc(OpcMcpGatewayConfig::getConfigId))
                .stream()
                .map(OpcMcpGatewayConfigVo::from)
                .collect(Collectors.toList()));
    }

    @Override
    public OpcMcpGatewayConfigVo queryById(Long configId) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> OpcMcpGatewayConfigVo.from(getById(configId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(OpcMcpGatewayConfigBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcMcpGatewayConfig entity = new OpcMcpGatewayConfig();
            entity.setTenantId(DEFAULT_TENANT);
            entity.setDelFlag("0");
            applyBo(entity, bo, true);
            return save(entity);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(OpcMcpGatewayConfigBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            if (bo.getConfigId() == null) {
                throw new ServiceException("配置ID不能为空");
            }
            OpcMcpGatewayConfig existing = getById(bo.getConfigId());
            if (existing == null) {
                throw new ServiceException("配置不存在");
            }
            applyBo(existing, bo, false);
            return updateById(existing);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(Long configId, String status) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcMcpGatewayConfig existing = getById(configId);
            if (existing == null) {
                throw new ServiceException("配置不存在");
            }
            if (!"0".equals(status) && !"1".equals(status)) {
                throw new ServiceException("状态无效");
            }
            existing.setStatus(status);
            return updateById(existing);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            if (ids == null || ids.isEmpty()) {
                return false;
            }
            long bound = coworkerMapper.selectCount(Wrappers.<OpcCoworker>lambdaQuery()
                .in(OpcCoworker::getMcpConfigId, ids));
            if (bound > 0) {
                throw new ServiceException("仍有同事绑定该网关配置，无法删除");
            }
            return removeByIds(ids);
        });
    }

    @Override
    public List<McpGatewayToolItemVo> listGatewayTools(Long configId) {
        OpcMcpGatewayConfig cfg = requireRuntimeConfig(configId);
        if (StringUtils.isBlank(cfg.getGatewayApiKey())) {
            throw new ServiceException("请先配置 Gateway Key（mcp_… API Key）");
        }
        String mcpUrl = resolveMcpUrl(cfg);
        String apiKey = cfg.getGatewayApiKey().trim();
        try {
            // MCP Streamable HTTP：initialize → initialized → tools/list；认 API Key，返回授权范围内工具
            String sessionId;
            try (HttpResponse initResp = postMcp(mcpUrl, apiKey, null, mcpInitializeBody())) {
                assertMcpHttpOk(initResp, "MCP initialize");
                sessionId = firstHeader(initResp, "mcp-session-id", "Mcp-Session-Id");
            }
            try (HttpResponse ignored = postMcp(mcpUrl, apiKey, sessionId, mcpInitializedNotificationBody())) {
                // notification 无 id，部分实现返回 202/空体，不强制校验业务 JSON
            }
            String rawBody;
            try (HttpResponse listResp = postMcp(mcpUrl, apiKey, sessionId, mcpToolsListBody())) {
                assertMcpHttpOk(listResp, "拉取网关 tools");
                rawBody = listResp.body();
            }
            String jsonPayload = extractJsonRpcPayload(rawBody);
            if (looksLikeMcpError(jsonPayload)) {
                throw new ServiceException("拉取网关 tools 失败：" + StrUtil.maxLength(jsonPayload, 300));
            }
            return parseTools(jsonPayload);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("拉取网关 tools 失败：" + ex.getMessage());
        }
    }

    @Override
    public OpcMcpGatewayConfig requireRuntimeConfig(Long configId) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcMcpGatewayConfig cfg;
            if (configId != null) {
                cfg = getById(configId);
            } else {
                cfg = getOne(Wrappers.<OpcMcpGatewayConfig>lambdaQuery()
                    .eq(OpcMcpGatewayConfig::getStatus, "1")
                    .orderByAsc(OpcMcpGatewayConfig::getSortOrder)
                    .orderByDesc(OpcMcpGatewayConfig::getConfigId)
                    .last("LIMIT 1"));
            }
            if (cfg == null || StringUtils.isBlank(cfg.getGatewayBaseUrl())) {
                throw new ServiceException(configId == null ? "尚未配置 MCP 网关地址" : "MCP 网关配置不存在或未填写地址");
            }
            if (configId != null && "0".equals(cfg.getStatus())) {
                throw new ServiceException("MCP 网关配置已停用");
            }
            return cfg;
        });
    }

    private void applyBo(OpcMcpGatewayConfig entity, OpcMcpGatewayConfigBo bo, boolean creating) {
        if (StringUtils.isBlank(bo.getConfigName())) {
            throw new ServiceException("配置名称不能为空");
        }
        entity.setConfigName(bo.getConfigName().trim());
        if (bo.getGatewayBaseUrl() != null) {
            entity.setGatewayBaseUrl(blankToNull(bo.getGatewayBaseUrl()));
        }
        if (StringUtils.isNotBlank(bo.getGatewayApiKey())) {
            entity.setGatewayApiKey(bo.getGatewayApiKey().trim());
        } else if (creating) {
            entity.setGatewayApiKey(null);
        }
        if (bo.getVirtualServerId() != null) {
            entity.setVirtualServerId(blankToNull(bo.getVirtualServerId()));
        }
        if (StringUtils.isNotBlank(bo.getAdminToken())) {
            entity.setAdminToken(bo.getAdminToken().trim());
        } else if (creating) {
            entity.setAdminToken(null);
        }
        if (bo.getRemark() != null) {
            entity.setRemark(bo.getRemark());
        }
        if (StringUtils.isNotBlank(bo.getStatus())) {
            entity.setStatus(bo.getStatus());
        } else if (creating) {
            entity.setStatus("1");
        }
        if (bo.getSortOrder() != null) {
            entity.setSortOrder(bo.getSortOrder());
        } else if (creating && entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
    }

    private static String normalizeBase(String baseUrl) {
        String base = StrUtil.blankToDefault(baseUrl, "").trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (StringUtils.isBlank(base)) {
            throw new ServiceException("网关地址无效");
        }
        return base;
    }

    private static String resolveMcpUrl(OpcMcpGatewayConfig cfg) {
        // 统一走根 /mcp/；工具范围由 Gateway Key 授权决定，不再依赖 Virtual Server
        return normalizeBase(cfg.getGatewayBaseUrl()) + "/mcp/";
    }

    private static HttpResponse postMcp(String mcpUrl, String apiKey, String sessionId, String body) {
        String url = mcpUrl;
        for (int hop = 0; hop < 4; hop++) {
            HttpRequest req = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .setFollowRedirects(false)
                .body(body)
                .timeout(20000);
            if (StringUtils.isNotBlank(sessionId)) {
                req.header("Mcp-Session-Id", sessionId);
            }
            HttpResponse resp = req.execute();
            int status = resp.getStatus();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = firstHeader(resp, "Location", "location");
                resp.close();
                if (StringUtils.isBlank(location)) {
                    throw new ServiceException("MCP 返回 " + status + " 但缺少 Location");
                }
                url = resolveRedirectUrl(url, location);
                continue;
            }
            return resp;
        }
        throw new ServiceException("MCP 重定向次数过多：" + mcpUrl);
    }

    private static String resolveRedirectUrl(String currentUrl, String location) {
        String loc = location.trim();
        if (loc.startsWith("http://") || loc.startsWith("https://")) {
            return loc;
        }
        try {
            java.net.URI base = java.net.URI.create(currentUrl);
            return base.resolve(loc).toString();
        } catch (Exception ex) {
            String base = normalizeBase(currentUrl);
            return loc.startsWith("/") ? base.replaceAll("(https?://[^/]+).*", "$1") + loc : base + "/" + loc;
        }
    }

    private static void assertMcpHttpOk(HttpResponse resp, String action) {
        if (resp == null || !resp.isOk()) {
            int status = resp == null ? -1 : resp.getStatus();
            String body = resp == null ? "" : StrUtil.maxLength(resp.body(), 200);
            throw new ServiceException(action + " 失败：HTTP " + status + " " + body);
        }
    }

    private static String firstHeader(HttpResponse resp, String... names) {
        if (resp == null || names == null) {
            return null;
        }
        for (String name : names) {
            String v = resp.header(name);
            if (StringUtils.isNotBlank(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static String mcpInitializeBody() {
        return new JSONObject()
            .set("jsonrpc", "2.0")
            .set("id", 1)
            .set("method", "initialize")
            .set("params", new JSONObject()
                .set("protocolVersion", "2024-11-05")
                .set("capabilities", new JSONObject())
                .set("clientInfo", new JSONObject()
                    .set("name", "workbench-admin")
                    .set("version", "1.0.0")))
            .toString();
    }

    private static String mcpInitializedNotificationBody() {
        return new JSONObject()
            .set("jsonrpc", "2.0")
            .set("method", "notifications/initialized")
            .toString();
    }

    private static String mcpToolsListBody() {
        return new JSONObject()
            .set("jsonrpc", "2.0")
            .set("id", 2)
            .set("method", "tools/list")
            .set("params", new JSONObject())
            .toString();
    }

    /** 从纯 JSON 或 SSE（event: message / data: {...}）中取出 JSON-RPC 负载 */
    private static String extractJsonRpcPayload(String body) {
        if (StringUtils.isBlank(body)) {
            return body;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        String lastData = null;
        for (String line : body.split("\\R")) {
            String t = line.trim();
            if (t.startsWith("data:")) {
                lastData = t.substring(5).trim();
            }
        }
        return StringUtils.isNotBlank(lastData) ? lastData : trimmed;
    }

    private static boolean looksLikeMcpError(String payload) {
        if (StringUtils.isBlank(payload) || !JSONUtil.isTypeJSON(payload)) {
            return false;
        }
        try {
            JSONObject jo = JSONUtil.parseObj(payload);
            return jo.containsKey("error");
        } catch (Exception ex) {
            return false;
        }
    }

    private static String blankToNull(String v) {
        return StringUtils.isBlank(v) ? null : v.trim();
    }

    private static List<McpGatewayToolItemVo> parseTools(String body) {
        List<McpGatewayToolItemVo> list = new ArrayList<>();
        if (StringUtils.isBlank(body)) {
            return list;
        }
        Object root = JSONUtil.parse(body);
        JSONArray arr = null;
        if (root instanceof JSONArray ja) {
            arr = ja;
        } else if (root instanceof JSONObject jo) {
            if (jo.get("tools") instanceof JSONArray) {
                arr = jo.getJSONArray("tools");
            } else if (jo.get("data") instanceof JSONArray) {
                arr = jo.getJSONArray("data");
            } else if (jo.get("items") instanceof JSONArray) {
                arr = jo.getJSONArray("items");
            } else if (jo.get("result") instanceof JSONObject result
                && result.get("tools") instanceof JSONArray) {
                arr = result.getJSONArray("tools");
            }
        }
        if (arr == null) {
            return list;
        }
        for (Object item : arr) {
            if (!(item instanceof JSONObject jo)) {
                continue;
            }
            McpGatewayToolItemVo vo = new McpGatewayToolItemVo();
            String id = firstNonBlank(jo, "id", "tool_id", "toolId", "name");
            String name = firstNonBlank(jo, "name", "tool_name", "toolName", "id");
            vo.setToolId(id);
            vo.setToolName(name);
            vo.setDescription(firstNonBlank(jo, "description", "desc"));
            JSONObject ann = jo.getJSONObject("annotations");
            String gatewayId = firstNonBlank(jo, "gateway_id", "gatewayId", "gateway");
            String gatewayName = firstNonBlank(jo, "gateway_name", "gatewayName", "gateway_slug", "gatewaySlug");
            if (ann != null) {
                if (StringUtils.isBlank(gatewayId)) {
                    gatewayId = firstNonBlank(ann, "gateway_id", "gatewayId", "gateway");
                }
                if (StringUtils.isBlank(gatewayName)) {
                    gatewayName = firstNonBlank(ann, "gateway_name", "gatewayName", "gateway_slug", "gatewaySlug");
                }
            }
            vo.setGatewayId(gatewayId);
            vo.setGatewayName(StringUtils.isNotBlank(gatewayName) ? gatewayName : gatewayId);
            if (StringUtils.isNotBlank(vo.getToolId()) || StringUtils.isNotBlank(vo.getToolName())) {
                list.add(vo);
            }
        }
        return list;
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
