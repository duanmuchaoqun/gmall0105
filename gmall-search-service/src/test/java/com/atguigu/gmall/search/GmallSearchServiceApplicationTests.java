package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.search.service.sync.ElasticSync;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

	@Reference
	SkuService skuService;

	@Autowired
	JestClient jestClient;

	@Test
	public void contextLoads() throws IOException {
		//jest的dsl的工具
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		//bool

		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
		//filter
		TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId","43");
		boolQueryBuilder.filter(termQueryBuilder);

//		TermQueryBuilder termQueryBuilder1 = new TermQueryBuilder("","");
//		boolQueryBuilder.filter(termQueryBuilder1);
//		TermQueryBuilder termQueryBuilder2 = new TermQueryBuilder("","");
//		boolQueryBuilder.filter(termQueryBuilder2);

//		TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder("","");
//		boolQueryBuilder.filter(termsQueryBuilder);

		// must
		MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName","华为");
		boolQueryBuilder.must(matchQueryBuilder);
		// query
		searchSourceBuilder.query(boolQueryBuilder);

		// from
		searchSourceBuilder.from(0);
		// size
		searchSourceBuilder.size(20);
		//highlight
		searchSourceBuilder.highlight(null);

		String dslStr = searchSourceBuilder.toString();
		System.err.println(dslStr);

		//用api执行复杂查询

		ArrayList<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

		Search search = new Search.Builder(dslStr).addIndex("gmall0105").addType("PmsSkuInfo").build();
		SearchResult result = jestClient.execute(search);
		List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hits = result.getHits(PmsSearchSkuInfo.class);
		for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hits) {
			PmsSearchSkuInfo source = hit.source;
			pmsSearchSkuInfos.add(source);
		}
		System.out.println(pmsSearchSkuInfos);
	}

	@Test
	public void put() throws IOException {

		//查询mysql数据
		List<PmsSkuInfo> pmsSkuInfoList = new ArrayList<>();
		pmsSkuInfoList = skuService.getAllSku();
		// 转化为es的数据结构
		List<PmsSearchSkuInfo> pmsSearchSkuInfos = new ArrayList<>();

		for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
			PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
			BeanUtils.copyProperties(pmsSkuInfo,pmsSearchSkuInfo);
			pmsSearchSkuInfos.add(pmsSearchSkuInfo);
		}

		// 导入es
		for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
			Index put = new Index.Builder(pmsSearchSkuInfo).index("gmall0105").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId()).build();
			jestClient.execute(put);
		}

	}

	//多线程同步测试
	@Test
	public void test(){
		ExecutorService pool = Executors.newFixedThreadPool(10);

		for (int i = 0; i < 100; i++) {
			pool.execute(new ElasticSync(i));
		}
		pool.shutdown();
	}

}
