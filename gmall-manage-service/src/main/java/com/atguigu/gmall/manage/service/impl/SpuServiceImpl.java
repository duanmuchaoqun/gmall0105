package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class SpuServiceImpl implements SpuService {

    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;

    @Autowired
    PmsProductImageMapper pmsProductImageMapper;

    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;

    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;

    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;


    /**
     * 获取spu列表
     * @param catalog3Id
     * @return
     */
    @Override
    public List<PmsProductInfo> supList(String catalog3Id) {
        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        return pmsProductInfoMapper.select(pmsProductInfo);
    }

    /**
     * 获取平台属性
     * @return
     */
    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    /**
     * 保存和修改spu信息
     * @param pmsProductInfo
     */
    @Override
    @Transactional(rollbackFor = {RuntimeException.class, Error.class})
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {
        String id = pmsProductInfo.getId();
        if(StringUtils.isBlank(id)){
            //id为空为新增
            //新增spu数据
            pmsProductInfoMapper.insertSelective(pmsProductInfo);
        } else {
            //id不为空为修改
            Example example = new Example(PmsProductInfo.class);
            example.createCriteria().andEqualTo("id",pmsProductInfo.getId());
            pmsProductInfoMapper.updateByExampleSelective(pmsProductInfo,example);

            //根据spu id删除该spu的图片
            PmsProductImage pmsProductImagedel = new PmsProductImage();
            pmsProductImagedel.setProductId(pmsProductInfo.getId());
            pmsProductImageMapper.delete(pmsProductImagedel);

            //根据spu id删除该spu的销售属性
            PmsProductSaleAttr pmsProductSaleAttrDel = new PmsProductSaleAttr();
            pmsProductSaleAttrDel.setProductId(pmsProductInfo.getId());
            pmsProductSaleAttrMapper.delete(pmsProductSaleAttrDel);

            //根据spu id删除该spu的销售属性值
            PmsProductSaleAttrValue pmsProductSaleAttrValueDel = new PmsProductSaleAttrValue();
            pmsProductSaleAttrValueDel.setProductId(pmsProductInfo.getId());
            pmsProductSaleAttrValueMapper.delete(pmsProductSaleAttrValueDel);
        }
        //新增spu图片数据
        List<PmsProductImage> imageList = pmsProductInfo.getSpuImageList();
        for (PmsProductImage pmsProductImage : imageList) {
            pmsProductImage.setProductId(pmsProductInfo.getId());
            pmsProductImageMapper.insertSelective(pmsProductImage);
        }

        //新增spu销售属性数据
        List<PmsProductSaleAttr> spuSaleAttrList = pmsProductInfo.getSpuSaleAttrList();
        for (PmsProductSaleAttr pmsProductSaleAttr : spuSaleAttrList) {
            pmsProductSaleAttr.setProductId(pmsProductInfo.getId());
            pmsProductSaleAttrMapper.insertSelective(pmsProductSaleAttr);
            // 获取销售属性值
            List<PmsProductSaleAttrValue> spuSaleAttrValueList = pmsProductSaleAttr.getSpuSaleAttrValueList();
            for (PmsProductSaleAttrValue pmsProductSaleAttrValue : spuSaleAttrValueList) {
                pmsProductSaleAttrValue.setProductId(pmsProductInfo.getId());
                pmsProductSaleAttrValueMapper.insertSelective(pmsProductSaleAttrValue);
            }
        }
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        // 根据spuId获取销售属性
        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        List<PmsProductSaleAttr> saleAttrs = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        //获取销售属性值
        for (PmsProductSaleAttr saleAttr : saleAttrs) {
            PmsProductSaleAttrValue saleAttrValue = new PmsProductSaleAttrValue();
            saleAttrValue.setSaleAttrId(saleAttr.getSaleAttrId());
            saleAttrValue.setProductId(spuId);
            saleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValueMapper.select(saleAttrValue));
        }
        return saleAttrs;
    }

    /**
     *
     * @param spuId
     * @return
     */
    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage pmsProductImage = new PmsProductImage();
        pmsProductImage.setProductId(spuId);
        return pmsProductImageMapper.select(pmsProductImage);
    }

    /**
     * 获取前台item销售属性
     * @param productId
     * @return
     */
    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId,String skuId) {
        return pmsProductSaleAttrMapper.selectSpuSaleAttrListCheckBySku(productId,skuId);
    }


}
