package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.OpcMcpGatewayConfigBo;
import org.dromara.system.domain.query.OpcMcpGatewayConfigQuery;
import org.dromara.system.domain.vo.McpGatewayToolItemVo;
import org.dromara.system.domain.vo.OpcMcpGatewayConfigVo;
import org.dromara.system.service.IOpcMcpGatewayConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 管理端：MCP 网关配置（多条，供同事分别绑定秘钥）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/mcpGateway")
public class OpcMcpGatewayConfigController extends BaseController {

    private final IOpcMcpGatewayConfigService mcpGatewayConfigService;

    @SaCheckPermission("system:mcpGateway:query")
    @GetMapping("/list")
    public TableDataInfo<OpcMcpGatewayConfigVo> list(OpcMcpGatewayConfigQuery query) {
        return mcpGatewayConfigService.queryPageList(query);
    }

    /** 同事表单下拉：仅启用中的配置 */
    @SaCheckPermission(value = {"system:mcpGateway:query", "system:coworker:query", "system:coworker:add", "system:coworker:edit"}, mode = SaMode.OR)
    @GetMapping("/options")
    public R<List<OpcMcpGatewayConfigVo>> options() {
        return R.ok(mcpGatewayConfigService.queryOptions());
    }

    @SaCheckPermission(value = {"system:mcpGateway:query", "system:mcpGateway:edit"}, mode = SaMode.OR)
    @GetMapping("/{configId}")
    public R<OpcMcpGatewayConfigVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long configId) {
        return R.ok(mcpGatewayConfigService.queryById(configId));
    }

    @SaCheckPermission("system:mcpGateway:add")
    @Log(title = "MCP网关配置", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody OpcMcpGatewayConfigBo bo) {
        return toAjax(mcpGatewayConfigService.insertByBo(bo));
    }

    @SaCheckPermission("system:mcpGateway:edit")
    @Log(title = "MCP网关配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody OpcMcpGatewayConfigBo bo) {
        return toAjax(mcpGatewayConfigService.updateByBo(bo));
    }

    @SaCheckPermission("system:mcpGateway:edit")
    @Log(title = "MCP网关状态", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody OpcMcpGatewayConfigBo bo) {
        return toAjax(mcpGatewayConfigService.updateStatus(bo.getConfigId(), bo.getStatus()));
    }

    @SaCheckPermission("system:mcpGateway:remove")
    @Log(title = "MCP网关配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable Long[] configIds) {
        return toAjax(mcpGatewayConfigService.deleteWithValidByIds(Arrays.asList(configIds)));
    }

    @SaCheckPermission(value = {"system:mcpGateway:query", "system:coworker:query", "system:coworker:add", "system:coworker:edit"}, mode = SaMode.OR)
    @GetMapping("/tools")
    public R<List<McpGatewayToolItemVo>> tools(@RequestParam(required = false) Long configId) {
        return R.ok(mcpGatewayConfigService.listGatewayTools(configId));
    }
}
