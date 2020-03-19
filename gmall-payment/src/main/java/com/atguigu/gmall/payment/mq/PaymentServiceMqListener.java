package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult(MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("out_trade_no");
        Integer count = 0;
        if(mapMessage.getString("count") !=null){
            count = Integer.parseInt(mapMessage.getString("count"));
        }
        //调用paymentService支付宝检查接口
        System.out.println("进行延迟检查，调用支付检查的接口服务");

        Map<String, Object> resultMap = paymentService.checkAlipayPayment(outTradeNo);
//        Map<String, Object> resultMap = null;
        if (resultMap != null && !resultMap.isEmpty()) {
            String tradeStatus = (String) resultMap.get("trade_status");

            // 根据查询的支付状态结果，判断是否进行下一次的延迟人物，还是支付成功更行数据和后续任务
            if (StringUtils.isNotBlank(tradeStatus)&&tradeStatus.equals("TRADE_SUCCESS")) {
                //支付成功，更新支付发送支付队列

                //进行支付更新的幂等性检查操作

                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(outTradeNo);//支付订单号
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no"));//支付宝的交易凭证号
                paymentInfo.setCallbackContent((String) resultMap.get("call_back_content"));//回调请求字符串
                paymentInfo.setCallbackTime(new Date());

                paymentService.updatePayment(paymentInfo);
                System.out.println("已经支付成功，修改支付信息和发送支付成功队列");
                return;
            }
        }

        if (count > 0) {
            //继续发送延迟检查任务，计算延迟时间等
            System.out.println("没有支付成功，检查剩余次数为" + count + "继续发送延迟检查任务");
            count--;
            paymentService.sendDelayPayResultCheckQueue(outTradeNo, count);
        } else {
            System.out.println("检查剩余次数用尽,结束检查");
        }


    }

}
