package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class OpcMcpAccessVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 相对或绝对代理 URL，如 /opc/desktop/mcp */
    private String proxyUrl;

    private String virtualServerId;

    private List<OpcMcpAllowToolVo> allowTools;
}
