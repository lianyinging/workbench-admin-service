package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * OPC 同事目录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opc_coworker")
public class OpcCoworker extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "coworker_id", type = IdType.AUTO)
    private Long coworkerId;

    private String personaKey;

    private String name;

    private String tagline;

    private String description;

    private String icon;

    private String groupKey;

    /** 所属分类（可空 = 未分类；更新时允许置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;

    private String version;

    /** zip base64（可含 data URL 前缀） */
    private String packageZip;

    private String packageSha256;

    private Long packageSize;

    /** public | login | subscribe */
    private String visibility;

    /** 绑定的 MCP 网关配置 */
    private Long mcpConfigId;

    private Integer sortOrder;

    /** 1启用 0停用 */
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
