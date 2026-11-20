package com.zys.service.impl;

import com.zys.entity.BlogComments;
import com.zys.mapper.BlogCommentsMapper;
import com.zys.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 张又升
 * @since 2026-11-20
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
