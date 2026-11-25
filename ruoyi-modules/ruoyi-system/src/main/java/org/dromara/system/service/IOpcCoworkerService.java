package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.OpcCoworkerBo;
import org.dromara.system.domain.query.OpcCoworkerQuery;
import org.dromara.system.domain.vo.OpcCoworkerVo;
import org.dromara.system.domain.vo.OpcDesktopCoworkerPackageVo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface IOpcCoworkerService {

    TableDataInfo<OpcCoworkerVo> queryPageList(OpcCoworkerQuery query);

    List<OpcCoworkerVo> queryList(OpcCoworkerQuery query);

    OpcCoworkerVo queryById(Long coworkerId);

    List<OpcCoworkerVo> listEnabledOptions();

    Boolean insertByBo(OpcCoworkerBo bo);

    Boolean updateByBo(OpcCoworkerBo bo);

    Boolean updateStatus(Long coworkerId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    List<OpcCoworkerVo> listForDesktop(boolean loggedIn, Set<Long> subscribedCoworkerIds);

    OpcDesktopCoworkerPackageVo getPackageForDesktop(String personaKey, Long userId, Set<Long> subscribedCoworkerIds);
}
