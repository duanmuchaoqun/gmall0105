#全部查询
GET gmall0105/PmsSkuInfo/_search
{
  "from": 0, 
  "size": 200
}

#过滤查询
GET gmall0105/PmsSkuInfo/_search
{
  "query": {
    "bool": {
      "filter": [
        {
          "term": {
            "skuAttrValueList.valueId": "39"
          }
        },
        {
          "term": {
            "skuAttrValueList.valueId": "43"
          }
        }
      ],
      "must": [
        {
          "match": {
            "skuName": "华为"
          }
          
        }
      ]
    }
  }
}