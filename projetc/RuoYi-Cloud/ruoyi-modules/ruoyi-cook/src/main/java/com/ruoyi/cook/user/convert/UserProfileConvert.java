package com.ruoyi.cook.user.convert;

import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.cook.auth.domain.CookUser;
import com.ruoyi.cook.user.domain.vo.UserProfileVo;

/**
 * 用户资料对象转换器。
 * <p>
 * 通过 MapStruct 在编译期生成字段映射代码，避免在业务代码中反复手写 set/get。
 * </p>
 */
@Mapper(componentModel = "spring")
public interface UserProfileConvert
{
    @Mapping(target = "phone", expression = "java(maskPhone(user.getPhone()))")
    @Mapping(target = "interestTags", expression = "java(parseInterestTags(user.getInterestTagsJson()))")
    UserProfileVo toVo(CookUser user);

    default String maskPhone(String phone)
    {
        if (phone == null || phone.length() < 7)
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    default List<String> parseInterestTags(String interestTagsJson)
    {
        if (interestTagsJson == null || interestTagsJson.isBlank())
        {
            return Collections.emptyList();
        }
        List<String> tags = JSON.parseArray(interestTagsJson, String.class);
        return tags == null ? Collections.emptyList() : tags;
    }
}
