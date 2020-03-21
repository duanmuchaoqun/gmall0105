package com.atguigu.gmall.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @RequestMapping("/seckill")
    @ResponseBody
    public String seckill() {
        Jedis jedis = redisUtil.getJedis();
        int stock = Integer.parseInt(jedis.get("114"));
        RSemaphore semaphore = redissonClient.getSemaphore("114");
        boolean b = semaphore.tryAcquire();
        if (b) {
            System.out.println("当前库存剩余数量" + stock + "，某用户抢购成功，当前抢购人数：" + (100 - stock));
        }else{
            System.out.println("当前库存剩余数量" + stock + "，某用户抢购失败");
        }
        return "1";
    }

    @RequestMapping("/kill")
    @ResponseBody
    public String kill() {
        Jedis jedis = redisUtil.getJedis();
        //开启商品监控
        jedis.watch("114");
        int stock = Integer.parseInt(jedis.get("114"));
        if (stock > 0) {
            Transaction multi = jedis.multi();
            multi.incrBy("114", -1);
            List<Object> exec = multi.exec();
            if (exec != null && exec.size() > 0) {
                System.out.println("当前库存剩余数量" + stock + "，某用户抢购成功，当前抢购人数：" + (100 - stock));
            }else{
                System.out.println("当前库存剩余数量" + stock + "，某用户抢购失败");
            }
        }

        jedis.close();
        return "1";
    }
}
