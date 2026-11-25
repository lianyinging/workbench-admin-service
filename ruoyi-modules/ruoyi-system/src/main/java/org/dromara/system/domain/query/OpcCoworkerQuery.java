package org.dromara.system.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BasePageQuery;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpcCoworkerQuery extends BasePageQuery {

    private String personaKey;

    private String name;

    private String groupKey;

    private String status;

    private String visibility;
}
