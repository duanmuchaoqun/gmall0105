package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(Integer memberId);

    Integer saveUser(UmsMember umsMember);

    void updateUser(UmsMember umsMember);

    void deleteUserById(Integer id);
}
