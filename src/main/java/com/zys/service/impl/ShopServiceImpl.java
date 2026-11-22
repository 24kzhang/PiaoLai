package com.zys.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zys.dto.Result;
import com.zys.entity.Shop;
import com.zys.mapper.ShopMapper;
import com.zys.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zys.utils.CacheClient;
import com.zys.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.zys.utils.RedisConstants.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //缓存击穿，互斥锁实现
//        Shop shop = queryWithMutex(id);
        //缓存击穿，逻辑过期实现
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        //7.返回
        return Result.ok(shop);
    }


    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 逻辑过期过期
     */
//    public Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //1. 从Redis 查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2. 判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//            //3.不存在，直接返回null
//            return null;
//        }
//        //4.存在，需要先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        //5.判断是否过期
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            //5.1 未过期，直接返回
//            return shop;
//        }
//        //5.2 过期，重新构建缓存
//        //6.缓存重建
//        //6.1 获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        //6.2 判断是否获取成功
//        if (isLock) {
//            //6.3 成功，开启独立线程重建缓存
//            CACHE_REBUILD_EXECUTOR.submit(() -> {
//                try {
//                    // 重建缓存
//                    this.saveShop2Redis(id, 20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    // 释放锁
//                    unlock(lockKey);
//                }
//            });
//        }
//
//        //6.4 返回过期的商铺信息
//
//        return shop;
//    }
    /**
     * 互斥锁解决缓存击穿
     */
//    public Shop queryWithMutex(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        // 1. 从 Redis 查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2. 判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3.存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //判断命中的是否是空值
//        if (shopJson != null) {
//            // 如果命中的是空值，则返回一个错误信息
//            return null;
//        }
//        //4.实现缓存重建
//        //4.1.获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        Shop shop = null;
//        try {
//            boolean isLock = tryLock(lockKey);
//            //4.2.判断是否获取成功
//            if (!isLock) {
//                //4.3.失败，则休眠并重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//
//            //4.4 成功拿到锁，Double Check, 再次检测Redis缓存是否存在
//            shopJson = stringRedisTemplate.opsForValue().get(key);
//            //4.4.1 如果存在，则直接返回
//            if (StrUtil.isNotBlank(shopJson)) {
//                shop = JSONUtil.toBean(shopJson, Shop.class);
//                return shop;
//            }
//            //4.4.2 如果不存在，则查询数据库
//            shop = getById(id);
//
//            //5.数据库条目不存在，返回错误
//            if (shop == null) {
//                // 写入空数据防止缓存穿透----防止缓存穿透
//                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            //6.存在，写入Redis
//            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            //7. 释放互斥锁
//            unlock(lockKey);
//        }
//        //8.返回数据
//        return shop;
//    }


    /**
     * 带缓存穿透的查询
     */
//    public Shop queryWithPassThrough(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        //1. 从Redis 查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2. 判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3.存在，直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //判断命中的是否是空值
//        if (shopJson != null) {
//            // 如果命中的是空值，则返回一个错误信息
//            return null;
//        }
//        //4.不存在，查询数据库
//        Shop shop = getById(id);
//        //5.不存在，报错
//        if (shop == null) {
//            // 写入空数据防止缓存穿透----防止缓存穿透
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //6.存在，写入Redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        //7.返回数据
//        return shop;
//    }

//    private boolean tryLock(String key) {
//        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 5, TimeUnit.SECONDS);
//        return BooleanUtil.isTrue(flag);
//    }
//    private void unlock(String key) {
//        stringRedisTemplate.delete(key);
//    }
//
//    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
//        //1.查询店铺数据
//        Shop shop = getById(id);
//        Thread.sleep(200);
//        //2.封装逻辑过期时间
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        //3.写入Redis
//        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
//    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
