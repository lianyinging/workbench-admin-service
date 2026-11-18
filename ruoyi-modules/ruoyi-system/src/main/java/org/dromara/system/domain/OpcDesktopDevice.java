package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * OPC 桌面端设备
 */
@Data
@TableName("opc_desktop_device")
public class OpcDesktopDevice implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "device_id")
    private String deviceId;

    private String deviceName;

    private String osName;

    private String osVersion;

    private String appVersion;

    private Long userId;

    private String tenantId;

    private Date lastHeartbeatTime;

    private String lastIp;

    /** 状态（1正常 0禁用） */
    private String status;

    private Date createTime;

    private Date updateTime;
}
