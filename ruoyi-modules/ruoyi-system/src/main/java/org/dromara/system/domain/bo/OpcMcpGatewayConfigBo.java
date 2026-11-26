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
public class OpcMcpGatewayConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long configId;

    @NotBlank(message = "配置名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 128, message = "配置名称过长", groups = {AddGroup.class, EditGroup.class})
    private String configName;

    @Size(max = 512, message = "网关地址过长")
    private String gatewayBaseUrl;

    /** 留空表示不修改已有 Key */
    private String gatewayApiKey;

    @Size(max = 128, message = "Virtual Server ID 过长")
    private String virtualServerId;

    /** 留空表示不修改 */
    private String adminToken;

    @Size(max = 500, message = "备注不能超过500")
    private String remark;

    private String status;

    private Integer sortOrder;
}
