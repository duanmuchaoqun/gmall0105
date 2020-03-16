package com.atguigu.gmall.passport.controller;


import com.alibaba.fastjson.JSON;
import comm.atguigu.gmall.util.HttpclientUtil;

import java.util.HashMap;
import java.util.Map;

public class TestOauth2 {

    public static String getCode(){
        //请求微博授权页面授权
        String s1 = HttpclientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=531285803&response_type=code&redirect_uri=http://passport.gmall.com:8085/vlogin");

        // 获取code授权码
        //a4b06d6dec76d64d45294784a2639af8
//        String s2 = "http://passport.gmall.com:8085/vlogin?code=a4b06d6dec76d64d45294784a2639af8";
        return null;
    }

    public static String getAccessToken(){
        //post请求https://api.weibo.com/oauth2/access_token 获取access_token
        //String s3 = "https://api.weibo.com/oauth2/access_token?client_id=531285803&client_secret=fbbc5ad722e4e5ea2f00e8badb54eea9&grant_type=authorization_code&redirect_uri=http://passport.gmall.com:8085/vlogin&code=CODE";
        String s3 = "https://api.weibo.com/oauth2/access_token";
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("client_id","531285803");
        paramMap.put("client_secret","fbbc5ad722e4e5ea2f00e8badb54eea9");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://passport.gmall.com:8085/vlogin");
        paramMap.put("code","742d3f73d566439eb104fda803094db9");
        String access_token = HttpclientUtil.doPost(s3, paramMap);
        System.out.println(access_token);
        return null;
    }
    public static Map<String,String>  getUserInfo(){
        //用access_token查询用户信息
        String s4 = "https://api.weibo.com/2/users/show.json?access_token=2.001tTDVD0rhNxZ4ca0635685Zb31PE&uid=3207294802";
        String user_json = HttpclientUtil.doGet(s4);
        System.out.println(user_json);
        Map<String,String> map = JSON.parseObject(user_json, Map.class);
        return map;
    }


    public static void main(String[] args) {
        // App_key 531285803
        // 授权回调页地址 http://passport.gmall.com:8085/vlogin





    }

}
