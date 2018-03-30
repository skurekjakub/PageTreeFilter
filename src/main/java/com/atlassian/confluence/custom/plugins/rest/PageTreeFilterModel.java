package com.atlassian.confluence.custom.plugins.rest;

import javax.xml.bind.annotation.*;
@XmlRootElement(name = "message")
@XmlAccessorType(XmlAccessType.FIELD)
public class PageTreeFilterModel {

    @XmlElement(name = "value")
    private String message;

    public PageTreeFilterModel() {
    }

    public PageTreeFilterModel(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}