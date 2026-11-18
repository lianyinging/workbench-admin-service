package org.dromara.system.service;

import org.dromara.system.domain.bo.OpcDesktopHeartbeatBo;
import org.dromara.system.domain.bo.OpcDesktopRegisterBo;
import org.dromara.system.domain.vo.OpcDesktopDeviceVo;

import java.util.List;
import java.util.Map;

/**
 * OPC 桌面端设备服务
 */
public interface IOpcDesktopDeviceService {

    /**
     * 注册设备并返回 deviceToken
     */
    Map<String, Object> register(OpcDesktopRegisterBo bo);

    /**
     * 心跳保活
     */
    void heartbeat(String deviceToken, OpcDesktopHeartbeatBo bo);

    /**
     * 设备列表（含在线态）
     *
     * @param onlineOnly true 仅在线；null/false 全部
     */
    List<OpcDesktopDeviceVo> listDevices(Boolean onlineOnly, String deviceName);

    /**
     * 启用/禁用设备
     */
    void updateStatus(String deviceId, String status);
}
