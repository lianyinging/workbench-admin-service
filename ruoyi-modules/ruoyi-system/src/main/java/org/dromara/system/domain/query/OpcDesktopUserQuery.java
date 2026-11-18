package org.dromara.system.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BasePageQuery;

/**
 * 桌面端用户查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpcDesktopUserQuery extends BasePageQuery {

    private String userName;

    private String nickName;

    private String status;
}
