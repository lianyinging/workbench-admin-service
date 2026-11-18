package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 桌面端用户业务对象
 */
@Data
public class OpcDesktopUserBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    @NotBlank(message = "账号不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(min = 2, max = 64, message = "账号长度须在2-64之间", groups = {AddGroup.class, EditGroup.class})
    private String userName;

    @Size(max = 64, message = "昵称长度不能超过64", groups = {AddGroup.class, EditGroup.class})
    private String nickName;

    /** 新增必填；修改时留空表示不改密码 */
    @NotBlank(message = "密码不能为空", groups = {AddGroup.class})
    @Size(min = 5, max = 50, message = "密码长度须在5-50之间", groups = {AddGroup.class})
    private String password;

    private String status;

    /** 兼容字段：有勾选行业应用时为 1 */
    private String subscribed;

    /** 订阅的行业应用 ID 列表（多选） */
    private java.util.List<Long> appIds;

    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}
