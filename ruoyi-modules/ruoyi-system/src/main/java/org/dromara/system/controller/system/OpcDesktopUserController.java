package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.util.StrUtil;
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
import org.dromara.system.domain.bo.OpcDesktopUserBo;
import org.dromara.system.domain.query.OpcDesktopUserQuery;
import org.dromara.system.domain.vo.OpcDesktopUserVo;
import org.dromara.system.service.IOpcDesktopUserService;
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
import java.util.Map;

/**
 * 管理端：OPC 桌面端登录用户
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/desktopUser")
public class OpcDesktopUserController extends BaseController {

    private final IOpcDesktopUserService desktopUserService;

    @SaCheckPermission("system:desktopUser:list")
    @GetMapping("/list")
    public TableDataInfo<OpcDesktopUserVo> list(OpcDesktopUserQuery query) {
        return desktopUserService.queryPageList(query);
    }

    @SaCheckPermission(value = {"system:desktopUser:query", "system:desktopUser:edit"}, mode = SaMode.OR)
    @GetMapping("/{userId}")
    public R<OpcDesktopUserVo> getInfo(@NotNull(message = "用户ID不能为空") @PathVariable Long userId) {
        return R.ok(desktopUserService.queryById(userId));
    }

    @SaCheckPermission("system:desktopUser:add")
    @Log(title = "客户端用户", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody OpcDesktopUserBo bo) {
        return toAjax(desktopUserService.insertByBo(bo));
    }

    @SaCheckPermission("system:desktopUser:edit")
    @Log(title = "客户端用户", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody OpcDesktopUserBo bo) {
        return toAjax(desktopUserService.updateByBo(bo));
    }

    @SaCheckPermission("system:desktopUser:edit")
    @Log(title = "客户端用户", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody OpcDesktopUserBo bo) {
        return toAjax(desktopUserService.updateStatus(bo.getUserId(), bo.getStatus()));
    }

    @SaCheckPermission("system:desktopUser:resetPwd")
    @Log(title = "客户端用户", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public R<Void> resetPwd(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId")));
        String password = body.get("password") == null ? null : String.valueOf(body.get("password"));
        if (userId == null || StrUtil.isBlank(password)) {
            return R.fail("用户ID和密码不能为空");
        }
        return toAjax(desktopUserService.resetPassword(userId, password));
    }

    @SaCheckPermission("system:desktopUser:remove")
    @Log(title = "客户端用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] userIds) {
        return toAjax(desktopUserService.deleteWithValidByIds(List.of(userIds)));
    }
}
