package org.dromara.system.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BasePageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpcMcpGatewayConfigQuery extends BasePageQuery {

    private String configName;

    private String status;
}
