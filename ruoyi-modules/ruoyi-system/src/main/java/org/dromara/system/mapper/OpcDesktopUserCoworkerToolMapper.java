package org.dromara.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.dromara.system.domain.OpcDesktopUserCoworkerTool;

@InterceptorIgnore(tenantLine = "true")
public interface OpcDesktopUserCoworkerToolMapper extends BaseMapper<OpcDesktopUserCoworkerTool> {
}
