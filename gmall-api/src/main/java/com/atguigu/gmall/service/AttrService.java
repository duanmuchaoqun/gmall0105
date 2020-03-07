package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseAttrInfo;

import java.util.List;

public interface AttrService {

    List<PmsBaseAttrInfo> getAttrInfoList(String catalog3Id);

}
