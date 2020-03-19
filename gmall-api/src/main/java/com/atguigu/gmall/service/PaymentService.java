package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService{
    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    void sendDelayPayResultCheckQueue(String outTradeNo,int count);

    Map<String,Object> checkAlipayPayment(String outTradeNo);
}
