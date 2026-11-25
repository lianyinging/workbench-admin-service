package org.dromara.system.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OpcDesktopUserCoworkerToolBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long coworkerId;

    private String logicalMcp;

    private String toolName;
}
