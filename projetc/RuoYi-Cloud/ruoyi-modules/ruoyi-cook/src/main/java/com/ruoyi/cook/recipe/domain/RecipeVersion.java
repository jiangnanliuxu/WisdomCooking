package com.ruoyi.cook.recipe.domain;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RecipeVersion implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long recipeId;
    private Long authorId;
    private Integer versionNo;
    private String title;
    private Long coverMediaId;
    private String intro;
    private String difficulty;
    private String cookTime;
    private String serving;
    private String ingredientsJson;
    private String stepsJson;
    private String tipsJson;
    private String videoJson;
    private String status;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

}
