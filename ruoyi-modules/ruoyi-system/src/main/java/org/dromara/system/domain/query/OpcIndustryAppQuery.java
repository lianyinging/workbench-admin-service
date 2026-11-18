package org.dromara.system.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BasePageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpcIndustryAppQuery extends BasePageQuery {

    private String appKey;

    private String title;

    private String mode;

    private String status;

    private String visibility;
}
