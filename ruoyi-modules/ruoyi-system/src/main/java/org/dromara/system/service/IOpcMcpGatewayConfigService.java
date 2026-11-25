package org.dromara.system.service;

import org.dromara.system.domain.bo.OpcMcpGatewayConfigBo;
import org.dromara.system.domain.vo.McpGatewayToolItemVo;
import org.dromara.system.domain.vo.OpcMcpGatewayConfigVo;

import java.util.List;

public interface IOpcMcpGatewayConfigService {

    OpcMcpGatewayConfigVo getConfig();

    Boolean saveConfig(OpcMcpGatewayConfigBo bo);

    List<McpGatewayToolItemVo> listGatewayTools();

    /** 运行时读取完整配置（含密钥），仅服务端内部使用 */
    org.dromara.system.domain.OpcMcpGatewayConfig requireRuntimeConfig();
}
