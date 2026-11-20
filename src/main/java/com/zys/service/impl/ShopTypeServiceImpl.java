package com.zys.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.zys.dto.Result;
import com.zys.entity.ShopType;
import com.zys.mapper.ShopTypeMapper;
import com.zys.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zys.utils.RedisConstants.SHOP_TYPE_CACHE_KEY;
import static com.zys.utils.RedisConstants.SHOP_TYPE_CACHE_TTL;

/**
 * @author 张又升
 * @since 2026-11-20
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    // 仅在键不存在时回填，避免并发追加重复数据，并同时设置有效期。
    private static final DefaultRedisScript<Long> CACHE_SHOP_TYPES_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 1 then return 0 end\n" +
            "for i = 2, #ARGV do redis.call('rpush', KEYS[1], ARGV[i]) end\n" +
            "return redis.call('expire', KEYS[1], ARGV[1])",
            Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = SHOP_TYPE_CACHE_KEY;
        // 1. 从 Redis 查询商户类型缓存
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(key, 0, -1);
        // 2. 判断缓存是否存在
        if (CollectionUtil.isNotEmpty(shopTypeJsonList)) {
            // 3. 反序列化后返回
            List<ShopType> shopTypes = shopTypeJsonList.stream()
                    .map(json -> JSONUtil.toBean(json, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        // 4. 缓存未命中，按 sort 升序查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 5. 没有商户类型时返回错误
        if (CollectionUtil.isEmpty(shopTypes)) {
            return Result.fail("商户类型不存在");
        }
        // 6. 按查询顺序写入 Redis，并设置有效期
        List<String> args = new ArrayList<>(shopTypes.size() + 1);
        args.add(Long.toString(TimeUnit.MINUTES.toSeconds(SHOP_TYPE_CACHE_TTL)));
        shopTypes.forEach(shopType -> args.add(JSONUtil.toJsonStr(shopType)));
        stringRedisTemplate.execute(CACHE_SHOP_TYPES_SCRIPT, Collections.singletonList(key), args.toArray());
        // 7. 返回数据
        return Result.ok(shopTypes);
    }
}
