package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentMapper paymentMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;


    @Override
    @Transactional
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentMapper.insertSelective(paymentInfo);
    }

    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class})
    public void updatePayment(PaymentInfo paymentInfo) {

        //幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentMapper.selectOne(paymentInfoParam);
        if(StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus())&&paymentInfoResult.getPaymentStatus().equals("已支付")){
            return;
        }else{
            Connection connection = null;
            Session session = null;
            try {
                connection = activeMQUtil.getConnectionFactory().createConnection();
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
            } catch (JMSException e) {
                e.printStackTrace();
            }
            Example example = new Example(PaymentInfo.class);
            example.createCriteria().andEqualTo("orderSn", paymentInfo.getOrderSn());
            try {
                paymentMapper.updateByExampleSelective(paymentInfo, example);
                // 支付成功后，引起的系统服务->订单服务的更新->库存服务->物流服务
                // 调用mq发送支付成功的消息
                Queue payhment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
                MessageProducer producer = session.createProducer(payhment_success_queue);

                MapMessage mapMessage = new ActiveMQMapMessage();
                mapMessage.setString("out_trade_no", paymentInfo.getOrderSn());

                producer.send(mapMessage);
                session.commit();
            } catch (Exception e) {
                // 消息回滚
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
                throw new RuntimeException();
            } finally {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public void sendDelayPayResultCheckQueue(String outTradeNo,int count) {
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }
        try {
            // 支付成功后，引起的系统服务->订单服务的更新->库存服务->物流服务
            // 调用mq发送支付成功的消息
            Queue payhment_check_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payhment_check_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no", outTradeNo);
            //计数器
            mapMessage.setInt("count", count);

            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*10);

            producer.send(mapMessage);
            session.commit();
        } catch (Exception e) {
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
            throw new RuntimeException();
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public Map<String, Object> checkAlipayPayment(String outTradeNo) {
        Map<String,Object> resultMap = new HashMap<>();
        AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        String param = JSON.toJSONString(map);
        alipayRequest.setBizContent(param);
        AlipayTradeQueryResponse response =null;
        try {
            response = alipayClient.execute(alipayRequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("交易创建,调用成功");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            resultMap.put("call_back_content",response.getMsg());
        } else {
            System.out.println("有可能交易未创建,调用失败");
        }

        return resultMap;
    }
}
