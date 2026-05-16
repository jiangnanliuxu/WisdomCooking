package com.ruoyi.cook.recipe.convert;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.ruoyi.cook.recipe.domain.Comment;
import com.ruoyi.cook.recipe.domain.vo.CommentVo;

/**
 * 评论对象转换器。
 * <p>
 * 简单字段由 MapStruct 生成映射实现，业务层只处理状态和权限。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface CommentConvert
{
    // liked 依赖当前登录用户的点赞状态，不能从评论实体直接映射。
    @Mapping(target = "liked", ignore = true)
    CommentVo toVo(Comment comment);

    List<CommentVo> toVoList(List<Comment> comments);
}
