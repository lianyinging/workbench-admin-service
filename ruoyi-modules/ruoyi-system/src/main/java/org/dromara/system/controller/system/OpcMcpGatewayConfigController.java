package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.OpcMcpGatewayConfigBo;
import org.dromara.system.domain.vo.McpGatewayToolItemVo;
import org.dromara.system.domain.vo.OpcMcpGatewayConfigVo;
import org.dromara.system.service.IOpcMcpGatewayConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：MCP 网关组织配置
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/mcpGateway")
public class OpcMcpGatewayConfigController extends BaseController {

    private final IOpcMcpGatewayConfigService mcpGatewayConfigService;

    @SaCheckPermission("system:mcpGateway:query")
    @GetMapping
    public R<OpcMcpGatewayConfigVo> get() {
        return R.ok(mcpGatewayConfigService.getConfig());
    }

    @SaCheckPermission("system:mcpGateway:edit")
    @Log(title = "MCP网关配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> save(@Validated @RequestBody OpcMcpGatewayConfigBo bo) {
        return toAjax(mcpGatewayConfigService.saveConfig(bo));
    }

    @SaCheckPermission("system:mcpGateway:query")
    @GetMapping("/tools")
    public R<List<McpGatewayToolItemVo>> tools() {
        return R.ok(mcpGatewayConfigService.listGatewayTools());
    }
}
