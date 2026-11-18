package org.dromara.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcDesktopDevice;
import org.dromara.system.domain.bo.OpcDesktopHeartbeatBo;
import org.dromara.system.domain.bo.OpcDesktopRegisterBo;
import org.dromara.system.domain.vo.OpcDesktopDeviceVo;
import org.dromara.system.domain.vo.SysClientVo;
import org.dromara.system.mapper.OpcDesktopDeviceMapper;
import org.dromara.system.service.IOpcDesktopDeviceService;
import org.dromara.system.service.IOpcDesktopUserService;
import org.dromara.system.service.ISysClientService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OPC 桌面端设备服务实现
 */
@Service
public class OpcDesktopDeviceServiceImpl implements IOpcDesktopDeviceService {

    /** 使用全局 Redis 前缀，避免未登录桌面端缺少 tenantId */
    public static final String ONLINE_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "opc:desktop:online:";
    public static final String TOKEN_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "opc:desktop:token:";
    public static final String DEVICE_TOKEN_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "opc:desktop:device-token:";
    public static final Duration ONLINE_TTL = Duration.ofSeconds(90);
    public static final Duration TOKEN_TTL = Duration.ofDays(7);
    private static final String DEFAULT_TENANT = "000000";

    private final OpcDesktopDeviceMapper deviceMapper;
    private final ISysClientService clientService;
    private final IOpcDesktopUserService desktopUserService;

    public OpcDesktopDeviceServiceImpl(
        OpcDesktopDeviceMapper deviceMapper,
        ISysClientService clientService,
        @Lazy IOpcDesktopUserService desktopUserService
    ) {
        this.deviceMapper = deviceMapper;
        this.clientService = clientService;
        this.desktopUserService = desktopUserService;
    }

    @Override
    public Map<String, Object> register(OpcDesktopRegisterBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> doRegister(bo));
    }

    private Map<String, Object> doRegister(OpcDesktopRegisterBo bo) {
        SysClientVo client = clientService.queryByClientId(bo.getClientId());
        if (client == null || !"1".equals(client.getStatus())) {
            throw new ServiceException("客户端不存在或已停用");
        }
        if (!"desktop".equals(client.getDeviceType())) {
            throw new ServiceException("仅允许桌面客户端注册");
        }
        if (!Objects.equals(client.getClientSecret(), bo.getClientSecret())) {
            throw new ServiceException("客户端密钥错误");
        }

        Date now = new Date();
        OpcDesktopDevice existing = deviceMapper.selectById(bo.getDeviceId());
        if (existing != null) {
            if ("0".equals(existing.getStatus())) {
                throw new ServiceException("设备已禁用", 403);
            }
            existing.setDeviceName(StrUtil.blankToDefault(bo.getDeviceName(), existing.getDeviceName()));
            existing.setOsName(StrUtil.blankToDefault(bo.getOsName(), existing.getOsName()));
            existing.setOsVersion(StrUtil.blankToDefault(bo.getOsVersion(), existing.getOsVersion()));
            existing.setAppVersion(StrUtil.blankToDefault(bo.getAppVersion(), existing.getAppVersion()));
            existing.setLastIp(ServletUtils.getClientIP());
            existing.setLastHeartbeatTime(now);
            existing.setUpdateTime(now);
            deviceMapper.updateById(existing);
        } else {
            OpcDesktopDevice device = new OpcDesktopDevice();
            device.setDeviceId(bo.getDeviceId());
            device.setDeviceName(StrUtil.blankToDefault(bo.getDeviceName(), bo.getDeviceId()));
            device.setOsName(StrUtil.blankToDefault(bo.getOsName(), ""));
            device.setOsVersion(StrUtil.blankToDefault(bo.getOsVersion(), ""));
            device.setAppVersion(StrUtil.blankToDefault(bo.getAppVersion(), ""));
            device.setTenantId(DEFAULT_TENANT);
            device.setLastIp(ServletUtils.getClientIP());
            device.setLastHeartbeatTime(now);
            device.setStatus("1");
            device.setCreateTime(now);
            device.setUpdateTime(now);
            deviceMapper.insert(device);
        }

        String deviceToken = IdUtil.fastSimpleUUID();
        // 覆盖旧 token
        String oldToken = RedisUtils.getObject(DEVICE_TOKEN_KEY_PREFIX + bo.getDeviceId());
        if (StringUtils.isNotBlank(oldToken)) {
            RedisUtils.deleteObject(TOKEN_KEY_PREFIX + oldToken);
        }
        RedisUtils.setObject(TOKEN_KEY_PREFIX + deviceToken, bo.getDeviceId(), TOKEN_TTL);
        RedisUtils.setObject(DEVICE_TOKEN_KEY_PREFIX + bo.getDeviceId(), deviceToken, TOKEN_TTL);
        markOnline(bo.getDeviceId(), StrUtil.blankToDefault(bo.getDeviceName(), bo.getDeviceId()),
            StrUtil.blankToDefault(bo.getAppVersion(), ""), ServletUtils.getClientIP());

        Map<String, Object> result = new HashMap<>(4);
        result.put("deviceId", bo.getDeviceId());
        result.put("deviceToken", deviceToken);
        result.put("heartbeatIntervalSec", 30);
        result.put("onlineTtlSec", ONLINE_TTL.getSeconds());
        return result;
    }

    @Override
    public void heartbeat(String deviceToken, OpcDesktopHeartbeatBo bo) {
        TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> doHeartbeat(deviceToken, bo));
    }

    private void doHeartbeat(String deviceToken, OpcDesktopHeartbeatBo bo) {
        if (StringUtils.isBlank(deviceToken)) {
            throw new ServiceException("缺少设备令牌");
        }
        String deviceId = RedisUtils.getObject(TOKEN_KEY_PREFIX + deviceToken);
        if (StringUtils.isBlank(deviceId)) {
            throw new ServiceException("设备令牌无效或已过期");
        }
        OpcDesktopDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        if ("0".equals(device.getStatus())) {
            throw new ServiceException("设备已禁用", 403);
        }

        Date now = new Date();
        if (bo != null) {
            if (StringUtils.isNotBlank(bo.getDeviceName())) {
                device.setDeviceName(bo.getDeviceName());
            }
            if (StringUtils.isNotBlank(bo.getAppVersion())) {
                device.setAppVersion(bo.getAppVersion());
            }
            if (StringUtils.isNotBlank(bo.getOsName())) {
                device.setOsName(bo.getOsName());
            }
            if (StringUtils.isNotBlank(bo.getOsVersion())) {
                device.setOsVersion(bo.getOsVersion());
            }
        }
        String ip = ServletUtils.getClientIP();
        device.setLastIp(ip);
        device.setLastHeartbeatTime(now);
        device.setUpdateTime(now);
        deviceMapper.updateById(device);

        // 刷新 token TTL
        RedisUtils.expire(TOKEN_KEY_PREFIX + deviceToken, TOKEN_TTL);
        RedisUtils.expire(DEVICE_TOKEN_KEY_PREFIX + deviceId, TOKEN_TTL);
        markOnline(deviceId, device.getDeviceName(), device.getAppVersion(), ip);
    }

    @Override
    public List<OpcDesktopDeviceVo> listDevices(Boolean onlineOnly, String deviceName) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            LambdaQueryWrapper<OpcDesktopDevice> wrapper = Wrappers.lambdaQuery();
            wrapper.like(StringUtils.isNotBlank(deviceName), OpcDesktopDevice::getDeviceName, deviceName);
            wrapper.orderByDesc(OpcDesktopDevice::getLastHeartbeatTime);
            List<OpcDesktopDevice> devices = deviceMapper.selectList(wrapper);
            Set<Long> userIds = devices.stream()
                .map(OpcDesktopDevice::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            Map<Long, String> nickMap = desktopUserService.nickNameMap(userIds);
            Map<Long, Boolean> userOnlineMap = desktopUserService.onlineMap(userIds);
            return devices.stream()
                .map(d -> OpcDesktopDeviceVo.from(
                    d,
                    RedisUtils.hasKey(ONLINE_KEY_PREFIX + d.getDeviceId()),
                    d.getUserId() == null ? null : nickMap.get(d.getUserId()),
                    d.getUserId() == null ? null : userOnlineMap.getOrDefault(d.getUserId(), false)
                ))
                .filter(vo -> onlineOnly == null || !onlineOnly || Boolean.TRUE.equals(vo.getOnline()))
                .collect(Collectors.toList());
        });
    }

    @Override
    public void updateStatus(String deviceId, String status) {
        TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            if (!"0".equals(status) && !"1".equals(status)) {
                throw new ServiceException("状态值无效");
            }
            OpcDesktopDevice device = deviceMapper.selectById(deviceId);
            if (device == null) {
                throw new ServiceException("设备不存在");
            }
            device.setStatus(status);
            device.setUpdateTime(new Date());
            deviceMapper.updateById(device);
            if ("0".equals(status)) {
                RedisUtils.deleteObject(ONLINE_KEY_PREFIX + deviceId);
                String token = RedisUtils.getObject(DEVICE_TOKEN_KEY_PREFIX + deviceId);
                if (StringUtils.isNotBlank(token)) {
                    RedisUtils.deleteObject(TOKEN_KEY_PREFIX + token);
                }
                RedisUtils.deleteObject(DEVICE_TOKEN_KEY_PREFIX + deviceId);
            }
        });
    }

    private void markOnline(String deviceId, String deviceName, String appVersion, String ip) {
        Map<String, Object> payload = new HashMap<>(6);
        payload.put("deviceId", deviceId);
        payload.put("deviceName", deviceName);
        payload.put("appVersion", appVersion);
        payload.put("ip", ip);
        payload.put("ts", System.currentTimeMillis());
        RedisUtils.setObject(ONLINE_KEY_PREFIX + deviceId, payload, ONLINE_TTL);
    }
}
