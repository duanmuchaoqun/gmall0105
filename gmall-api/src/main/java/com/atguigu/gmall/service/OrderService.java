package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

import java.math.BigDecimal;

public interface OrderService {
    String genTradeCode(String memberId);

    String checkTradeCode(String memberId,String tradeCode);

    void save(OmsOrder omsOrder);
}