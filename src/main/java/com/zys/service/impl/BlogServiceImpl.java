package com.zys.service.impl;

import com.zys.entity.Blog;
import com.zys.mapper.BlogMapper;
import com.zys.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 张又升
 * @since 2026-11-20
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
