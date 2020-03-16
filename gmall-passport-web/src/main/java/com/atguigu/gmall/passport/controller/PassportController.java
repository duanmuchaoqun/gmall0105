package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.JwtUtil;
import comm.atguigu.gmall.util.EncryptionUtils;
import comm.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    private static final String AppKey = "531285803";

    private static final String AppSecret = "fbbc5ad722e4e5ea2f00e8badb54eea9";

    private static final String RedirectUri="http://passport.gmall.com:8085/vlogin";

    private static final String WeiBoAuthApi="https://api.weibo.com/oauth2/access_token";

    private static final String UserShowApi="https://api.weibo.com/2/users/show.json";

    @RequestMapping("/vlogin")
    public String vlogin(String code, HttpServletRequest request) {
        String token = "";
        // 根据授权码换取access_token
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("client_id",AppKey);
        paramMap.put("client_secret",AppSecret);
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri",RedirectUri);
        paramMap.put("code",code);
        String accessToken = HttpclientUtil.doPost(WeiBoAuthApi, paramMap);
        if(StringUtils.isBlank(accessToken)){
            return "fail";
        } else {
            Map<String,String> accessTokenMap = JSON.parseObject(accessToken, Map.class);
            // access_token 换取用户信息
            String uid = accessTokenMap.get("uid");
            String access_token = accessTokenMap.get("access_token");
            String userShowUri = UserShowApi+"?access_token="+access_token+"&uid="+uid;
            String userJson = HttpclientUtil.doGet(userShowUri);
            Map<String,String> userInfoMap = JSON.parseObject(userJson, Map.class);
            // 将用户信息保存数据库，用户类型设置为微博用户
            UmsMember umsMember = new UmsMember();
            umsMember.setSourceType("2");
            umsMember.setAccessCode(code);
            umsMember.setAccessToken(access_token);
            umsMember.setSourceUid(userInfoMap.get("idstr"));
            umsMember.setCity(userInfoMap.get("location"));
            String gender = userInfoMap.get("gender");
            umsMember.setGender("0");
            if(gender.equals("m")){
                umsMember.setGender("1");
            }
            if(gender.equals("f")){
                umsMember.setGender("2");
            }
            umsMember.setNickname(userInfoMap.get("screen_name"));

            //校验是否已经存在
            UmsMember umsCheck = new UmsMember();
            umsCheck.setSourceUid(umsMember.getSourceUid());
            UmsMember umsMemberCheck = userService.checkOathUser(umsCheck);
            if(umsMemberCheck==null){
                userService.addOauthUser(umsMember);
            } else {
                umsMember = umsMemberCheck;
            }
            String memberId = umsMember.getId();
            String nickname = umsMember.getNickname();
            // 生成jwt的token，并且重定向到首页，携带该token
            token = makeToken(memberId, nickname, request);
            return "redirect:http://search.gmall.com:8083/index?token="+token;
        }
    }

    @RequestMapping("/verify")
    @ResponseBody
    public String verify(String token, String currentIp) {

        //通过jwt校验真假

        Map<String, String> map = new HashMap<>();


        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", EncryptionUtils.md5(currentIp));
        if (decode != null) {
            map.put("status", "success");
            map.put("memberId", (String) decode.get("memberId"));
            map.put("nickname", (String) decode.get("nickname"));
        } else {
            map.put("status", "fail");
        }
        return JSON.toJSONString(map);
    }


    @RequestMapping("/login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {
        String token = "";

        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null) {
            //登陆成功
            //获取token值
            token = makeToken(umsMember.getId(), umsMember.getNickname(), request);
        } else {
            //登陆失败
            token = "fail";
        }
        return token;
    }

    @RequestMapping("/index")
    public String index(String ReturnUrl, ModelMap map) {
        map.put("ReturnUrl", ReturnUrl);
        return "index";
    }

    /**
     * 制作token
     * @param memberId
     * @param nickname
     * @param request
     * @return
     */
    private String makeToken(String memberId,String nickname,HttpServletRequest request){
        String token = "";
        //使用jwt制作token
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("memberId", memberId);
        userMap.put("nickname", nickname);


        String ip = request.getHeader("x-forwarded-for");//通过nginx转发的客户端
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();//从request中获取ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";
            }
        }

        //需要按照设计的算法对蚕食进行加密后，生成token
        token = JwtUtil.encode("2019gmall0105", userMap, EncryptionUtils.md5(ip));

        //将token存入redis一份
        userService.addUserToken(token, memberId);

        return token;
    }


}
