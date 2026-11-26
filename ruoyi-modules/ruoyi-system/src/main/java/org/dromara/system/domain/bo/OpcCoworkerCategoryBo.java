package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OpcCoworkerCategoryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long categoryId;

    @NotBlank(message = "分类名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 64, message = "分类名称不能超过64", groups = {AddGroup.class, EditGroup.class})
    private String categoryName;

    private Integer sortOrder;

    private String status;

    @Size(max = 500, message = "备注不能超过500")
    private String remark;
}
