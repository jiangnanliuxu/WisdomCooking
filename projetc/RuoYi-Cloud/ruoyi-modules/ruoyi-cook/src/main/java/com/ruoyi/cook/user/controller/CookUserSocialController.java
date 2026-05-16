package com.ruoyi.cook.user.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.annotation.RequiresLogin;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.user.domain.vo.UserInteractionVo;
import com.ruoyi.cook.user.domain.vo.UserPublicProfileVo;
import com.ruoyi.cook.user.service.ICookUserExtraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户社交与公开主页接口。
 */
@Tag(name = "用户端-用户扩展", description = "关注用户、公开主页、达人推荐、我的点赞和我的收藏")
@RestController
@RequestMapping("/api/v1/users")
public class CookUserSocialController
{
    @Autowired
    private ICookUserExtraService userExtraService;

    @Operation(summary = "关注用户")
    @RequiresLogin
    @PostMapping("/{id}/follow")
    public R<?> followUser(@Parameter(description = "被关注用户ID") @PathVariable("id") Long id)
    {
        userExtraService.followUser(id);
        return R.ok();
    }

    @Operation(summary = "取消关注用户")
    @RequiresLogin
    @DeleteMapping("/{id}/follow")
    public R<?> unfollowUser(@Parameter(description = "被取消关注用户ID") @PathVariable("id") Long id)
    {
        userExtraService.unfollowUser(id);
        return R.ok();
    }

    @Operation(summary = "用户公开主页")
    @GetMapping("/{id}/profile")
    public R<UserPublicProfileVo> getProfile(@Parameter(description = "用户ID") @PathVariable("id") Long id)
    {
        return R.ok(userExtraService.getUserProfile(id));
    }

    @Operation(summary = "达人推荐")
    @GetMapping("/recommended")
    public R<List<UserPublicProfileVo>> recommendedUsers(
            @Parameter(description = "返回数量，默认 8") @RequestParam(required = false) Integer limit)
    {
        return R.ok(userExtraService.listRecommendedUsers(limit));
    }

    @Operation(summary = "我的粉丝", description = "分页查询关注当前用户的人")
    @RequiresLogin
    @GetMapping("/me/followers")
    public R<PageVo<UserPublicProfileVo>> listFollowers(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(userExtraService.listMyFollowers(page, pageSize));
    }

    @Operation(summary = "我的关注", description = "分页查询当前用户关注的人")
    @RequiresLogin
    @GetMapping("/me/following")
    public R<PageVo<UserPublicProfileVo>> listFollowing(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(userExtraService.listMyFollowing(page, pageSize));
    }

    @Operation(summary = "我的收藏", description = "统一返回当前用户收藏的菜谱和动态")
    @RequiresLogin
    @GetMapping("/me/favorites")
    public R<PageVo<UserInteractionVo>> listFavorites(
            @Parameter(description = "目标类型：recipe/post，不传则全部") @RequestParam(required = false) String targetType,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(userExtraService.listMyFavorites(targetType, page, pageSize));
    }

    @Operation(summary = "我的点赞", description = "统一返回当前用户点赞的菜谱和动态")
    @RequiresLogin
    @GetMapping("/me/likes")
    public R<PageVo<UserInteractionVo>> listLikes(
            @Parameter(description = "目标类型：recipe/post，不传则全部") @RequestParam(required = false) String targetType,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量") @RequestParam(required = false) Integer pageSize)
    {
        return R.ok(userExtraService.listMyLikes(targetType, page, pageSize));
    }
}
