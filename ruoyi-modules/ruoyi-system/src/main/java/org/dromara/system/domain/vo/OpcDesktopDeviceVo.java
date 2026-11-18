package org.dromara.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.OpcDesktopDevice;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 桌面端设备视图（含在线态）
 */
@Data
@AutoMapper(target = OpcDesktopDevice.class)
public class OpcDesktopDeviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String deviceId;

    private String deviceName;

    private String osName;

    private String osVersion;

    private String appVersion;

    private Long userId;

    /** 绑定桌面用户昵称/账号 */
    private String userDisplayName;

    /** 绑定用户是否仍登录（会话在线） */
    private Boolean userOnline;

    private String lastIp;

    private Date lastHeartbeatTime;

    /** 1正常 0禁用 */
    private String status;

    /** 设备是否在线（心跳） */
    private Boolean online;

    public static OpcDesktopDeviceVo from(OpcDesktopDevice device, boolean online) {
        return from(device, online, null, null);
    }

    public static OpcDesktopDeviceVo from(OpcDesktopDevice device, boolean online, String userDisplayName) {
        return from(device, online, userDisplayName, null);
    }

    public static OpcDesktopDeviceVo from(
        OpcDesktopDevice device, boolean online, String userDisplayName, Boolean userOnline
    ) {
        OpcDesktopDeviceVo vo = new OpcDesktopDeviceVo();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(device.getDeviceName());
        vo.setOsName(device.getOsName());
        vo.setOsVersion(device.getOsVersion());
        vo.setAppVersion(device.getAppVersion());
        vo.setUserId(device.getUserId());
        vo.setUserDisplayName(userDisplayName);
        vo.setUserOnline(userOnline);
        vo.setLastIp(device.getLastIp());
        vo.setLastHeartbeatTime(device.getLastHeartbeatTime());
        vo.setStatus(device.getStatus());
        vo.setOnline(online);
        return vo;
    }
}
