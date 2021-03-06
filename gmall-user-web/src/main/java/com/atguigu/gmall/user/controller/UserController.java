package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {

    @Reference
    UserService userService;

    @RequestMapping("/index")
    @ResponseBody
    public String index() {
        return "hello user";
    }

    @RequestMapping("/getAllUser")
    @ResponseBody
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers = userService.getAllUser();
        return umsMembers;
    }

    @ResponseBody
    @RequestMapping("/saveUser")
    public String saveUser(UmsMember umsMember) {
        System.out.println(umsMember);
        Integer id = userService.saveUser(umsMember);
        return "success " + id;
    }

    @ResponseBody
    @RequestMapping("/updateUser")
    public String updateUser(UmsMember umsMember) {
        userService.updateUser(umsMember);
        return "success";
    }

    @ResponseBody
    @RequestMapping("/deleteUser")
    public String deleteUser(Integer id) {
        userService.deleteUserById(id);
        return "success";
    }


    @RequestMapping("/getReceiveAddress")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(Integer memberId) {

        List<UmsMemberReceiveAddress> umsMemberReceiveAddress = userService.getReceiveAddressByMemberId(memberId);
        return umsMemberReceiveAddress;
    }

}
