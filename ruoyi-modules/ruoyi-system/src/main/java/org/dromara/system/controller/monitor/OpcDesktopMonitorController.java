package org.dromara.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.OpcDesktopDeviceVo;
import org.dromara.system.service.IOpcDesktopDeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：桌面端在线监控
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/desktop")
public class OpcDesktopMonitorController extends BaseController {

    private final IOpcDesktopDeviceService desktopDeviceService;

    /**
     * 桌面设备列表
     */
    /** 权限挂在「应用客户端」菜单：system:client:list / edit */
    @SaCheckPermission("system:client:list")
    @GetMapping("/list")
    public R<List<OpcDesktopDeviceVo>> list(
        @RequestParam(required = false) Boolean onlineOnly,
        @RequestParam(required = false) String deviceName
    ) {
        return R.ok(desktopDeviceService.listDevices(onlineOnly, deviceName));
    }

    /**
     * 启用/禁用设备 status=1|0
     */
    @SaCheckPermission("system:client:edit")
    @Log(title = "桌面端设备", businessType = BusinessType.UPDATE)
    @PutMapping("/{deviceId}/status/{status}")
    public R<Void> updateStatus(@PathVariable String deviceId, @PathVariable String status) {
        desktopDeviceService.updateStatus(deviceId, status);
        return R.ok();
    }
}
