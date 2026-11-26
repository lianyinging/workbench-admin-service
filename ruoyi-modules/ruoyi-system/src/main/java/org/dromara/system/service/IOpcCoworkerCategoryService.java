package org.dromara.system.service;

import org.dromara.system.domain.bo.OpcCoworkerCategoryBo;
import org.dromara.system.domain.vo.OpcCoworkerCategorySidebarVo;
import org.dromara.system.domain.vo.OpcCoworkerCategoryVo;

import java.util.Collection;
import java.util.List;

public interface IOpcCoworkerCategoryService {

    List<OpcCoworkerCategoryVo> queryList();

    /** 左侧栏：全部/未分类计数 + 分类列表 */
    OpcCoworkerCategorySidebarVo querySidebar();

    /** 启用中的分类（表单下拉） */
    List<OpcCoworkerCategoryVo> queryEnabledOptions();

    OpcCoworkerCategoryVo queryById(Long categoryId);

    Boolean insertByBo(OpcCoworkerCategoryBo bo);

    Boolean updateByBo(OpcCoworkerCategoryBo bo);

    /** 有同事绑定则禁止删除 */
    Boolean deleteWithValidByIds(Collection<Long> ids);
}
