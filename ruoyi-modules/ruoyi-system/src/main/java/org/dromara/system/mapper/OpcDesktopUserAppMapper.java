package org.dromara.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.dromara.system.domain.OpcDesktopUserApp;

/** 关联表无 tenant_id，忽略租户行插件 */
@InterceptorIgnore(tenantLine = "true")
public interface OpcDesktopUserAppMapper extends BaseMapper<OpcDesktopUserApp> {
}
