package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.OpcDesktopLoginBo;
import org.dromara.system.domain.bo.OpcDesktopUserBo;
import org.dromara.system.domain.query.OpcDesktopUserQuery;
import org.dromara.system.domain.vo.OpcDesktopUserVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OPC 桌面端登录用户
 */
public interface IOpcDesktopUserService {

    TableDataInfo<OpcDesktopUserVo> queryPageList(OpcDesktopUserQuery query);

    List<OpcDesktopUserVo> queryList(OpcDesktopUserQuery query);

    OpcDesktopUserVo queryById(Long userId);

    Boolean insertByBo(OpcDesktopUserBo bo);

    Boolean updateByBo(OpcDesktopUserBo bo);

    Boolean updateStatus(Long userId, String status);

    Boolean resetPassword(Long userId, String password);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 桌面端账号密码登录，返回 accessToken 等
     */
    Map<String, Object> login(OpcDesktopLoginBo bo);

    void logout(String accessToken);

    OpcDesktopUserVo currentUser(String accessToken);

    /** 当前 token 对应用户已订阅的行业应用 ID（无效登录返回空集） */
    java.util.Set<Long> listSubscribedAppIds(String accessToken);

    /** 当前 token 对应用户已订阅的同事 ID（无效登录返回空集） */
    java.util.Set<Long> listSubscribedCoworkerIds(String accessToken);

    /** 解析桌面登录 token → userId；无效返回 null */
    Long resolveUserId(String accessToken);

    /**
     * 按 userId 批量取昵称（设备列表展示）
     */
    Map<Long, String> nickNameMap(Collection<Long> userIds);

    /**
     * 按 userId 批量判断是否仍有登录会话（用户在线）
     */
    Map<Long, Boolean> onlineMap(Collection<Long> userIds);
}
