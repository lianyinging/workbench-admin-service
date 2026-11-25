package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcMcpGatewayConfig;
import org.dromara.system.domain.bo.OpcMcpGatewayConfigBo;
import org.dromara.system.domain.vo.McpGatewayToolItemVo;
import org.dromara.system.domain.vo.OpcMcpGatewayConfigVo;
import org.dromara.system.mapper.OpcMcpGatewayConfigMapper;
import org.dromara.system.service.IOpcMcpGatewayConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpcMcpGatewayConfigServiceImpl extends ServiceImpl<OpcMcpGatewayConfigMapper, OpcMcpGatewayConfig>
    implements IOpcMcpGatewayConfigService {

    private static final String DEFAULT_TENANT = "000000";

    @Override
    public OpcMcpGatewayConfigVo getConfig() {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> toVo(getOrNull()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveConfig(OpcMcpGatewayConfigBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcMcpGatewayConfig existing = getOrNull();
            if (existing == null) {
                existing = new OpcMcpGatewayConfig();
                existing.setTenantId(DEFAULT_TENANT);
                existing.setDelFlag("0");
            }
            if (bo.getGatewayBaseUrl() != null) {
                existing.setGatewayBaseUrl(blankToNull(bo.getGatewayBaseUrl()));
            }
            if (StringUtils.isNotBlank(bo.getGatewayApiKey())) {
                existing.setGatewayApiKey(bo.getGatewayApiKey().trim());
            }
            if (bo.getVirtualServerId() != null) {
                existing.setVirtualServerId(blankToNull(bo.getVirtualServerId()));
            }
            if (StringUtils.isNotBlank(bo.getAdminToken())) {
                existing.setAdminToken(bo.getAdminToken().trim());
            }
            if (bo.getRemark() != null) {
                existing.setRemark(bo.getRemark());
            }
            return saveOrUpdate(existing);
        });
    }

    @Override
    public List<McpGatewayToolItemVo> listGatewayTools() {
        OpcMcpGatewayConfig cfg = requireRuntimeConfig();
        String base = normalizeBase(cfg.getGatewayBaseUrl());
        String token = StrUtil.blankToDefault(cfg.getAdminToken(), cfg.getGatewayApiKey());
        if (StringUtils.isBlank(token)) {
            throw new ServiceException("请先配置 Gateway Key 或 Admin Token");
        }
        String url = base + "/tools";
        try (HttpResponse resp = HttpRequest.get(url)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/json")
            .timeout(15000)
            .execute()) {
            if (!resp.isOk()) {
                throw new ServiceException("拉取网关 tools 失败：HTTP " + resp.getStatus() + " " + StrUtil.maxLength(resp.body(), 200));
            }
            return parseTools(resp.body());
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("拉取网关 tools 失败：" + ex.getMessage());
        }
    }

    @Override
    public OpcMcpGatewayConfig requireRuntimeConfig() {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcMcpGatewayConfig cfg = getOrNull();
            if (cfg == null || StringUtils.isBlank(cfg.getGatewayBaseUrl())) {
                throw new ServiceException("尚未配置 MCP 网关地址");
            }
            return cfg;
        });
    }

    private OpcMcpGatewayConfig getOrNull() {
        return getOne(Wrappers.<OpcMcpGatewayConfig>lambdaQuery().last("LIMIT 1"));
    }

    private static OpcMcpGatewayConfigVo toVo(OpcMcpGatewayConfig cfg) {
        OpcMcpGatewayConfigVo vo = new OpcMcpGatewayConfigVo();
        if (cfg == null) {
            vo.setHasApiKey(false);
            vo.setHasAdminToken(false);
            return vo;
        }
        vo.setConfigId(cfg.getConfigId());
        vo.setGatewayBaseUrl(cfg.getGatewayBaseUrl());
        vo.setVirtualServerId(cfg.getVirtualServerId());
        vo.setRemark(cfg.getRemark());
        vo.setUpdateTime(cfg.getUpdateTime());
        boolean hasKey = StringUtils.isNotBlank(cfg.getGatewayApiKey());
        vo.setHasApiKey(hasKey);
        vo.setApiKeyHint(hasKey ? hint(cfg.getGatewayApiKey()) : null);
        vo.setHasAdminToken(StringUtils.isNotBlank(cfg.getAdminToken()));
        return vo;
    }

    private static String hint(String key) {
        String k = key.trim();
        if (k.length() <= 4) {
            return "****";
        }
        return "****" + k.substring(k.length() - 4);
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
            vo.setGatewayId(firstNonBlank(jo, "gateway_id", "gatewayId", "gateway"));
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
