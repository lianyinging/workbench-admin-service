package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.OpcIndustryAppBo;
import org.dromara.system.domain.query.OpcIndustryAppQuery;
import org.dromara.system.domain.vo.OpcIndustryAppVo;

import java.util.Collection;
import java.util.List;

public interface IOpcIndustryAppService {

    TableDataInfo<OpcIndustryAppVo> queryPageList(OpcIndustryAppQuery query);

    List<OpcIndustryAppVo> queryList(OpcIndustryAppQuery query);

    OpcIndustryAppVo queryById(Long appId);

    Boolean insertByBo(OpcIndustryAppBo bo);

    Boolean updateByBo(OpcIndustryAppBo bo);

    Boolean updateStatus(Long appId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 桌面端清单：启用项全量返回；accessible 按登录 + 已勾选应用计算
     */
    List<OpcIndustryAppVo> listForDesktop(boolean loggedIn, java.util.Set<Long> subscribedAppIds);
}
