package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.service.SearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @RequestMapping("/index")
    public String index(){
        return "index";
    }

    @RequestMapping("/list")
    public String list(PmsSearchParam pmsSearchParam, ModelMap map){ // 三级分类id、关键字、平台属性

        //调用搜索服务，返回搜索结果
        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        map.put("skuLsInfoList",pmsSearchSkuInfos);
        //抽取检索结果所包含的平台集合
        Set<String> valueIdSet = new HashSet<>();

        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                valueIdSet.add(pmsSkuAttrValue.getValueId());
            }
        }
//        map.put();

        return "list";
    }
}
