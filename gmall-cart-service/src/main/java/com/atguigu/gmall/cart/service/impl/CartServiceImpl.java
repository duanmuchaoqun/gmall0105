package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.atguigu.gmall.cart.service.mapper.OmsCartItemMapper;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {
        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(memberId);
        cartItem.setProductSkuId(skuId);
        OmsCartItem omsCartItem = omsCartItemMapper.selectOne(cartItem);
        return omsCartItem;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        omsCartItemMapper.insertSelective(omsCartItem);
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb,example);
    }

    @Override
    public void flushCartCache(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        HashMap<String, String> map = new HashMap<>();
        for (OmsCartItem cartItem : omsCartItems) {
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }

        //同步redis
        Jedis jedis = redisUtil.getJedis();
        jedis.hmset("user:"+memberId+":cart",map);

        jedis.close();
    }

    @Override
    public List<OmsCartItem> cartList(String userId) {
        Jedis jedis = null;
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try {
            jedis = redisUtil.getJedis();
            String key = "user:"+userId+":cart";
            List<String> hvals = jedis.hvals(key);
            for (String hval : hvals) {
                System.out.println(hval);
                OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }
//            String value = jedis.get(key);
//            omsCartItems = JSON.parseArray(value, OmsCartItem.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }

        return omsCartItems;
    }
}
