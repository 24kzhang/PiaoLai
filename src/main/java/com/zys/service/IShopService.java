package com.zys.service;

import com.zys.dto.Result;
import com.zys.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IShopService extends IService<Shop> {

    Result queryById(Long id);
}
