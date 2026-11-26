package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcCoworker;
import org.dromara.system.domain.OpcCoworkerCategory;
import org.dromara.system.domain.bo.OpcCoworkerCategoryBo;
import org.dromara.system.domain.vo.OpcCoworkerCategorySidebarVo;
import org.dromara.system.domain.vo.OpcCoworkerCategoryVo;
import org.dromara.system.mapper.OpcCoworkerCategoryMapper;
import org.dromara.system.mapper.OpcCoworkerMapper;
import org.dromara.system.service.IOpcCoworkerCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OpcCoworkerCategoryServiceImpl extends ServiceImpl<OpcCoworkerCategoryMapper, OpcCoworkerCategory>
    implements IOpcCoworkerCategoryService {

    private static final String DEFAULT_TENANT = "000000";

    private final OpcCoworkerMapper coworkerMapper;

    @Override
    public List<OpcCoworkerCategoryVo> queryList() {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            List<OpcCoworkerCategory> list = list(Wrappers.<OpcCoworkerCategory>lambdaQuery()
                .orderByAsc(OpcCoworkerCategory::getSortOrder)
                .orderByDesc(OpcCoworkerCategory::getCategoryId));
            Map<Long, Long> countMap = coworkerCountByCategory();
            return list.stream().map(c -> {
                OpcCoworkerCategoryVo vo = OpcCoworkerCategoryVo.from(c);
                vo.setCoworkerCount(countMap.getOrDefault(c.getCategoryId(), 0L));
                return vo;
            }).collect(Collectors.toList());
        });
    }

    @Override
    public OpcCoworkerCategorySidebarVo querySidebar() {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcCoworkerCategorySidebarVo sidebar = new OpcCoworkerCategorySidebarVo();
            sidebar.setCategories(queryList());
            sidebar.setAllCount(coworkerMapper.selectCount(Wrappers.<OpcCoworker>lambdaQuery()));
            sidebar.setUncategorizedCount(coworkerMapper.selectCount(Wrappers.<OpcCoworker>lambdaQuery()
                .isNull(OpcCoworker::getCategoryId)));
            return sidebar;
        });
    }

    @Override
    public List<OpcCoworkerCategoryVo> queryEnabledOptions() {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () ->
            list(Wrappers.<OpcCoworkerCategory>lambdaQuery()
                .eq(OpcCoworkerCategory::getStatus, "1")
                .orderByAsc(OpcCoworkerCategory::getSortOrder)
                .orderByDesc(OpcCoworkerCategory::getCategoryId))
                .stream()
                .map(OpcCoworkerCategoryVo::from)
                .collect(Collectors.toList()));
    }

    @Override
    public OpcCoworkerCategoryVo queryById(Long categoryId) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> OpcCoworkerCategoryVo.from(getById(categoryId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(OpcCoworkerCategoryBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            checkNameUnique(null, bo.getCategoryName());
            OpcCoworkerCategory entity = new OpcCoworkerCategory();
            entity.setTenantId(DEFAULT_TENANT);
            entity.setDelFlag("0");
            applyBo(entity, bo, true);
            return save(entity);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(OpcCoworkerCategoryBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            if (bo.getCategoryId() == null) {
                throw new ServiceException("分类ID不能为空");
            }
            OpcCoworkerCategory existing = getById(bo.getCategoryId());
            if (existing == null) {
                throw new ServiceException("分类不存在");
            }
            checkNameUnique(bo.getCategoryId(), bo.getCategoryName());
            applyBo(existing, bo, false);
            return updateById(existing);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            if (ids == null || ids.isEmpty()) {
                return false;
            }
            long bound = coworkerMapper.selectCount(Wrappers.<OpcCoworker>lambdaQuery()
                .in(OpcCoworker::getCategoryId, ids));
            if (bound > 0) {
                throw new ServiceException("分类下仍有同事，无法删除");
            }
            return removeByIds(ids);
        });
    }

    private Map<Long, Long> coworkerCountByCategory() {
        List<OpcCoworker> rows = coworkerMapper.selectList(Wrappers.<OpcCoworker>lambdaQuery()
            .select(OpcCoworker::getCategoryId)
            .isNotNull(OpcCoworker::getCategoryId));
        Map<Long, Long> map = new HashMap<>();
        for (OpcCoworker row : rows) {
            if (row.getCategoryId() == null) {
                continue;
            }
            map.merge(row.getCategoryId(), 1L, Long::sum);
        }
        return map;
    }

    private void checkNameUnique(Long categoryId, String name) {
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("分类名称不能为空");
        }
        String trimmed = name.trim();
        long cnt = count(Wrappers.<OpcCoworkerCategory>lambdaQuery()
            .eq(OpcCoworkerCategory::getCategoryName, trimmed)
            .ne(categoryId != null, OpcCoworkerCategory::getCategoryId, categoryId));
        if (cnt > 0) {
            throw new ServiceException("分类名称已存在");
        }
    }

    private void applyBo(OpcCoworkerCategory entity, OpcCoworkerCategoryBo bo, boolean creating) {
        entity.setCategoryName(bo.getCategoryName().trim());
        if (bo.getSortOrder() != null) {
            entity.setSortOrder(bo.getSortOrder());
        } else if (creating && entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        if (StringUtils.isNotBlank(bo.getStatus())) {
            entity.setStatus(bo.getStatus());
        } else if (creating) {
            entity.setStatus("1");
        }
        if (bo.getRemark() != null) {
            entity.setRemark(StrUtil.blankToDefault(bo.getRemark(), null));
        }
    }
}
