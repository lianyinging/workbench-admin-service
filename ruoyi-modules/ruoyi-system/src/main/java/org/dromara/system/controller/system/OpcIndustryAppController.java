package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.constraints.NotEmpty;
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
import org.dromara.system.domain.bo.OpcIndustryAppBo;
import org.dromara.system.domain.query.OpcIndustryAppQuery;
import org.dromara.system.domain.vo.OpcIndustryAppVo;
import org.dromara.system.service.IOpcIndustryAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：行业应用配置
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/industryApp")
public class OpcIndustryAppController extends BaseController {

    private final IOpcIndustryAppService industryAppService;

    @SaCheckPermission("system:industryApp:list")
    @GetMapping("/list")
    public TableDataInfo<OpcIndustryAppVo> list(OpcIndustryAppQuery query) {
        return industryAppService.queryPageList(query);
    }

    @SaCheckPermission(value = {"system:industryApp:query", "system:industryApp:edit"}, mode = SaMode.OR)
    @GetMapping("/{appId}")
    public R<OpcIndustryAppVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long appId) {
        return R.ok(industryAppService.queryById(appId));
    }

    @SaCheckPermission("system:industryApp:add")
    @Log(title = "行业应用", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody OpcIndustryAppBo bo) {
        return toAjax(industryAppService.insertByBo(bo));
    }

    @SaCheckPermission("system:industryApp:edit")
    @Log(title = "行业应用", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody OpcIndustryAppBo bo) {
        return toAjax(industryAppService.updateByBo(bo));
    }

    @SaCheckPermission("system:industryApp:edit")
    @Log(title = "行业应用", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody OpcIndustryAppBo bo) {
        return toAjax(industryAppService.updateStatus(bo.getAppId(), bo.getStatus()));
    }

    @SaCheckPermission("system:industryApp:remove")
    @Log(title = "行业应用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] appIds) {
        return toAjax(industryAppService.deleteWithValidByIds(List.of(appIds)));
    }
}
