package com.zys.service;

import com.zys.dto.Result;
import com.zys.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 张又升
 * @since 2026-11-20
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);
}
