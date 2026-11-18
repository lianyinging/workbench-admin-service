package org.dromara.system.domain.bo;

import lombok.Data;

/**
 * 桌面端心跳请求
 */
@Data
public class OpcDesktopHeartbeatBo {

    private String deviceName;

    private String appVersion;

    private String osName;

    private String osVersion;
}
