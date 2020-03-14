package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    SkuService skuService;

    @Reference
    CartService cartService;

    @RequestMapping("/checkCart")
    public String checkCart(Integer isChecked,String skuId,HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){
        String memberId = "1";
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        //调用服务修改状态
        cartService.checkCart(omsCartItem);
        //将新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        for (OmsCartItem cartItem : omsCartItems) {
            BigDecimal quantity = BigDecimal.valueOf(cartItem.getQuantity());
            cartItem.setTotalPrice(cartItem.getPrice().multiply(quantity));
        }
        modelMap.put("cartList",omsCartItems);
        // 被勾选的商品总额
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        return "cartListInner";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal(0);
        for (OmsCartItem cartItem : omsCartItems) {
            if(cartItem.getIsChecked().equals(1)){
                BigDecimal totalPrice = cartItem.getTotalPrice();
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }

    @RequestMapping("/cartList")
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        List<OmsCartItem> omsCartItems = new ArrayList<>();
        String userId = "1";
        if(StringUtils.isNotBlank(userId)){
            //已经登录的查询db
            omsCartItems = cartService.cartList(userId);
        }else{
            // 没有登录的查询cookie
            String carrListCookie = CookieUtil.getCookieValue(request, "carrListCookie", true);
            if(StringUtils.isNotBlank(carrListCookie)){
                omsCartItems = JSON.parseArray(carrListCookie,OmsCartItem.class);
            }
        }
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal quantity = BigDecimal.valueOf(omsCartItem.getQuantity());
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(quantity));
        }

        modelMap.put("cartList",omsCartItems);

        return "cartList";
    }

    @RequestMapping("/addToCart")
    public String addToCart(String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){

        List<OmsCartItem> omsCartItems = new ArrayList<>();
        //调用商品服查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);

        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode("1111111111111");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(quantity);

        // 判断用户是否登录
        String memberId="1";
        if(StringUtils.isBlank(memberId)){
            // 没有登陆cookie
            //cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);

            if(StringUtils.isBlank(cartListCookie)){
                //cookie为空
                omsCartItems.add(omsCartItem);
            }else{
                //cookie不为空
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);

                boolean exist = if_cart_exist(omsCartItems,omsCartItem);
                if(exist){
                    //之前添加过，更新购物车添加数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if(cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity() + omsCartItem.getQuantity());
                            cartItem.setPrice(cartItem.getPrice().add(omsCartItem.getPrice()));
                        }
                    }
                } else {
                    //新增当前购物车
                    omsCartItems.add(omsCartItem);
                }
            }
            //更新cookie
            CookieUtil.setCookie(request,response,"cartListCookie", JSON.toJSONString(omsCartItems),60*60*72,true);

        } else {
            // 已经登录

            OmsCartItem omsCartItemFromDb = cartService.ifCartExistByUser(memberId,skuId);

            if(omsCartItemFromDb == null){
                //该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                cartService.addCart(omsCartItem);
            } else {
                //该用户添加过当前商品
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity() + omsCartItem.getQuantity());
                cartService.updateCart(omsCartItemFromDb);
            }

            //同步缓存
            cartService.flushCartCache(memberId);

        }

        return "redirect:/success.html";

    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {
        boolean b = false;
        for (OmsCartItem cartItem : omsCartItems) {
            String skuId = cartItem.getProductSkuId();
            if(skuId.equals(omsCartItem.getProductSkuId())){
                b = true;
            }
        }

        return b;
    }

}
