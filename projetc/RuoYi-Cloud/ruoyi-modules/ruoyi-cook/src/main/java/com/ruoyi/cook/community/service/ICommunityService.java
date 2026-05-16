package com.ruoyi.cook.community.service;

import java.util.List;
import com.ruoyi.cook.common.domain.vo.PageVo;
import com.ruoyi.cook.community.domain.dto.PostBlockRequest;
import com.ruoyi.cook.community.domain.dto.PostSaveRequest;
import com.ruoyi.cook.community.domain.vo.PostVo;
import com.ruoyi.cook.community.domain.vo.TopicVo;

/**
 * 社区主链路服务接口。
 */
public interface ICommunityService
{
    PageVo<PostVo> listPublicPosts(String topicCode, String keyword, String recipeCategoryCode, Integer page,
            Integer pageSize);

    PostVo getPostDetail(Long id);

    PostVo createPost(PostSaveRequest request);

    PostVo updatePost(Long id, PostSaveRequest request);

    PostVo submitPost(Long id);

    PostVo withdrawPost(Long id);

    void deletePost(Long id);

    void likePost(Long id);

    void unlikePost(Long id);

    void favoritePost(Long id);

    void unfavoritePost(Long id);

    PageVo<PostVo> listMyPosts(String status, Integer page, Integer pageSize);

    List<TopicVo> listTopics();

    PageVo<PostVo> listAdminPosts(String status, String keyword, Integer page, Integer pageSize);

    PostVo getAdminPostDetail(Long id);

    PostVo approvePost(Long id);

    PostVo blockPost(Long id, PostBlockRequest request);

    PostVo restorePost(Long id);
}
