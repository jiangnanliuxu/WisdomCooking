package com.ruoyi.cook.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.ai.domain.AiModel;

/**
 * AI 模型配置 Mapper。
 */
public interface AiModelMapper
{
    AiModel selectById(Long id);

    AiModel selectDefaultByType(@Param("modelType") String modelType);

    List<AiModel> selectList(@Param("modelType") String modelType,
            @Param("status") String status, @Param("keyword") String keyword);

    int insertModel(AiModel model);

    int updateModel(AiModel model);

    int clearDefaultByType(@Param("modelType") String modelType);

    int setDefault(@Param("id") Long id);

    int updateTestResult(@Param("id") Long id, @Param("status") String status,
            @Param("latencyMs") Integer latencyMs, @Param("message") String message);
}
