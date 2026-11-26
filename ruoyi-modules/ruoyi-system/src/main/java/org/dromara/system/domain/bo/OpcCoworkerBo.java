package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class OpcCoworkerBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long coworkerId;

    @NotBlank(message = "personaKey 不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 64, message = "personaKey 不能超过64", groups = {AddGroup.class, EditGroup.class})
    private String personaKey;

    @NotBlank(message = "名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 128, message = "名称不能超过128", groups = {AddGroup.class, EditGroup.class})
    private String name;

    @Size(max = 255, message = "一句话不能超过255")
    private String tagline;

    @Size(max = 1000, message = "描述不能超过1000")
    private String description;

    @Size(max = 64, message = "图标不能超过64")
    private String icon;

    private String groupKey;

    /** 所属分类，可空 */
    private Long categoryId;

    private String version;

    /** zip base64；编辑时为空表示不改包 */
    private String packageZipBase64;

    private String visibility;

    /** 绑定的 MCP 网关配置 */
    private Long mcpConfigId;

    private Integer sortOrder;

    private String status;

    @Size(max = 500, message = "备注不能超过500")
    private String remark;

    private List<OpcCoworkerToolItemBo> tools;
}
