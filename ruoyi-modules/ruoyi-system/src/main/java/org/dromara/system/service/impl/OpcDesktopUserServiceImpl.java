package org.dromara.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.SortQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.OpcCoworker;
import org.dromara.system.domain.OpcCoworkerMcpTool;
import org.dromara.system.domain.OpcDesktopDevice;
import org.dromara.system.domain.OpcDesktopUser;
import org.dromara.system.domain.OpcDesktopUserApp;
import org.dromara.system.domain.OpcDesktopUserCoworker;
import org.dromara.system.domain.OpcDesktopUserCoworkerTool;
import org.dromara.system.domain.OpcIndustryApp;
import org.dromara.system.domain.bo.OpcDesktopLoginBo;
import org.dromara.system.domain.bo.OpcDesktopUserBo;
import org.dromara.system.domain.bo.OpcDesktopUserCoworkerToolBo;
import org.dromara.system.domain.query.OpcDesktopUserQuery;
import org.dromara.system.domain.vo.OpcDesktopUserCoworkerToolVo;
import org.dromara.system.domain.vo.OpcDesktopUserVo;
import org.dromara.system.mapper.OpcCoworkerMapper;
import org.dromara.system.mapper.OpcCoworkerMcpToolMapper;
import org.dromara.system.mapper.OpcDesktopDeviceMapper;
import org.dromara.system.mapper.OpcDesktopUserAppMapper;
import org.dromara.system.mapper.OpcDesktopUserCoworkerMapper;
import org.dromara.system.mapper.OpcDesktopUserCoworkerToolMapper;
import org.dromara.system.mapper.OpcDesktopUserMapper;
import org.dromara.system.mapper.OpcIndustryAppMapper;
import org.dromara.system.service.IOpcDesktopUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OPC 桌面端登录用户
 */
@RequiredArgsConstructor
@Service
public class OpcDesktopUserServiceImpl extends ServiceImpl<OpcDesktopUserMapper, OpcDesktopUser>
    implements IOpcDesktopUserService {

    public static final String USER_TOKEN_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "opc:desktop:user-token:";
    public static final String USER_SESSION_KEY_PREFIX = GlobalConstants.GLOBAL_REDIS_KEY + "opc:desktop:user-session:";
    public static final Duration USER_TOKEN_TTL = Duration.ofDays(7);
    private static final String DEFAULT_TENANT = "000000";

    private final OpcDesktopDeviceMapper deviceMapper;
    private final OpcDesktopUserAppMapper userAppMapper;
    private final OpcIndustryAppMapper industryAppMapper;
    private final OpcDesktopUserCoworkerMapper userCoworkerMapper;
    private final OpcDesktopUserCoworkerToolMapper userCoworkerToolMapper;
    private final OpcCoworkerMapper coworkerMapper;
    private final OpcCoworkerMcpToolMapper coworkerMcpToolMapper;

    @Override
    public TableDataInfo<OpcDesktopUserVo> queryPageList(OpcDesktopUserQuery query) {
        return PageQuery.of(() -> queryList(query));
    }

    @Override
    public List<OpcDesktopUserVo> queryList(OpcDesktopUserQuery query) {
        return SortQuery.of(() -> {
            List<OpcDesktopUser> users = listByQuery(query);
            Set<Long> ids = users.stream().map(OpcDesktopUser::getUserId).collect(Collectors.toSet());
            Map<Long, Boolean> online = onlineMap(ids);
            Map<Long, List<Long>> appIdsByUser = appIdsByUserIds(ids);
            Map<Long, String> appTitleMap = appTitleMap(appIdsByUser.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet()));
            Map<Long, List<Long>> coworkerIdsByUser = coworkerIdsByUserIds(ids);
            Map<Long, String> coworkerTitleMap = coworkerTitleMap(coworkerIdsByUser.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet()));
            Map<Long, List<OpcDesktopUserCoworkerToolVo>> toolsByUser = coworkerToolsByUserIds(ids);
            return users.stream()
                .map(u -> {
                    OpcDesktopUserVo vo = OpcDesktopUserVo.from(u, online.getOrDefault(u.getUserId(), false));
                    fillAppFields(vo, appIdsByUser.getOrDefault(u.getUserId(), List.of()), appTitleMap);
                    fillCoworkerFields(vo,
                        coworkerIdsByUser.getOrDefault(u.getUserId(), List.of()),
                        coworkerTitleMap,
                        toolsByUser.getOrDefault(u.getUserId(), List.of()));
                    return vo;
                })
                .collect(Collectors.toList());
        });
    }

    private List<OpcDesktopUser> listByQuery(OpcDesktopUserQuery query) {
        LambdaQueryWrapper<OpcDesktopUser> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(query.getUserName()), OpcDesktopUser::getUserName, query.getUserName());
        lqw.like(StringUtils.isNotBlank(query.getNickName()), OpcDesktopUser::getNickName, query.getNickName());
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), OpcDesktopUser::getStatus, query.getStatus());
        lqw.orderByDesc(OpcDesktopUser::getUserId);
        return list(lqw);
    }

    @Override
    public OpcDesktopUserVo queryById(Long userId) {
        OpcDesktopUser user = getById(userId);
        Boolean online = user == null ? null : onlineMap(List.of(userId)).getOrDefault(userId, false);
        OpcDesktopUserVo vo = OpcDesktopUserVo.from(user, online);
        if (vo != null) {
            List<Long> appIds = listAppIdsByUserId(userId);
            Map<Long, String> titles = appTitleMap(new HashSet<>(appIds));
            fillAppFields(vo, appIds, titles);
            List<Long> coworkerIds = listCoworkerIdsByUserId(userId);
            Map<Long, String> coworkerTitles = coworkerTitleMap(new HashSet<>(coworkerIds));
            fillCoworkerFields(vo, coworkerIds, coworkerTitles, listCoworkerToolsByUserId(userId));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(OpcDesktopUserBo bo) {
        checkUserNameUnique(null, bo.getUserName());
        List<Long> appIds = normalizeAppIds(bo.getAppIds());
        List<Long> coworkerIds = normalizeAppIds(bo.getCoworkerIds());
        OpcDesktopUser user = new OpcDesktopUser();
        user.setTenantId(DEFAULT_TENANT);
        user.setUserName(bo.getUserName().trim());
        user.setNickName(StrUtil.blankToDefault(bo.getNickName(), bo.getUserName().trim()));
        user.setPassword(BCrypt.hashpw(bo.getPassword()));
        user.setStatus(StrUtil.blankToDefault(bo.getStatus(), "1"));
        user.setSubscribed(appIds.isEmpty() && coworkerIds.isEmpty() ? "0" : "1");
        user.setRemark(bo.getRemark());
        user.setDelFlag("0");
        boolean ok = save(user);
        if (ok) {
            replaceUserApps(user.getUserId(), appIds);
            replaceUserCoworkers(user.getUserId(), coworkerIds, bo.getCoworkerTools());
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(OpcDesktopUserBo bo) {
        if (bo.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        OpcDesktopUser existing = getById(bo.getUserId());
        if (existing == null) {
            throw new ServiceException("用户不存在");
        }
        checkUserNameUnique(bo.getUserId(), bo.getUserName());
        List<Long> appIds = normalizeAppIds(bo.getAppIds());
        List<Long> coworkerIds = normalizeAppIds(bo.getCoworkerIds());
        existing.setUserName(bo.getUserName().trim());
        existing.setNickName(StrUtil.blankToDefault(bo.getNickName(), bo.getUserName().trim()));
        if (StringUtils.isNotBlank(bo.getStatus())) {
            existing.setStatus(bo.getStatus());
        }
        existing.setSubscribed(appIds.isEmpty() && coworkerIds.isEmpty() ? "0" : "1");
        existing.setRemark(bo.getRemark());
        if (StringUtils.isNotBlank(bo.getPassword())) {
            existing.setPassword(BCrypt.hashpw(bo.getPassword()));
            invalidateSessions(bo.getUserId());
        }
        boolean ok = updateById(existing);
        if (ok) {
            replaceUserApps(bo.getUserId(), appIds);
            replaceUserCoworkers(bo.getUserId(), coworkerIds, bo.getCoworkerTools());
            if ("0".equals(existing.getStatus())) {
                invalidateSessions(bo.getUserId());
            }
        }
        return ok;
    }

    @Override
    public Boolean updateStatus(Long userId, String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new ServiceException("状态值无效");
        }
        OpcDesktopUser user = getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        user.setStatus(status);
        boolean ok = updateById(user);
        if (ok && "0".equals(status)) {
            invalidateSessions(userId);
        }
        return ok;
    }

    @Override
    public Boolean resetPassword(Long userId, String password) {
        if (StringUtils.isBlank(password) || password.length() < 5) {
            throw new ServiceException("密码长度不能少于5位");
        }
        OpcDesktopUser user = getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        user.setPassword(BCrypt.hashpw(password));
        boolean ok = updateById(user);
        if (ok) {
            invalidateSessions(userId);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean ok = removeByIds(ids);
        if (ok) {
            userAppMapper.delete(Wrappers.<OpcDesktopUserApp>lambdaQuery().in(OpcDesktopUserApp::getUserId, ids));
            userCoworkerMapper.delete(Wrappers.<OpcDesktopUserCoworker>lambdaQuery().in(OpcDesktopUserCoworker::getUserId, ids));
            userCoworkerToolMapper.delete(Wrappers.<OpcDesktopUserCoworkerTool>lambdaQuery().in(OpcDesktopUserCoworkerTool::getUserId, ids));
            for (Long id : ids) {
                invalidateSessions(id);
            }
        }
        return ok;
    }

    @Override
    public Map<String, Object> login(OpcDesktopLoginBo bo) {
        return TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> doLogin(bo));
    }

    private Map<String, Object> doLogin(OpcDesktopLoginBo bo) {
        String username = bo.getUsername().trim();
        OpcDesktopUser user = getOne(Wrappers.<OpcDesktopUser>lambdaQuery()
            .eq(OpcDesktopUser::getUserName, username)
            .last("LIMIT 1"));
        if (user == null) {
            throw new ServiceException("账号或密码错误");
        }
        if ("0".equals(user.getStatus())) {
            throw new ServiceException("账号已停用");
        }
        if (!BCrypt.checkpw(bo.getPassword(), user.getPassword())) {
            throw new ServiceException("账号或密码错误");
        }

        Date now = new Date();
        String ip = ServletUtils.getClientIP();
        user.setLastLoginIp(ip);
        user.setLastLoginTime(now);
        updateById(user);

        // 单端：踢掉旧会话
        invalidateSessions(user.getUserId());

        String accessToken = IdUtil.fastSimpleUUID();
        Map<String, Object> session = new HashMap<>(6);
        session.put("userId", user.getUserId());
        session.put("userName", user.getUserName());
        session.put("nickName", user.getNickName());
        RedisUtils.setObject(USER_TOKEN_KEY_PREFIX + accessToken, session, USER_TOKEN_TTL);
        RedisUtils.setObject(USER_SESSION_KEY_PREFIX + user.getUserId(), accessToken, USER_TOKEN_TTL);

        if (StringUtils.isNotBlank(bo.getDeviceId())) {
            bindDevice(bo.getDeviceId().trim(), user.getUserId());
        }

        Map<String, Object> result = new HashMap<>(8);
        result.put("accessToken", accessToken);
        result.put("expireIn", USER_TOKEN_TTL.getSeconds());
        result.put("userId", user.getUserId());
        result.put("userName", user.getUserName());
        result.put("nickName", user.getNickName());
        return result;
    }

    private void bindDevice(String deviceId, Long userId) {
        OpcDesktopDevice device = deviceMapper.selectById(deviceId);
        if (device == null) {
            return;
        }
        device.setUserId(userId);
        device.setUpdateTime(new Date());
        deviceMapper.updateById(device);
    }

    @Override
    public void logout(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            return;
        }
        Map<String, Object> session = RedisUtils.getObject(USER_TOKEN_KEY_PREFIX + accessToken);
        RedisUtils.deleteObject(USER_TOKEN_KEY_PREFIX + accessToken);
        if (session != null && session.get("userId") != null) {
            Long userId = Long.valueOf(String.valueOf(session.get("userId")));
            String current = RedisUtils.getObject(USER_SESSION_KEY_PREFIX + userId);
            if (Objects.equals(current, accessToken)) {
                RedisUtils.deleteObject(USER_SESSION_KEY_PREFIX + userId);
            }
        }
    }

    @Override
    public OpcDesktopUserVo currentUser(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            throw new ServiceException("未登录", 401);
        }
        Map<String, Object> session = RedisUtils.getObject(USER_TOKEN_KEY_PREFIX + accessToken);
        if (session == null || session.get("userId") == null) {
            throw new ServiceException("登录已失效", 401);
        }
        Long userId = Long.valueOf(String.valueOf(session.get("userId")));
        OpcDesktopUser user = TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> getById(userId));
        if (user == null || "0".equals(user.getStatus())) {
            invalidateSessions(userId);
            throw new ServiceException("账号不可用", 401);
        }
        // 续期
        RedisUtils.expire(USER_TOKEN_KEY_PREFIX + accessToken, USER_TOKEN_TTL);
        RedisUtils.expire(USER_SESSION_KEY_PREFIX + userId, USER_TOKEN_TTL);
        return OpcDesktopUserVo.from(user);
    }

    @Override
    public Set<Long> listSubscribedAppIds(String accessToken) {
        Long userId = resolveUserId(accessToken);
        if (userId == null) {
            return Set.of();
        }
        return new HashSet<>(listAppIdsByUserId(userId));
    }

    @Override
    public Set<Long> listSubscribedCoworkerIds(String accessToken) {
        Long userId = resolveUserId(accessToken);
        if (userId == null) {
            return Set.of();
        }
        return new HashSet<>(listCoworkerIdsByUserId(userId));
    }

    @Override
    public Long resolveUserId(String accessToken) {
        if (StringUtils.isBlank(accessToken)) {
            return null;
        }
        Map<String, Object> session = RedisUtils.getObject(USER_TOKEN_KEY_PREFIX + accessToken);
        if (session == null || session.get("userId") == null) {
            return null;
        }
        Long userId = Long.valueOf(String.valueOf(session.get("userId")));
        OpcDesktopUser user = TenantHelper.dynamicTenant(DEFAULT_TENANT, () -> getById(userId));
        if (user == null || "0".equals(user.getStatus())) {
            return null;
        }
        return userId;
    }

    @Override
    public Map<Long, String> nickNameMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OpcDesktopUser> users = list(Wrappers.<OpcDesktopUser>lambdaQuery()
            .select(OpcDesktopUser::getUserId, OpcDesktopUser::getNickName, OpcDesktopUser::getUserName)
            .in(OpcDesktopUser::getUserId, userIds));
        Map<Long, String> map = new HashMap<>(users.size());
        for (OpcDesktopUser u : users) {
            map.put(u.getUserId(), StrUtil.blankToDefault(u.getNickName(), u.getUserName()));
        }
        return map;
    }

    @Override
    public Map<Long, Boolean> onlineMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Boolean> map = new HashMap<>(userIds.size());
        for (Long id : userIds) {
            if (id == null) {
                continue;
            }
            map.put(id, RedisUtils.hasKey(USER_SESSION_KEY_PREFIX + id));
        }
        return map;
    }

    private void checkUserNameUnique(Long userId, String userName) {
        if (StringUtils.isBlank(userName)) {
            throw new ServiceException("账号不能为空");
        }
        boolean exists = exists(Wrappers.<OpcDesktopUser>lambdaQuery()
            .eq(OpcDesktopUser::getUserName, userName.trim())
            .ne(userId != null, OpcDesktopUser::getUserId, userId));
        if (exists) {
            throw new ServiceException("账号已存在");
        }
    }

    private void invalidateSessions(Long userId) {
        if (userId == null) {
            return;
        }
        String token = RedisUtils.getObject(USER_SESSION_KEY_PREFIX + userId);
        if (StringUtils.isNotBlank(token)) {
            RedisUtils.deleteObject(USER_TOKEN_KEY_PREFIX + token);
        }
        RedisUtils.deleteObject(USER_SESSION_KEY_PREFIX + userId);
    }

    private List<Long> listAppIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userAppMapper.selectList(Wrappers.<OpcDesktopUserApp>lambdaQuery()
                .eq(OpcDesktopUserApp::getUserId, userId)
                .select(OpcDesktopUserApp::getAppId))
            .stream()
            .map(OpcDesktopUserApp::getAppId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Map<Long, List<Long>> appIdsByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OpcDesktopUserApp> rows = userAppMapper.selectList(
            Wrappers.<OpcDesktopUserApp>lambdaQuery().in(OpcDesktopUserApp::getUserId, userIds));
        Map<Long, List<Long>> map = new HashMap<>();
        for (OpcDesktopUserApp row : rows) {
            if (row.getUserId() == null || row.getAppId() == null) {
                continue;
            }
            map.computeIfAbsent(row.getUserId(), k -> new ArrayList<>()).add(row.getAppId());
        }
        return map;
    }

    private Map<Long, String> appTitleMap(Collection<Long> appIds) {
        if (appIds == null || appIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return industryAppMapper.selectList(Wrappers.<OpcIndustryApp>lambdaQuery()
                .in(OpcIndustryApp::getAppId, appIds)
                .select(OpcIndustryApp::getAppId, OpcIndustryApp::getTitle))
            .stream()
            .collect(Collectors.toMap(OpcIndustryApp::getAppId, OpcIndustryApp::getTitle, (a, b) -> a));
    }

    private static void fillAppFields(OpcDesktopUserVo vo, List<Long> appIds, Map<Long, String> titleMap) {
        List<Long> ids = appIds == null ? List.of() : appIds;
        vo.setAppIds(ids);
        vo.setSubscribed(ids.isEmpty() ? "0" : "1");
        vo.setAppTitles(ids.stream()
            .map(id -> titleMap.getOrDefault(id, String.valueOf(id)))
            .collect(Collectors.toList()));
    }

    private List<Long> normalizeAppIds(List<Long> appIds) {
        if (appIds == null || appIds.isEmpty()) {
            return List.of();
        }
        return appIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private void replaceUserApps(Long userId, List<Long> appIds) {
        userAppMapper.delete(Wrappers.<OpcDesktopUserApp>lambdaQuery().eq(OpcDesktopUserApp::getUserId, userId));
        if (appIds == null || appIds.isEmpty()) {
            return;
        }
        for (Long appId : appIds) {
            OpcDesktopUserApp row = new OpcDesktopUserApp();
            row.setUserId(userId);
            row.setAppId(appId);
            userAppMapper.insert(row);
        }
    }

    private List<Long> listCoworkerIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userCoworkerMapper.selectList(Wrappers.<OpcDesktopUserCoworker>lambdaQuery()
                .eq(OpcDesktopUserCoworker::getUserId, userId)
                .select(OpcDesktopUserCoworker::getCoworkerId))
            .stream()
            .map(OpcDesktopUserCoworker::getCoworkerId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Map<Long, List<Long>> coworkerIdsByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OpcDesktopUserCoworker> rows = userCoworkerMapper.selectList(
            Wrappers.<OpcDesktopUserCoworker>lambdaQuery().in(OpcDesktopUserCoworker::getUserId, userIds));
        Map<Long, List<Long>> map = new HashMap<>();
        for (OpcDesktopUserCoworker row : rows) {
            if (row.getUserId() == null || row.getCoworkerId() == null) {
                continue;
            }
            map.computeIfAbsent(row.getUserId(), k -> new ArrayList<>()).add(row.getCoworkerId());
        }
        return map;
    }

    private Map<Long, String> coworkerTitleMap(Collection<Long> coworkerIds) {
        if (coworkerIds == null || coworkerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return coworkerMapper.selectList(Wrappers.<OpcCoworker>lambdaQuery()
                .in(OpcCoworker::getCoworkerId, coworkerIds)
                .select(OpcCoworker::getCoworkerId, OpcCoworker::getName))
            .stream()
            .collect(Collectors.toMap(OpcCoworker::getCoworkerId, OpcCoworker::getName, (a, b) -> a));
    }

    private List<OpcDesktopUserCoworkerToolVo> listCoworkerToolsByUserId(Long userId) {
        return coworkerToolsByUserIds(userId == null ? Set.of() : Set.of(userId))
            .getOrDefault(userId, List.of());
    }

    private Map<Long, List<OpcDesktopUserCoworkerToolVo>> coworkerToolsByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OpcDesktopUserCoworkerTool> rows = userCoworkerToolMapper.selectList(
            Wrappers.<OpcDesktopUserCoworkerTool>lambdaQuery().in(OpcDesktopUserCoworkerTool::getUserId, userIds));
        Map<Long, List<OpcDesktopUserCoworkerToolVo>> map = new HashMap<>();
        for (OpcDesktopUserCoworkerTool row : rows) {
            OpcDesktopUserCoworkerToolVo vo = new OpcDesktopUserCoworkerToolVo();
            vo.setCoworkerId(row.getCoworkerId());
            vo.setLogicalMcp(row.getLogicalMcp());
            vo.setToolName(row.getToolName());
            map.computeIfAbsent(row.getUserId(), k -> new ArrayList<>()).add(vo);
        }
        return map;
    }

    private static void fillCoworkerFields(OpcDesktopUserVo vo, List<Long> coworkerIds,
                                           Map<Long, String> titleMap,
                                           List<OpcDesktopUserCoworkerToolVo> tools) {
        List<Long> ids = coworkerIds == null ? List.of() : coworkerIds;
        vo.setCoworkerIds(ids);
        vo.setCoworkerTitles(ids.stream()
            .map(id -> titleMap.getOrDefault(id, String.valueOf(id)))
            .collect(Collectors.toList()));
        vo.setCoworkerTools(tools == null ? List.of() : tools);
    }

    private void replaceUserCoworkers(Long userId, List<Long> coworkerIds,
                                      List<OpcDesktopUserCoworkerToolBo> coworkerTools) {
        userCoworkerMapper.delete(Wrappers.<OpcDesktopUserCoworker>lambdaQuery()
            .eq(OpcDesktopUserCoworker::getUserId, userId));
        userCoworkerToolMapper.delete(Wrappers.<OpcDesktopUserCoworkerTool>lambdaQuery()
            .eq(OpcDesktopUserCoworkerTool::getUserId, userId));
        Set<Long> allowedCoworkers = new HashSet<>(coworkerIds == null ? List.of() : coworkerIds);
        for (Long coworkerId : allowedCoworkers) {
            OpcDesktopUserCoworker row = new OpcDesktopUserCoworker();
            row.setUserId(userId);
            row.setCoworkerId(coworkerId);
            userCoworkerMapper.insert(row);
        }
        if (coworkerTools == null || coworkerTools.isEmpty() || allowedCoworkers.isEmpty()) {
            return;
        }
        // 工具授权必须落在「已订同事」且属于该同事工具上限子集
        Map<Long, Set<String>> allowedTools = coworkerAllowedToolNames(allowedCoworkers);
        Set<String> seen = new HashSet<>();
        for (OpcDesktopUserCoworkerToolBo item : coworkerTools) {
            if (item == null || item.getCoworkerId() == null || StringUtils.isBlank(item.getToolName())) {
                continue;
            }
            if (!allowedCoworkers.contains(item.getCoworkerId())) {
                continue;
            }
            String toolName = item.getToolName().trim();
            Set<String> upper = allowedTools.getOrDefault(item.getCoworkerId(), Set.of());
            if (!upper.isEmpty() && !upper.contains(toolName)) {
                continue;
            }
            String logical = StrUtil.blankToDefault(item.getLogicalMcp(), "");
            String dedupe = item.getCoworkerId() + "|" + logical + "|" + toolName;
            if (!seen.add(dedupe)) {
                continue;
            }
            OpcDesktopUserCoworkerTool row = new OpcDesktopUserCoworkerTool();
            row.setUserId(userId);
            row.setCoworkerId(item.getCoworkerId());
            row.setLogicalMcp(logical);
            row.setToolName(toolName);
            userCoworkerToolMapper.insert(row);
        }
    }

    private Map<Long, Set<String>> coworkerAllowedToolNames(Collection<Long> coworkerIds) {
        if (coworkerIds == null || coworkerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OpcCoworkerMcpTool> rows = coworkerMcpToolMapper.selectList(
            Wrappers.<OpcCoworkerMcpTool>lambdaQuery().in(OpcCoworkerMcpTool::getCoworkerId, coworkerIds));
        Map<Long, Set<String>> map = new HashMap<>();
        for (OpcCoworkerMcpTool row : rows) {
            if (row.getCoworkerId() == null || StringUtils.isBlank(row.getToolName())) {
                continue;
            }
            map.computeIfAbsent(row.getCoworkerId(), k -> new HashSet<>()).add(row.getToolName().trim());
        }
        return map;
    }
}
