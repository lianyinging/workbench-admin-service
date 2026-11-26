package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.SortQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcCoworker;
import org.dromara.system.domain.OpcCoworkerCategory;
import org.dromara.system.domain.OpcCoworkerMcpTool;
import org.dromara.system.domain.OpcMcpGatewayConfig;
import org.dromara.system.domain.bo.OpcCoworkerBo;
import org.dromara.system.domain.bo.OpcCoworkerToolItemBo;
import org.dromara.system.domain.query.OpcCoworkerQuery;
import org.dromara.system.domain.vo.OpcCoworkerToolItemVo;
import org.dromara.system.domain.vo.OpcCoworkerVo;
import org.dromara.system.domain.vo.OpcDesktopCoworkerPackageVo;
import org.dromara.system.mapper.OpcCoworkerCategoryMapper;
import org.dromara.system.mapper.OpcCoworkerMapper;
import org.dromara.system.mapper.OpcCoworkerMcpToolMapper;
import org.dromara.system.mapper.OpcMcpGatewayConfigMapper;
import org.dromara.system.service.IOpcCoworkerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OpcCoworkerServiceImpl extends ServiceImpl<OpcCoworkerMapper, OpcCoworker>
    implements IOpcCoworkerService {

    private static final String DEFAULT_TENANT = "000000";
    private static final Set<String> VISIBILITIES = Set.of("public", "login", "subscribe");

    private final OpcCoworkerMcpToolMapper coworkerMcpToolMapper;
    private final OpcMcpGatewayConfigMapper mcpGatewayConfigMapper;
    private final OpcCoworkerCategoryMapper coworkerCategoryMapper;

    @Override
    public TableDataInfo<OpcCoworkerVo> queryPageList(OpcCoworkerQuery query) {
        return PageQuery.of(() -> queryList(query));
    }

    @Override
    public List<OpcCoworkerVo> queryList(OpcCoworkerQuery query) {
        return SortQuery.of(() -> {
            List<OpcCoworker> list = listByQuery(query);
            Map<Long, List<OpcCoworkerToolItemVo>> tools = toolsByCoworkerIds(
                list.stream().map(OpcCoworker::getCoworkerId).collect(Collectors.toSet()));
            Map<Long, String> mcpNames = mcpConfigNameMap(list);
            Map<Long, String> categoryNames = categoryNameMap(list);
            return list.stream().map(c -> {
                OpcCoworkerVo vo = OpcCoworkerVo.from(c);
                vo.setTools(tools.getOrDefault(c.getCoworkerId(), List.of()));
                if (c.getMcpConfigId() != null) {
                    vo.setMcpConfigName(mcpNames.get(c.getMcpConfigId()));
                }
                if (c.getCategoryId() != null) {
                    vo.setCategoryName(categoryNames.get(c.getCategoryId()));
                }
                return vo;
            }).collect(Collectors.toList());
        });
    }

    private Map<Long, String> mcpConfigNameMap(List<OpcCoworker> list) {
        Set<Long> ids = list.stream().map(OpcCoworker::getMcpConfigId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mcpGatewayConfigMapper.selectList(Wrappers.<OpcMcpGatewayConfig>lambdaQuery()
                .in(OpcMcpGatewayConfig::getConfigId, ids)
                .select(OpcMcpGatewayConfig::getConfigId, OpcMcpGatewayConfig::getConfigName))
            .stream()
            .collect(Collectors.toMap(OpcMcpGatewayConfig::getConfigId, OpcMcpGatewayConfig::getConfigName, (a, b) -> a));
    }

    private Map<Long, String> categoryNameMap(List<OpcCoworker> list) {
        Set<Long> ids = list.stream().map(OpcCoworker::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return coworkerCategoryMapper.selectList(Wrappers.<OpcCoworkerCategory>lambdaQuery()
                .in(OpcCoworkerCategory::getCategoryId, ids)
                .select(OpcCoworkerCategory::getCategoryId, OpcCoworkerCategory::getCategoryName))
            .stream()
            .collect(Collectors.toMap(OpcCoworkerCategory::getCategoryId, OpcCoworkerCategory::getCategoryName, (a, b) -> a));
    }

    private List<OpcCoworker> listByQuery(OpcCoworkerQuery query) {
        LambdaQueryWrapper<OpcCoworker> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(query.getPersonaKey()), OpcCoworker::getPersonaKey, query.getPersonaKey());
        lqw.like(StringUtils.isNotBlank(query.getName()), OpcCoworker::getName, query.getName());
        lqw.eq(StringUtils.isNotBlank(query.getGroupKey()), OpcCoworker::getGroupKey, query.getGroupKey());
        if (Boolean.TRUE.equals(query.getUncategorized())) {
            lqw.isNull(OpcCoworker::getCategoryId);
        } else if (query.getCategoryId() != null) {
            lqw.eq(OpcCoworker::getCategoryId, query.getCategoryId());
        }
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), OpcCoworker::getStatus, query.getStatus());
        lqw.eq(StringUtils.isNotBlank(query.getVisibility()), OpcCoworker::getVisibility, query.getVisibility());
        lqw.orderByAsc(OpcCoworker::getSortOrder).orderByDesc(OpcCoworker::getCoworkerId);
        return list(lqw);
    }

    @Override
    public OpcCoworkerVo queryById(Long coworkerId) {
        OpcCoworker entity = getById(coworkerId);
        OpcCoworkerVo vo = OpcCoworkerVo.from(entity);
        if (vo != null) {
            vo.setTools(toolsByCoworkerIds(Set.of(coworkerId)).getOrDefault(coworkerId, List.of()));
            if (entity != null && entity.getCategoryId() != null) {
                OpcCoworkerCategory cat = coworkerCategoryMapper.selectById(entity.getCategoryId());
                if (cat != null) {
                    vo.setCategoryName(cat.getCategoryName());
                }
            }
        }
        return vo;
    }

    @Override
    public List<OpcCoworkerVo> listEnabledOptions() {
        LambdaQueryWrapper<OpcCoworker> lqw = Wrappers.lambdaQuery();
        lqw.eq(OpcCoworker::getStatus, "1");
        lqw.orderByAsc(OpcCoworker::getSortOrder).orderByDesc(OpcCoworker::getCoworkerId);
        List<OpcCoworker> list = list(lqw);
        Map<Long, List<OpcCoworkerToolItemVo>> tools = toolsByCoworkerIds(
            list.stream().map(OpcCoworker::getCoworkerId).collect(Collectors.toSet()));
        return list.stream().map(c -> {
            OpcCoworkerVo vo = OpcCoworkerVo.from(c);
            vo.setTools(tools.getOrDefault(c.getCoworkerId(), List.of()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(OpcCoworkerBo bo) {
        validateVisibility(bo.getVisibility());
        checkPersonaUnique(null, bo.getPersonaKey());
        if (StringUtils.isBlank(bo.getPackageZipBase64())) {
            throw new ServiceException("新增必须上传同事包 zip");
        }
        OpcCoworker entity = new OpcCoworker();
        entity.setTenantId(DEFAULT_TENANT);
        fillMeta(entity, bo);
        applyPackage(entity, bo.getPackageZipBase64());
        entity.setDelFlag("0");
        boolean ok = save(entity);
        if (ok) {
            replaceTools(entity.getCoworkerId(), bo.getTools());
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(OpcCoworkerBo bo) {
        if (bo.getCoworkerId() == null) {
            throw new ServiceException("同事ID不能为空");
        }
        OpcCoworker existing = getById(bo.getCoworkerId());
        if (existing == null) {
            throw new ServiceException("同事不存在");
        }
        validateVisibility(bo.getVisibility());
        checkPersonaUnique(bo.getCoworkerId(), bo.getPersonaKey());
        fillMeta(existing, bo);
        if (StringUtils.isNotBlank(bo.getPackageZipBase64())) {
            applyPackage(existing, bo.getPackageZipBase64());
        }
        boolean ok = updateById(existing);
        if (ok) {
            replaceTools(existing.getCoworkerId(), bo.getTools());
        }
        return ok;
    }

    @Override
    public Boolean updateStatus(Long coworkerId, String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new ServiceException("状态值无效");
        }
        OpcCoworker entity = getById(coworkerId);
        if (entity == null) {
            throw new ServiceException("同事不存在");
        }
        entity.setStatus(status);
        return updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean ok = removeByIds(ids);
        if (ok) {
            coworkerMcpToolMapper.delete(Wrappers.<OpcCoworkerMcpTool>lambdaQuery()
                .in(OpcCoworkerMcpTool::getCoworkerId, ids));
        }
        return ok;
    }

    @Override
    public List<OpcCoworkerVo> listForDesktop(boolean loggedIn, Set<Long> subscribedCoworkerIds) {
        Set<Long> entitled = subscribedCoworkerIds == null ? Set.of() : subscribedCoworkerIds;
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            LambdaQueryWrapper<OpcCoworker> lqw = Wrappers.lambdaQuery();
            lqw.eq(OpcCoworker::getStatus, "1");
            lqw.orderByAsc(OpcCoworker::getSortOrder).orderByDesc(OpcCoworker::getCoworkerId);
            List<OpcCoworker> list = list(lqw);
            Map<Long, List<OpcCoworkerToolItemVo>> tools = toolsByCoworkerIds(
                list.stream().map(OpcCoworker::getCoworkerId).collect(Collectors.toSet()));
            return list.stream().map(c -> {
                OpcCoworkerVo vo = OpcCoworkerVo.from(c);
                boolean entitledCw = c.getCoworkerId() != null && entitled.contains(c.getCoworkerId());
                vo.setAccessible(resolveAccessible(c.getVisibility(), loggedIn, entitledCw));
                vo.setTools(tools.getOrDefault(c.getCoworkerId(), List.of()));
                return vo;
            }).collect(Collectors.toList());
        });
    }

    @Override
    public OpcDesktopCoworkerPackageVo getPackageForDesktop(String personaKey, Long userId,
                                                            Set<Long> subscribedCoworkerIds) {
        if (StringUtils.isBlank(personaKey)) {
            throw new ServiceException("personaKey 不能为空");
        }
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> {
            OpcCoworker entity = getOne(Wrappers.<OpcCoworker>lambdaQuery()
                .eq(OpcCoworker::getPersonaKey, personaKey.trim())
                .eq(OpcCoworker::getStatus, "1")
                .last("LIMIT 1"));
            if (entity == null) {
                throw new ServiceException("同事不存在或已停用");
            }
            boolean loggedIn = userId != null;
            boolean entitled = subscribedCoworkerIds != null
                && entity.getCoworkerId() != null
                && subscribedCoworkerIds.contains(entity.getCoworkerId());
            if (!resolveAccessible(entity.getVisibility(), loggedIn, entitled)) {
                throw new ServiceException("无权下载该同事包", 403);
            }
            if (StringUtils.isBlank(entity.getPackageZip())) {
                throw new ServiceException("同事包尚未上传");
            }
            OpcDesktopCoworkerPackageVo vo = new OpcDesktopCoworkerPackageVo();
            vo.setPersonaKey(entity.getPersonaKey());
            vo.setPackageSha256(entity.getPackageSha256());
            vo.setPackageZipBase64(entity.getPackageZip());
            return vo;
        });
    }

    private void fillMeta(OpcCoworker entity, OpcCoworkerBo bo) {
        entity.setPersonaKey(bo.getPersonaKey().trim());
        entity.setName(bo.getName().trim());
        entity.setTagline(StrUtil.blankToDefault(bo.getTagline(), ""));
        entity.setDescription(StrUtil.blankToDefault(bo.getDescription(), ""));
        entity.setIcon(StrUtil.blankToDefault(bo.getIcon(), ""));
        entity.setGroupKey(StrUtil.blankToDefault(bo.getGroupKey(), "general"));
        if (bo.getCategoryId() != null) {
            OpcCoworkerCategory cat = coworkerCategoryMapper.selectById(bo.getCategoryId());
            if (cat == null || !"1".equals(cat.getStatus())) {
                throw new ServiceException("所选分类不存在或已停用");
            }
        }
        entity.setCategoryId(bo.getCategoryId());
        entity.setVersion(StrUtil.blankToDefault(bo.getVersion(), "1.0.0"));
        entity.setVisibility(StrUtil.blankToDefault(bo.getVisibility(), "subscribe"));
        entity.setMcpConfigId(bo.getMcpConfigId());
        entity.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        entity.setStatus(StrUtil.blankToDefault(bo.getStatus(), "1"));
        entity.setRemark(bo.getRemark());
    }

    private void applyPackage(OpcCoworker entity, String packageZipBase64) {
        String raw = packageZipBase64.trim();
        String b64 = raw;
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            b64 = raw.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(b64.replaceAll("\\s", ""));
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("同事包 base64 无效");
        }
        if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
            throw new ServiceException("同事包必须是 zip 文件");
        }
        rejectHardcodedUpstreamUrls(bytes);
        entity.setPackageZip(raw);
        entity.setPackageSize((long) bytes.length);
        entity.setPackageSha256(DigestUtil.sha256Hex(bytes));
    }

    /** 仅扫描 zip 内常见文本条目，禁止硬编码上游 http(s) URL */
    private void rejectHardcodedUpstreamUrls(byte[] zipBytes) {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
            new java.io.ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = StrUtil.blankToDefault(entry.getName(), "").toLowerCase();
                boolean textLike = name.endsWith(".md") || name.endsWith(".txt") || name.endsWith(".json")
                    || name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".toml")
                    || name.endsWith(".prompt") || name.contains("manifest");
                if (!textLike) {
                    continue;
                }
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                int n;
                int total = 0;
                while ((n = zis.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                    total += n;
                    if (total > 512 * 1024) {
                        break;
                    }
                }
                String text = bos.toString(StandardCharsets.UTF_8);
                if (text.matches("(?is).*https?://[\\w.-]+.*")) {
                    throw new ServiceException("同事包内禁止硬编码上游 URL（" + entry.getName() + "），请仅使用逻辑 MCP 名");
                }
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("同事包 zip 无法解析：" + ex.getMessage());
        }
    }

    private void replaceTools(Long coworkerId, List<OpcCoworkerToolItemBo> tools) {
        coworkerMcpToolMapper.delete(Wrappers.<OpcCoworkerMcpTool>lambdaQuery()
            .eq(OpcCoworkerMcpTool::getCoworkerId, coworkerId));
        if (tools == null || tools.isEmpty()) {
            return;
        }
        for (OpcCoworkerToolItemBo item : tools) {
            if (item == null || StringUtils.isBlank(item.getToolName())) {
                continue;
            }
            OpcCoworkerMcpTool row = new OpcCoworkerMcpTool();
            row.setCoworkerId(coworkerId);
            row.setLogicalMcp(StrUtil.blankToDefault(item.getLogicalMcp(), ""));
            row.setToolId(StrUtil.blankToDefault(item.getToolId(), ""));
            row.setToolName(item.getToolName().trim());
            coworkerMcpToolMapper.insert(row);
        }
    }

    private Map<Long, List<OpcCoworkerToolItemVo>> toolsByCoworkerIds(Collection<Long> coworkerIds) {
        if (coworkerIds == null || coworkerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OpcCoworkerMcpTool> rows = coworkerMcpToolMapper.selectList(
            Wrappers.<OpcCoworkerMcpTool>lambdaQuery().in(OpcCoworkerMcpTool::getCoworkerId, coworkerIds));
        Map<Long, List<OpcCoworkerToolItemVo>> map = new HashMap<>();
        for (OpcCoworkerMcpTool row : rows) {
            OpcCoworkerToolItemVo vo = new OpcCoworkerToolItemVo();
            vo.setLogicalMcp(row.getLogicalMcp());
            vo.setToolId(row.getToolId());
            vo.setToolName(row.getToolName());
            map.computeIfAbsent(row.getCoworkerId(), k -> new ArrayList<>()).add(vo);
        }
        return map;
    }

    private void checkPersonaUnique(Long coworkerId, String personaKey) {
        if (StringUtils.isBlank(personaKey)) {
            throw new ServiceException("personaKey 不能为空");
        }
        boolean exists = exists(Wrappers.<OpcCoworker>lambdaQuery()
            .eq(OpcCoworker::getPersonaKey, personaKey.trim())
            .ne(coworkerId != null, OpcCoworker::getCoworkerId, coworkerId));
        if (exists) {
            throw new ServiceException("personaKey 已存在");
        }
    }

    private void validateVisibility(String visibility) {
        String vis = StrUtil.blankToDefault(visibility, "subscribe");
        if (!VISIBILITIES.contains(vis)) {
            throw new ServiceException("访问权限仅支持 public / login / subscribe");
        }
    }

    private static boolean resolveAccessible(String visibility, boolean loggedIn, boolean entitled) {
        String vis = StrUtil.blankToDefault(visibility, "subscribe");
        if ("public".equals(vis)) {
            return true;
        }
        if ("login".equals(vis)) {
            return loggedIn;
        }
        if ("subscribe".equals(vis)) {
            return loggedIn && entitled;
        }
        return false;
    }
}
