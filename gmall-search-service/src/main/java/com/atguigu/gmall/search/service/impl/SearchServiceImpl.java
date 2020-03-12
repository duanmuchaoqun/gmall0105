package com.atguigu.gmall.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsSearchParam;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.service.SearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    JestClient jestClient;

    private Integer pageSize = 20;

    @Override
    public List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam){

        String dslStr = getSearchDsl(pmsSearchParam);
        //用api执行复杂查询

        //设置返回列表
        ArrayList<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

        Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();

        SearchResult result = null;
        try {
            result = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 获取返回数据
        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = result.getHits(PmsSearchSkuInfo.class);
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
            PmsSearchSkuInfo source = hit.source;
            // 高亮数据替换
            Map<String, List<String>> highlight = hit.highlight;
            if(highlight != null){
                String skuName = highlight.get("skuName").get(0);
                source.setSkuName(skuName);
            }
            pmsSearchSkuInfos.add(source);
        }
        return pmsSearchSkuInfos;
    }

    /**
     * 生成search查询条件
     * @param pmsSearchParam
     * @return
     */
    private String getSearchDsl(PmsSearchParam pmsSearchParam) {
        //获取平台属性
        List<PmsSkuAttrValue> skuAttrValueList = pmsSearchParam.getSkuAttrValueList();
        //获取关键字
        String keyword = pmsSearchParam.getKeyword();
        //获取三级分类
        String catalog3Id = pmsSearchParam.getCatalog3Id();

        //获取页码
        Integer pageNum = pmsSearchParam.getPageNum();


        //jest的dsl的工具
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        //filter 三级分类id设置
        if(StringUtils.isNotBlank(catalog3Id)){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //filter 属性分类条件设置
        if (skuAttrValueList != null) {
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", pmsSkuAttrValue.getValueId());
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // must 关键字查询设置
        if(StringUtils.isNotBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }

        // query
        searchSourceBuilder.query(boolQueryBuilder);

        // from
        if(pageNum == null || pageNum <= 0){
            pageNum = 1;
        }
        searchSourceBuilder.from(pageNum);
        // size
        searchSourceBuilder.size(pageSize);

        //highlight 查询高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        //sort 排序
        searchSourceBuilder.sort("id",SortOrder.DESC);


        return searchSourceBuilder.toString();
    }
}
