package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class McpGatewayToolItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String toolId;

    private String toolName;

    private String description;

    private String gatewayId;
}
