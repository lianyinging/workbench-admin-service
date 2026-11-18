package org.dromara.common.core.utils.ip;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.xdb.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * IP地址行政区域工具类。
 * <p>初始化失败时降级为「未知」，避免登录等主流程被 NoClassDefFoundError 拖垮。
 */
@Slf4j
public class RegionUtils {

    public static final String DEFAULT_IPV4_XDB_PATH = "ip2region_v4.xdb";
    public static final String DEFAULT_IPV6_XDB_PATH = "ip2region_v6.xdb";
    public static final String UNKNOWN_ADDRESS = "未知";

    private static volatile Ip2Region ip2Region;

    static {
        try {
            Path v4File = materializeXdb(DEFAULT_IPV4_XDB_PATH);
            if (v4File == null) {
                log.error("RegionUtils 未找到 {}，IP 归属地查询将返回「未知」。请将 xdb 放到 classpath resources", DEFAULT_IPV4_XDB_PATH);
            } else {
                // VIndexCache：不把整库灌进内存，适合 Docker 小内存
                Config v4Config = Config.custom()
                    .setCachePolicy(Config.VIndexCache)
                    .setXdbFile(v4File.toFile())
                    .asV4();

                Config v6Config = null;
                Path v6File = materializeXdb(DEFAULT_IPV6_XDB_PATH);
                if (v6File == null) {
                    log.warn("未加载 IPv6 地址库 {}，仅启用 IPv4 查询", DEFAULT_IPV6_XDB_PATH);
                } else {
                    v6Config = Config.custom()
                        .setCachePolicy(Config.VIndexCache)
                        .setXdbFile(v6File.toFile())
                        .asV6();
                }

                ip2Region = Ip2Region.create(v4Config, v6Config);
                log.info("RegionUtils 初始化成功（VIndexCache）");
            }
        } catch (Throwable e) {
            ip2Region = null;
            log.error("RegionUtils 初始化失败，IP 归属地查询降级为「未知」: {}", e.getMessage(), e);
        }
    }

    /**
     * 将 classpath 中的 xdb 落到临时文件，供 VIndexCache / setXdbFile 使用。
     */
    private static Path materializeXdb(String classpathName) {
        try (InputStream in = ResourceUtil.getStreamSafe(classpathName)) {
            if (in == null) {
                return null;
            }
            Path tmp = Files.createTempFile("ip2region-", "-" + classpathName);
            tmp.toFile().deleteOnExit();
            try (OutputStream out = Files.newOutputStream(tmp)) {
                IoUtil.copy(in, out);
            }
            return tmp;
        } catch (Exception e) {
            log.warn("加载地址库 {} 失败: {}", classpathName, e.getMessage());
            return null;
        }
    }

    public static String getRegion(String ipString) {
        Ip2Region regionService = ip2Region;
        if (regionService == null) {
            return UNKNOWN_ADDRESS;
        }
        try {
            String region = regionService.search(ipString);
            return StringUtils.isBlank(region) ? UNKNOWN_ADDRESS : region;
        } catch (Exception e) {
            log.error("IP地址离线获取城市异常 {}", ipString);
            return UNKNOWN_ADDRESS;
        }
    }

    public static String getRegion(byte[] ipBytes) {
        Ip2Region regionService = ip2Region;
        if (regionService == null) {
            return UNKNOWN_ADDRESS;
        }
        try {
            String region = regionService.search(ipBytes);
            return StringUtils.isBlank(region) ? UNKNOWN_ADDRESS : region;
        } catch (Exception e) {
            log.error("IP地址离线获取城市异常 {}", Util.ipToString(ipBytes));
            return UNKNOWN_ADDRESS;
        }
    }

    public static void close() {
        close(Duration.ofSeconds(10));
    }

    public static void close(final Duration timeout) {
        Ip2Region regionService = ip2Region;
        if (regionService == null) {
            return;
        }
        try {
            long ms = timeout == null ? 10000L : timeout.toMillis();
            regionService.close(ms);
        } catch (Exception e) {
            log.error("Ip2Region服务关闭异常", e);
        }
    }
}
