package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    @Reference
    SpuService spuService;

    /**
     * 根据sku_id获取sku信息接口
     * @param skuId
     * @param map
     * @return
     */
    @RequestMapping("/{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap map, HttpServletRequest request){
        String remoteAddr = request.getRemoteAddr();
        //request.getHeader(""); nginx负载均衡

        // sku基本信息
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        map.put("skuInfo",pmsSkuInfo);

        // sku销售属性
        List<PmsProductSaleAttr> spuSaleAttrList = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),skuId);
        map.put("spuSaleAttrListCheckBySku",spuSaleAttrList);

        // sku获取spu销售属性对应值
        List<PmsSkuInfo>  pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());
        // sku当前spu的集合hash表
        HashMap<String, String> skuSaleAttrHash = new HashMap<>();
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k = "";
            String v = skuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue value : skuSaleAttrValueList) {
                k += value.getSaleAttrValueId()+"|";
            }
            int i = k.lastIndexOf("|");
            skuSaleAttrHash.put(k.substring(0,i),v);
//            skuSaleAttrHash.put(k,v);
        }
        String skuSaleAttrHashStr = JSON.toJSONString(skuSaleAttrHash);

        map.put("skuSaleAttrHashStr",skuSaleAttrHashStr);
        return "item";
    }


    @RequestMapping("/index")
    public String index(){
        return "index";
    }

}
