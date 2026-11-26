package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.system.domain.OpcMcpGatewayConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class OpcMcpGatewayConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long configId;

    private String configName;

    private String gatewayBaseUrl;

    private Boolean hasApiKey;

    private String apiKeyHint;

    private String virtualServerId;

    private Boolean hasAdminToken;

    private String remark;

    private String status;

    private Integer sortOrder;

    private Date updateTime;

    public static OpcMcpGatewayConfigVo from(OpcMcpGatewayConfig cfg) {
        OpcMcpGatewayConfigVo vo = new OpcMcpGatewayConfigVo();
        if (cfg == null) {
            vo.setHasApiKey(false);
            vo.setHasAdminToken(false);
            return vo;
        }
        vo.setConfigId(cfg.getConfigId());
        vo.setConfigName(cfg.getConfigName());
        vo.setGatewayBaseUrl(cfg.getGatewayBaseUrl());
        vo.setVirtualServerId(cfg.getVirtualServerId());
        vo.setRemark(cfg.getRemark());
        vo.setStatus(cfg.getStatus());
        vo.setSortOrder(cfg.getSortOrder());
        vo.setUpdateTime(cfg.getUpdateTime());
        boolean hasKey = cfg.getGatewayApiKey() != null && !cfg.getGatewayApiKey().isBlank();
        vo.setHasApiKey(hasKey);
        vo.setApiKeyHint(hasKey ? hint(cfg.getGatewayApiKey()) : null);
        vo.setHasAdminToken(cfg.getAdminToken() != null && !cfg.getAdminToken().isBlank());
        return vo;
    }

    private static String hint(String key) {
        String k = key.trim();
        if (k.length() <= 4) {
            return "****";
        }
        return "****" + k.substring(k.length() - 4);
    }
}
