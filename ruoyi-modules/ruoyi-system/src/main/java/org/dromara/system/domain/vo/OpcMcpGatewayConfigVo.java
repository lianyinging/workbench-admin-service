package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class OpcMcpGatewayConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long configId;

    private String gatewayBaseUrl;

    private Boolean hasApiKey;

    private String apiKeyHint;

    private String virtualServerId;

    private Boolean hasAdminToken;

    private String remark;

    private Date updateTime;
}
