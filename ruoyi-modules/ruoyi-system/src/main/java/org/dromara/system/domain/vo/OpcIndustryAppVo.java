package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.system.domain.OpcIndustryApp;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
public class OpcIndustryAppVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;

    private String appKey;

    private String title;

    private String subtitle;

    private String category;

    private String tags;

    private List<String> tagList;

    private String icon;

    /** Logo base64 data URL */
    private String logo;

    private String tone;

    private String mode;

    private String url;

    private String pluginId;

    private String visibility;

    /**
     * 当前桌面用户是否可打开（按 visibility / 登录态 / 后续订阅计算，清单仍全量返回）
     */
    private Boolean accessible;

    private Integer sortOrder;

    private String status;

    private String remark;

    private Date createTime;

    private Date updateTime;

    public static OpcIndustryAppVo from(OpcIndustryApp app) {
        if (app == null) {
            return null;
        }
        OpcIndustryAppVo vo = new OpcIndustryAppVo();
        vo.setAppId(app.getAppId());
        vo.setAppKey(app.getAppKey());
        vo.setTitle(app.getTitle());
        vo.setSubtitle(app.getSubtitle());
        vo.setCategory(app.getCategory());
        vo.setTags(app.getTags());
        vo.setTagList(splitTags(app.getTags()));
        vo.setIcon(app.getIcon());
        vo.setLogo(app.getLogo());
        vo.setTone(app.getTone());
        vo.setMode(app.getMode());
        vo.setUrl(app.getUrl());
        vo.setPluginId(app.getPluginId());
        vo.setVisibility(app.getVisibility());
        vo.setSortOrder(app.getSortOrder());
        vo.setStatus(app.getStatus());
        vo.setRemark(app.getRemark());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());
        return vo;
    }

    private static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split("[,，]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
