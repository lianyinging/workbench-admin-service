package org.dromara.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.dromara.system.domain.OpcDesktopUserCoworker;

@InterceptorIgnore(tenantLine = "true")
public interface OpcDesktopUserCoworkerMapper extends BaseMapper<OpcDesktopUserCoworker> {
}
