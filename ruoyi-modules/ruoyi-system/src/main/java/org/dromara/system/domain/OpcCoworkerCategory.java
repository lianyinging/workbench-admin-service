package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * OPC 同事分类（一层）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opc_coworker_category")
public class OpcCoworkerCategory extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "category_id", type = IdType.AUTO)
    private Long categoryId;

    private String categoryName;

    private Integer sortOrder;

    /** 1正常 0停用 */
    private String status;

    private String remark;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private Long createDept;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
