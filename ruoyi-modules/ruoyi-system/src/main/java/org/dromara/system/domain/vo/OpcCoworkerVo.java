package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.system.domain.OpcCoworker;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class OpcCoworkerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long coworkerId;

    private String personaKey;

    private String name;

    private String tagline;

    private String description;

    private String icon;

    private String groupKey;

    private Long categoryId;

    /** 分类名称（列表展示） */
    private String categoryName;

    private String version;

    private String packageSha256;

    private Long packageSize;

    private Boolean hasPackage;

    private String visibility;

    /** 绑定的 MCP 网关配置 */
    private Long mcpConfigId;

    /** 网关配置名称（列表展示） */
    private String mcpConfigName;

    /** 桌面端：当前用户是否可同步/使用 */
    private Boolean accessible;

    private Integer sortOrder;

    private String status;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private List<OpcCoworkerToolItemVo> tools;

    public static OpcCoworkerVo from(OpcCoworker entity) {
        if (entity == null) {
            return null;
        }
        OpcCoworkerVo vo = new OpcCoworkerVo();
        vo.setCoworkerId(entity.getCoworkerId());
        vo.setPersonaKey(entity.getPersonaKey());
        vo.setName(entity.getName());
        vo.setTagline(entity.getTagline());
        vo.setDescription(entity.getDescription());
        vo.setIcon(entity.getIcon());
        vo.setGroupKey(entity.getGroupKey());
        vo.setCategoryId(entity.getCategoryId());
        vo.setVersion(entity.getVersion());
        vo.setPackageSha256(entity.getPackageSha256());
        vo.setPackageSize(entity.getPackageSize());
        vo.setHasPackage(entity.getPackageZip() != null && !entity.getPackageZip().isBlank());
        vo.setVisibility(entity.getVisibility());
        vo.setMcpConfigId(entity.getMcpConfigId());
        vo.setSortOrder(entity.getSortOrder());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
