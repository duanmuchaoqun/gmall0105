package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Reference
    CartService cartService;

    @Override
    public String genTradeCode(String memberId) {
        Jedis jedis = redisUtil.getJedis();
        String tradeKey = null;
        String tradeCode = null;
        try {
            tradeKey = "user:" + memberId + ":tradeCode";
            tradeCode = UUID.randomUUID().toString();
            jedis.setex(tradeKey, 60 * 15, tradeCode);
            return tradeCode;
        } finally {
            jedis.close();
        }
    }

    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = redisUtil.getJedis();
        try {
            String tradeKey = "user:" + memberId + ":tradeCode";
            String tradeCodeFromCache = jedis.get(tradeKey);
            if (StringUtils.isNotBlank(tradeCodeFromCache) && tradeCodeFromCache.equals(tradeCode)) {
                // 校验成功删除交易码
                //jedis.del(tradeKey);
                // 使用lua脚本在发型key的同时将key删除，防止并发订单攻击
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));
                if (eval != null && eval != 0) {
                    return "success";
                } else {
                    return "fail";
                }
            } else {
                return "fail";
            }
        } finally {
            jedis.close();
        }
    }

    @Override
    public void save(OmsOrder omsOrder) {
        omsOrderMapper.insertSelective(omsOrder);
        String id = omsOrder.getId();
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(id);
            omsOrderItemMapper.insertSelective(omsOrderItem);
            //删除购物车数据
//            cartService.delCart();
        }
    }
}
