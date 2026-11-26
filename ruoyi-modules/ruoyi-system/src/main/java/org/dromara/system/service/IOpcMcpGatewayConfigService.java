package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.OpcMcpGatewayConfig;
import org.dromara.system.domain.bo.OpcMcpGatewayConfigBo;
import org.dromara.system.domain.query.OpcMcpGatewayConfigQuery;
import org.dromara.system.domain.vo.McpGatewayToolItemVo;
import org.dromara.system.domain.vo.OpcMcpGatewayConfigVo;

import java.util.Collection;
import java.util.List;

public interface IOpcMcpGatewayConfigService {

    TableDataInfo<OpcMcpGatewayConfigVo> queryPageList(OpcMcpGatewayConfigQuery query);

    List<OpcMcpGatewayConfigVo> queryOptions();

    OpcMcpGatewayConfigVo queryById(Long configId);

    Boolean insertByBo(OpcMcpGatewayConfigBo bo);

    Boolean updateByBo(OpcMcpGatewayConfigBo bo);

    Boolean updateStatus(Long configId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    List<McpGatewayToolItemVo> listGatewayTools(Long configId);

    /** 运行时读取完整配置（含密钥）；configId 为空时取第一条启用配置 */
    OpcMcpGatewayConfig requireRuntimeConfig(Long configId);

    /** @deprecated 使用 {@link #requireRuntimeConfig(Long)} */
    default OpcMcpGatewayConfig requireRuntimeConfig() {
        return requireRuntimeConfig(null);
    }
}
