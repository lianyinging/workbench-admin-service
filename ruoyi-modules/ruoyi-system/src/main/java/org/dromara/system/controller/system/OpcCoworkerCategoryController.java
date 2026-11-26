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
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.OpcCoworkerCategoryBo;
import org.dromara.system.domain.vo.OpcCoworkerCategorySidebarVo;
import org.dromara.system.domain.vo.OpcCoworkerCategoryVo;
import org.dromara.system.service.IOpcCoworkerCategoryService;
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
 * 管理端：同事分类（一层，挂在同事列表左侧）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/coworkerCategory")
public class OpcCoworkerCategoryController extends BaseController {

    private final IOpcCoworkerCategoryService categoryService;

    @SaCheckPermission(value = {"system:coworkerCategory:query", "system:coworker:list", "system:coworker:query"}, mode = SaMode.OR)
    @GetMapping("/list")
    public R<List<OpcCoworkerCategoryVo>> list() {
        return R.ok(categoryService.queryList());
    }

    @SaCheckPermission(value = {"system:coworkerCategory:query", "system:coworker:list", "system:coworker:query"}, mode = SaMode.OR)
    @GetMapping("/sidebar")
    public R<OpcCoworkerCategorySidebarVo> sidebar() {
        return R.ok(categoryService.querySidebar());
    }

    @SaCheckPermission(value = {"system:coworkerCategory:query", "system:coworker:add", "system:coworker:edit"}, mode = SaMode.OR)
    @GetMapping("/options")
    public R<List<OpcCoworkerCategoryVo>> options() {
        return R.ok(categoryService.queryEnabledOptions());
    }

    @SaCheckPermission("system:coworkerCategory:query")
    @GetMapping("/{categoryId}")
    public R<OpcCoworkerCategoryVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long categoryId) {
        return R.ok(categoryService.queryById(categoryId));
    }

    @SaCheckPermission("system:coworkerCategory:add")
    @Log(title = "同事分类", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody OpcCoworkerCategoryBo bo) {
        return toAjax(categoryService.insertByBo(bo));
    }

    @SaCheckPermission("system:coworkerCategory:edit")
    @Log(title = "同事分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody OpcCoworkerCategoryBo bo) {
        return toAjax(categoryService.updateByBo(bo));
    }

    @SaCheckPermission("system:coworkerCategory:remove")
    @Log(title = "同事分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] categoryIds) {
        return toAjax(categoryService.deleteWithValidByIds(List.of(categoryIds)));
    }
}
