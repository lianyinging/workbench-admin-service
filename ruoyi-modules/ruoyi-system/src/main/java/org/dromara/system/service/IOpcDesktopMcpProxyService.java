package org.dromara.system.service;

import org.dromara.system.domain.vo.OpcMcpAccessVo;

public interface IOpcDesktopMcpProxyService {

    OpcMcpAccessVo buildMcpAccess(Long userId);

    /**
     * 代理转发 MCP JSON-RPC / HTTP 请求到网关，并按用户授权过滤 tools。
     * @return 网关响应体（JSON 字符串）
     */
    String proxy(Long userId, String body, String accept);
}
