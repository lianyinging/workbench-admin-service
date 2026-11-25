package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OpcDesktopUserCoworkerToolVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long coworkerId;

    private String logicalMcp;

    private String toolName;
}
