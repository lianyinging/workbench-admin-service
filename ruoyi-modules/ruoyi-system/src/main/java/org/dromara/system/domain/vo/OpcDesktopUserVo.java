package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.system.domain.OpcDesktopUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 桌面端用户视图（不含密码）
 */
@Data
public class OpcDesktopUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String userName;

    private String nickName;

    private String status;

    /** 兼容：勾选过行业应用则为 1 */
    private String subscribed;

    /** 订阅的行业应用 ID 列表 */
    private java.util.List<Long> appIds;

    /** 订阅应用标题（列表展示） */
    private java.util.List<String> appTitles;

    private String remark;

    private String lastLoginIp;

    private Date lastLoginTime;

    private Date createTime;

    private Date updateTime;

    /** 是否仍有桌面端登录会话 */
    private Boolean online;

    public static OpcDesktopUserVo from(OpcDesktopUser user) {
        return from(user, null);
    }

    public static OpcDesktopUserVo from(OpcDesktopUser user, Boolean online) {
        if (user == null) {
            return null;
        }
        OpcDesktopUserVo vo = new OpcDesktopUserVo();
        vo.setUserId(user.getUserId());
        vo.setUserName(user.getUserName());
        vo.setNickName(user.getNickName());
        vo.setStatus(user.getStatus());
        vo.setSubscribed(user.getSubscribed());
        vo.setRemark(user.getRemark());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setOnline(online);
        return vo;
    }
}
