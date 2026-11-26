package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.system.domain.OpcCoworkerCategory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class OpcCoworkerCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;

    private String categoryName;

    private Integer sortOrder;

    private String status;

    private String remark;

    /** 该分类下同事数 */
    private Long coworkerCount;

    private Date updateTime;

    public static OpcCoworkerCategoryVo from(OpcCoworkerCategory entity) {
        if (entity == null) {
            return null;
        }
        OpcCoworkerCategoryVo vo = new OpcCoworkerCategoryVo();
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
