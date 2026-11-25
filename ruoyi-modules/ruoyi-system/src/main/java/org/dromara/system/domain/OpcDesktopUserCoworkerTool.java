package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 桌面用户 × 同事 × 工具授权
 */
@Data
@TableName("opc_desktop_user_coworker_tool")
public class OpcDesktopUserCoworkerTool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long coworkerId;

    private String logicalMcp;

    private String toolName;
}
