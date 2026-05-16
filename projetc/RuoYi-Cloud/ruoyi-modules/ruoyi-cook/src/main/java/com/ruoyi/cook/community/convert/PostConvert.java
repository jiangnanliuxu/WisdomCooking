package com.ruoyi.cook.community.convert;

import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.cook.community.domain.Post;
import com.ruoyi.cook.community.domain.vo.PostVo;

/**
 * 动态对象转换器。
 * <p>
 * MapStruct 负责普通字段映射，JSON 数组字段在 default 方法中集中解析，
 * 让业务层只关注状态流转和权限判断。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface PostConvert
{
    @Mapping(target = "mediaIds", expression = "java(parseLongList(post.getMediaIdsJson()))")
    @Mapping(target = "topicCodes", expression = "java(parseStringList(post.getTopicCodesJson()))")
    @Mapping(target = "liked", ignore = true)
    @Mapping(target = "favorited", ignore = true)
    @Mapping(target = "authorFollowed", ignore = true)
    PostVo toVo(Post post);

    List<PostVo> toVoList(List<Post> posts);

    default List<Long> parseLongList(String json)
    {
        if (json == null || json.isBlank())
        {
            return Collections.emptyList();
        }
        List<Long> values = JSON.parseArray(json, Long.class);
        return values == null ? Collections.emptyList() : values;
    }

    default List<String> parseStringList(String json)
    {
        if (json == null || json.isBlank())
        {
            return Collections.emptyList();
        }
        List<String> values = JSON.parseArray(json, String.class);
        return values == null ? Collections.emptyList() : values;
    }
}
