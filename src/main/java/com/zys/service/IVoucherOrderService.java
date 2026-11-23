package com.zys.service;

import com.zys.dto.Result;
import com.zys.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 张又升
 * @since 2026-11-20
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
    Result createVoucherOrder(Long voucherId);
}
