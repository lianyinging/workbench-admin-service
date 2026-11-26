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

    /** 按分类筛选；与 uncategorized 互斥 */
    private Long categoryId;

    /** true：仅查未分类（category_id IS NULL） */
    private Boolean uncategorized;

    private String status;

    private String visibility;
}
