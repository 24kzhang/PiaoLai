package com.zys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zys.entity.Voucher;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 张又升
 * @since 2026-11-20
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
