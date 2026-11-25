package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 同事可绑 MCP 工具上限
 */
@Data
@TableName("opc_coworker_mcp_tool")
public class OpcCoworkerMcpTool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long coworkerId;

    private String logicalMcp;

    private String toolId;

    private String toolName;
}
