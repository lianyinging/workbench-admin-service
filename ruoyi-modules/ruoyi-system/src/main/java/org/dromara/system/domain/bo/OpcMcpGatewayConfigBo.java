package org.dromara.system.domain.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OpcMcpGatewayConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 512, message = "网关地址过长")
    private String gatewayBaseUrl;

    /** 留空表示不修改已有 Key */
    private String gatewayApiKey;

    @Size(max = 128, message = "Virtual Server ID 过长")
    private String virtualServerId;

    /** 留空表示不修改 */
    private String adminToken;

    @Size(max = 500, message = "备注不能超过500")
    private String remark;
}
