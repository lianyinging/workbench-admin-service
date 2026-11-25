package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class OpcDesktopCoworkerPackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String personaKey;

    private String packageSha256;

    private String packageZipBase64;
}
