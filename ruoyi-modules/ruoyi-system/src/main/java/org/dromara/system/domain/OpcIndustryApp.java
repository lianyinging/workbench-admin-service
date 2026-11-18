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
 * OPC 行业应用配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opc_industry_app")
public class OpcIndustryApp extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "app_id", type = IdType.AUTO)
    private Long appId;

    private String appKey;

    private String title;

    private String subtitle;

    private String category;

    private String tags;

    private String icon;

    /** Logo：data:image/...;base64,... */
    private String logo;

    private String tone;

    /** embed | plugin | external */
    private String mode;

    private String url;

    private String pluginId;

    /** public 公开 | login 登录 | subscribe 登录且订阅 */
    private String visibility;

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
