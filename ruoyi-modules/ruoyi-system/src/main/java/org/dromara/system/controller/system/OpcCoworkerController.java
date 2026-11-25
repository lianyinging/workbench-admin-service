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
import org.dromara.system.domain.bo.OpcCoworkerBo;
import org.dromara.system.domain.query.OpcCoworkerQuery;
import org.dromara.system.domain.vo.OpcCoworkerVo;
import org.dromara.system.service.IOpcCoworkerService;
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
 * 管理端：同事目录
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/coworker")
public class OpcCoworkerController extends BaseController {

    private final IOpcCoworkerService coworkerService;

    @SaCheckPermission("system:coworker:list")
    @GetMapping("/list")
    public TableDataInfo<OpcCoworkerVo> list(OpcCoworkerQuery query) {
        return coworkerService.queryPageList(query);
    }

    @SaCheckPermission(value = {"system:coworker:query", "system:coworker:edit", "system:desktopUser:edit"}, mode = SaMode.OR)
    @GetMapping("/enabledOptions")
    public R<List<OpcCoworkerVo>> enabledOptions() {
        return R.ok(coworkerService.listEnabledOptions());
    }

    @SaCheckPermission(value = {"system:coworker:query", "system:coworker:edit"}, mode = SaMode.OR)
    @GetMapping("/{coworkerId}")
    public R<OpcCoworkerVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long coworkerId) {
        return R.ok(coworkerService.queryById(coworkerId));
    }

    @SaCheckPermission("system:coworker:add")
    @Log(title = "同事", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody OpcCoworkerBo bo) {
        return toAjax(coworkerService.insertByBo(bo));
    }

    @SaCheckPermission("system:coworker:edit")
    @Log(title = "同事", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody OpcCoworkerBo bo) {
        return toAjax(coworkerService.updateByBo(bo));
    }

    @SaCheckPermission("system:coworker:edit")
    @Log(title = "同事", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody OpcCoworkerBo bo) {
        return toAjax(coworkerService.updateStatus(bo.getCoworkerId(), bo.getStatus()));
    }

    @SaCheckPermission("system:coworker:remove")
    @Log(title = "同事", businessType = BusinessType.DELETE)
    @DeleteMapping("/{coworkerIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] coworkerIds) {
        return toAjax(coworkerService.deleteWithValidByIds(List.of(coworkerIds)));
    }
}
