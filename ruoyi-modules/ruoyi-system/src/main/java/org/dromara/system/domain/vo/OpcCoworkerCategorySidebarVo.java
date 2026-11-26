package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class OpcCoworkerCategorySidebarVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long allCount = 0L;

    private Long uncategorizedCount = 0L;

    private List<OpcCoworkerCategoryVo> categories = new ArrayList<>();
}
