package com.liu.dev.sysparam;

import jakarta.persistence.*;

/** 系統參數實體：讓 JPA (ddl-auto: update) 自動建立 sys_param 資料表。 */
@Entity
@Table(name = "`sys_param`")
public class SysParam {

    @Id
    @Column(name = "param_key", length = 100)
    private String paramKey;

    @Column(name = "param_value", length = 500)
    private String paramValue;

    @Column(length = 255)
    private String description;

    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }
    public String getParamValue() { return paramValue; }
    public void setParamValue(String paramValue) { this.paramValue = paramValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
