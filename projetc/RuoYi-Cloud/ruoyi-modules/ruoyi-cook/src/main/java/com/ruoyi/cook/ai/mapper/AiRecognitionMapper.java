package com.ruoyi.cook.ai.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.cook.ai.domain.AiImageRecognitionLog;

/**
 * AI 识图日志 Mapper。
 */
public interface AiRecognitionMapper
{
    AiImageRecognitionLog selectById(Long id);

    AiImageRecognitionLog selectByIdForUser(@Param("id") Long id, @Param("userId") Long userId);

    List<AiImageRecognitionLog> selectUserList(@Param("userId") Long userId,
            @Param("status") String status);

    List<AiImageRecognitionLog> selectAdminList(@Param("keyword") String keyword,
            @Param("status") String status, @Param("userId") Long userId);

    int insertLog(AiImageRecognitionLog log);

    int softDeleteForUser(@Param("id") Long id, @Param("userId") Long userId);

    int softDeleteById(Long id);
}
