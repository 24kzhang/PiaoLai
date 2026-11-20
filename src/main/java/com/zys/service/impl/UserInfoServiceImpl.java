package com.zys.service.impl;

import com.zys.entity.UserInfo;
import com.zys.mapper.UserInfoMapper;
import com.zys.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author 张又升
 * @since 2026-11-20
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
