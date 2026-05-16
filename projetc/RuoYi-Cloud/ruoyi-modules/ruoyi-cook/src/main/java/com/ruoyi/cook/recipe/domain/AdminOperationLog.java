package com.ruoyi.cook.recipe.domain;

import lombok.Data;
@Data
public class AdminOperationLog
{
    private Long id;
    private Long adminId;
    private String bizType;
    private Long bizId;
    private String action;
    private String beforeJson;
    private String afterJson;
    private String remark;

}
