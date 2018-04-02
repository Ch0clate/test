package com.coxandkings.travel.bookingengine.resource;

import java.io.Serializable;

public class BaseResource implements Serializable {
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}