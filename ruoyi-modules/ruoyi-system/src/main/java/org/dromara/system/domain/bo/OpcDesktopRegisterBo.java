package org.dromara.system.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 桌面端注册请求
 */
@Data
public class OpcDesktopRegisterBo {

    @NotBlank(message = "clientId不能为空")
    private String clientId;

    @NotBlank(message = "clientSecret不能为空")
    private String clientSecret;

    @NotBlank(message = "deviceId不能为空")
    private String deviceId;

    private String deviceName;

    private String osName;

    private String osVersion;

    private String appVersion;
}
