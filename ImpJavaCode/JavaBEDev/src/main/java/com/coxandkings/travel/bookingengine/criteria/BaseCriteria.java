package com.coxandkings.travel.bookingengine.criteria;

import java.io.Serializable;

public class BaseCriteria implements Serializable {
    private String[] ids;
    private String[] excludeIds;
    private String createdByUserId;
    private String lastModifiedByUserId;

    public String[] getIds() {
        return ids;
    }

    public void setIds(String... ids) {
        this.ids = ids;
    }

    public String[] getExcludeIds() {
        return excludeIds;
    }

    public void setExcludeIds(String... excludeIds) {
        this.excludeIds = excludeIds;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getLastModifiedByUserId() {
        return lastModifiedByUserId;
    }

    public void setLastModifiedByUserId(String lastModifiedByUserId) {
        this.lastModifiedByUserId = lastModifiedByUserId;
    }
}

