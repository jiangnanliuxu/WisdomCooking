package com.ruoyi.cook.recipe.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ContentInteraction implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String actionType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
