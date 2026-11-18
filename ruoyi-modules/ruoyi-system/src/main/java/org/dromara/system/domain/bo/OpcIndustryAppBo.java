package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 行业应用业务对象
 */
@Data
public class OpcIndustryAppBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;

    @NotBlank(message = "应用键不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 64, message = "应用键不能超过64", groups = {AddGroup.class, EditGroup.class})
    private String appKey;

    @NotBlank(message = "标题不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 128, message = "标题不能超过128", groups = {AddGroup.class, EditGroup.class})
    private String title;

    @Size(max = 255, message = "副标题不能超过255")
    private String subtitle;

    private String category;

    private String tags;

    private String icon;

    /** Logo base64 data URL */
    private String logo;

    private String tone;

    @NotBlank(message = "模式不能为空", groups = {AddGroup.class, EditGroup.class})
    private String mode;

    private String url;

    private String pluginId;

    private String visibility;

    private Integer sortOrder;

    private String status;

    private String remark;
}
