package com.zys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zys.dto.LoginFormDTO;
import com.zys.dto.Result;
import com.zys.entity.User;

import javax.servlet.http.HttpSession;

/**
 * @author 张又升
 * @since 2026-11-20
 */
public interface IUserService extends IService<User> {


    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
