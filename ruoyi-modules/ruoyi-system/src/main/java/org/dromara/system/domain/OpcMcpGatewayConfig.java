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
 * OPC MCP 网关配置（可多条，同事按 config_id 绑定不同秘钥）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("opc_mcp_gateway_config")
public class OpcMcpGatewayConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "config_id", type = IdType.AUTO)
    private Long configId;

    /** 配置名称 */
    private String configName;

    private String gatewayBaseUrl;

    private String gatewayApiKey;

    private String virtualServerId;

    private String adminToken;

    private String remark;

    /** 1正常 0停用 */
    private String status;

    private Integer sortOrder;

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
