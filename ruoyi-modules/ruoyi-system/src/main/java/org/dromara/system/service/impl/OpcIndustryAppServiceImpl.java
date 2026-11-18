package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.SortQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcIndustryApp;
import org.dromara.system.domain.bo.OpcIndustryAppBo;
import org.dromara.system.domain.query.OpcIndustryAppQuery;
import org.dromara.system.domain.vo.OpcIndustryAppVo;
import org.dromara.system.mapper.OpcIndustryAppMapper;
import org.dromara.system.service.IOpcIndustryAppService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OpcIndustryAppServiceImpl extends ServiceImpl<OpcIndustryAppMapper, OpcIndustryApp>
    implements IOpcIndustryAppService {

    private static final String DEFAULT_TENANT = "000000";
    private static final Set<String> MODES = Set.of("embed", "plugin", "external");

    @Override
    public TableDataInfo<OpcIndustryAppVo> queryPageList(OpcIndustryAppQuery query) {
        return PageQuery.of(() -> queryList(query));
    }

    @Override
    public List<OpcIndustryAppVo> queryList(OpcIndustryAppQuery query) {
        return SortQuery.of(() -> listByQuery(query).stream().map(OpcIndustryAppVo::from).collect(Collectors.toList()));
    }

    private List<OpcIndustryApp> listByQuery(OpcIndustryAppQuery query) {
        LambdaQueryWrapper<OpcIndustryApp> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(query.getAppKey()), OpcIndustryApp::getAppKey, query.getAppKey());
        lqw.like(StringUtils.isNotBlank(query.getTitle()), OpcIndustryApp::getTitle, query.getTitle());
        lqw.eq(StringUtils.isNotBlank(query.getMode()), OpcIndustryApp::getMode, query.getMode());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), OpcIndustryApp::getStatus, query.getStatus());
        lqw.eq(StringUtils.isNotBlank(query.getVisibility()), OpcIndustryApp::getVisibility, query.getVisibility());
        lqw.orderByAsc(OpcIndustryApp::getSortOrder).orderByDesc(OpcIndustryApp::getAppId);
        return list(lqw);
    }

    @Override
    public OpcIndustryAppVo queryById(Long appId) {
        return OpcIndustryAppVo.from(getById(appId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(OpcIndustryAppBo bo) {
        validateEntry(bo);
        checkAppKeyUnique(null, bo.getAppKey());
        OpcIndustryApp app = toEntity(bo);
        app.setTenantId(DEFAULT_TENANT);
        app.setDelFlag("0");
        if (app.getSortOrder() == null) {
            app.setSortOrder(0);
        }
        if (StringUtils.isBlank(app.getStatus())) {
            app.setStatus("1");
        }
        if (StringUtils.isBlank(app.getVisibility())) {
            app.setVisibility("subscribe");
        }
        return save(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(OpcIndustryAppBo bo) {
        if (bo.getAppId() == null) {
            throw new ServiceException("应用ID不能为空");
        }
        OpcIndustryApp existing = getById(bo.getAppId());
        if (existing == null) {
            throw new ServiceException("应用不存在");
        }
        validateEntry(bo);
        checkAppKeyUnique(bo.getAppId(), bo.getAppKey());
        existing.setAppKey(bo.getAppKey().trim());
        existing.setTitle(bo.getTitle().trim());
        existing.setSubtitle(StrUtil.blankToDefault(bo.getSubtitle(), ""));
        existing.setCategory(StrUtil.blankToDefault(bo.getCategory(), "global"));
        existing.setTags(StrUtil.blankToDefault(bo.getTags(), ""));
        existing.setIcon(StrUtil.blankToDefault(bo.getIcon(), "sparkle"));
        existing.setLogo(normalizeLogo(bo.getLogo()));
        existing.setTone(StrUtil.blankToDefault(bo.getTone(), "#1d4ed8"));
        existing.setMode(bo.getMode().trim());
        existing.setUrl(blankToNull(bo.getUrl()));
        existing.setPluginId(blankToNull(bo.getPluginId()));
        existing.setVisibility(StrUtil.blankToDefault(bo.getVisibility(), "subscribe"));
        existing.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        if (StringUtils.isNotBlank(bo.getStatus())) {
            existing.setStatus(bo.getStatus());
        }
        existing.setRemark(bo.getRemark());
        return updateById(existing);
    }

    @Override
    public Boolean updateStatus(Long appId, String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new ServiceException("状态值无效");
        }
        OpcIndustryApp app = getById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        app.setStatus(status);
        return updateById(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return removeByIds(ids);
    }

    @Override
    public List<OpcIndustryAppVo> listForDesktop(boolean loggedIn, Set<Long> subscribedAppIds) {
        Set<Long> entitled = subscribedAppIds == null ? Set.of() : subscribedAppIds;
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            LambdaQueryWrapper<OpcIndustryApp> lqw = Wrappers.lambdaQuery();
            // 启用中的应用全量返回；能否打开由 accessible 表达（登录 + 勾选该应用）
            lqw.eq(OpcIndustryApp::getStatus, "1");
            lqw.orderByAsc(OpcIndustryApp::getSortOrder).orderByDesc(OpcIndustryApp::getAppId);
            return list(lqw).stream().map(app -> {
                OpcIndustryAppVo vo = OpcIndustryAppVo.from(app);
                boolean entitledApp = app.getAppId() != null && entitled.contains(app.getAppId());
                vo.setAccessible(resolveAccessible(app.getVisibility(), loggedIn, entitledApp));
                return vo;
            }).collect(Collectors.toList());
        });
    }

    /**
     * public：公开可开；login：需登录；subscribe：需登录且勾选订阅该应用
     */
    private static boolean resolveAccessible(String visibility, boolean loggedIn, boolean entitledApp) {
        String vis = StrUtil.blankToDefault(visibility, "public");
        if ("public".equals(vis)) {
            return true;
        }
        if ("login".equals(vis)) {
            return loggedIn;
        }
        if ("subscribe".equals(vis)) {
            return loggedIn && entitledApp;
        }
        return false;
    }

    private OpcIndustryApp toEntity(OpcIndustryAppBo bo) {
        OpcIndustryApp app = new OpcIndustryApp();
        app.setAppKey(bo.getAppKey().trim());
        app.setTitle(bo.getTitle().trim());
        app.setSubtitle(StrUtil.blankToDefault(bo.getSubtitle(), ""));
        app.setCategory(StrUtil.blankToDefault(bo.getCategory(), "global"));
        app.setTags(StrUtil.blankToDefault(bo.getTags(), ""));
        app.setIcon(StrUtil.blankToDefault(bo.getIcon(), "sparkle"));
        app.setLogo(normalizeLogo(bo.getLogo()));
        app.setTone(StrUtil.blankToDefault(bo.getTone(), "#1d4ed8"));
        app.setMode(bo.getMode().trim());
        app.setUrl(blankToNull(bo.getUrl()));
        app.setPluginId(blankToNull(bo.getPluginId()));
        app.setVisibility(StrUtil.blankToDefault(bo.getVisibility(), "subscribe"));
        app.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        app.setStatus(StrUtil.blankToDefault(bo.getStatus(), "1"));
        app.setRemark(bo.getRemark());
        return app;
    }

    private void validateEntry(OpcIndustryAppBo bo) {
        String mode = bo.getMode() == null ? "" : bo.getMode().trim();
        if (!MODES.contains(mode)) {
            throw new ServiceException("模式仅支持 embed / plugin / external");
        }
        if (("embed".equals(mode) || "external".equals(mode)) && StringUtils.isBlank(bo.getUrl())) {
            throw new ServiceException("Web/外开模式必须配置 URL");
        }
        if ("plugin".equals(mode) && StringUtils.isBlank(bo.getPluginId())) {
            throw new ServiceException("插件模式必须配置 pluginId");
        }
        String vis = StrUtil.blankToDefault(bo.getVisibility(), "subscribe");
        if (!"public".equals(vis) && !"login".equals(vis) && !"subscribe".equals(vis)) {
            throw new ServiceException("访问权限仅支持 public / login / subscribe");
        }
        String logo = bo.getLogo();
        if (StringUtils.isNotBlank(logo)) {
            if (!logo.startsWith("data:image/")) {
                throw new ServiceException("Logo 须为图片 base64（data:image/...）");
            }
            // ~300KB payload 上限，避免超大落库
            if (logo.length() > 400_000) {
                throw new ServiceException("Logo 过大，请压缩到约 200KB 以内");
            }
        }
    }

    private static String normalizeLogo(String logo) {
        return StringUtils.isBlank(logo) ? null : logo.trim();
    }

    private void checkAppKeyUnique(Long appId, String appKey) {
        boolean exists = exists(Wrappers.<OpcIndustryApp>lambdaQuery()
            .eq(OpcIndustryApp::getAppKey, appKey.trim())
            .ne(appId != null, OpcIndustryApp::getAppId, appId));
        if (exists) {
            throw new ServiceException("应用键已存在");
        }
    }

    private static String blankToNull(String v) {
        return StringUtils.isBlank(v) ? null : v.trim();
    }
}
