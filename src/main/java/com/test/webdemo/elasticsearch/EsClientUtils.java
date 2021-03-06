package com.test.webdemo.elasticsearch;

import com.test.webdemo.utils.GetPropertyUtil;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.MatchPhraseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;


/**
 * @author by Dawei on 2018/5/30.
 */
public class EsClientUtils {

    private static final String ES_SERVER_HOST = GetPropertyUtil.getPropertiesInfoString("elasticsearch.properties", "hostAddress", "192.168.111.21");
    private static final Integer ES_SERVER_PORT = GetPropertyUtil.getPropertiesInfoInt("elasticsearch.properties", "esPort", "9300");


    /* 分词器 类型  默认分词器语句分成单个汉字 */
    /* 可智能分词 */
    private static final String ANALYZER_SMARTCN = "smartcn";

    /**
     * 创建连接
     */
    public static TransportClient getESClient() {
        TransportClient transportClient = null;
        try {
            //设置集群信息
            Settings settings = Settings.builder().put("cluster.name","my-application").build();
            //创建Client
            transportClient =
                    new PreBuiltTransportClient(settings).addTransportAddress(new TransportAddress(InetAddress.getByName(ES_SERVER_HOST), ES_SERVER_PORT));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return transportClient;
    }

    /**
     * 关闭连接
     */
    public static void closeClient(TransportClient transportClient) {
        if(transportClient != null) {
            transportClient.close();
        }
    }

    /**
     * 创建索引
     * @param index1 一级索引分组
     * @param index2 二级索引分组
     * @param indexId 索引ID
     */
    public static IndexRequestBuilder createIndex(TransportClient transportClient, String index1, String index2, String indexId) {
        /*
         * 创建索引 参数: 索引一级分组，索引二级分组， [索引ID]
         */
        IndexRequestBuilder indexRequest;
        if(indexId != null) {
            indexRequest = transportClient.prepareIndex(index1, index2, indexId);
        } else {
            indexRequest = transportClient.prepareIndex(index1, index2);
        }
        return indexRequest;
    }

    /**
     * 添加文档
     * @param indexRequest 索引请求
     * @param source 存储的资源内容 进入的Source 需匹配对应的类型
     * @param contentType 资源内容格式类型
     * @return 索引操作的相应结果
     */
    public static IndexResponse addDocument(IndexRequestBuilder indexRequest, String source, XContentType contentType) {
        /* 添加文档 */
        IndexRequestBuilder documentResponse = indexRequest.setSource(source, contentType);
        /* 获取结果信息 */
        return documentResponse.get();
    }

    /**
     * 获取文档
     * @param transportClient Es Client
     * @param index1 一级索引分组
     * @param index2 二级索引分组
     * @param indexId 索引ID
     */
    public static GetResponse getDocument(TransportClient transportClient, String index1, String index2, String indexId) {
       return transportClient.prepareGet(index1, index2, indexId).get();
    }


    /**
     * 删除文档
     * @param transportClient Es Client
     * @param index 索引分组
     * @param type 类型分组
     * @param indexId 索引ID
     */
    public static DeleteResponse deleteDocument(TransportClient transportClient, String index, String type, String indexId) {
        return transportClient.prepareDelete(index, type, indexId).get();
    }

    /**
     * 查询所有
     * @param transportClient 连接客户端
     * @param index 索引组
     * @param type 类型组
     * @return 查询结果
     */
    public SearchResponse searchAll(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        return searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery())
                .execute()
                .actionGet();

    }

    /**
     * 查询过滤
     * @param transportClient 连接客户端
     * @param index 索引组
     * @param type 类型组
     * @return 查询结果
     */
    public SearchResponse searchAllInclude(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        return searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery())
                .setFetchSource(new String[]{"包含的属性列1","包含的属性列2"}, new String[]{"剔除的属性列1", "剔除的属性列2"})
                .execute()
                .actionGet();

    }

    /**
     * 匹配查询
     * @param transportClient 连接客户端
     * @param index 索引组
     * @param type 类型组
     * @return 查询结果
     */
    public SearchResponse searchAllMatch(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        return searchRequestBuilder.setQuery(QueryBuilders.matchQuery("属性列", "匹配值"))
                .setFetchSource(new String[]{"包含的属性列1","包含的属性列2"}, new String[]{"剔除的属性列1", "剔除的属性列2"})
                .execute()
                .actionGet();

    }

    /**
     * 高亮显示
     * @param transportClient 连接客户端
     * @param index 索引组
     * @param type 类型组
     * @return 查询结果
     */
    public SearchResponse searchHighLight(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<strong>");
        highlightBuilder.postTags("</strong");
        return searchRequestBuilder.setQuery(QueryBuilders.matchQuery("属性列", "匹配值"))
                .highlighter(highlightBuilder)
                .setFetchSource(new String[]{"包含的属性列1","包含的属性列2"}, new String[]{"剔除的属性列1", "剔除的属性列2"})
                .execute()
                .actionGet();

    }





    /**
     * 分页查询
     * @param transportClient 连接客户端
     * @param index 索引组
     * @param type 类型组
     * @param pageIndex  页码起始值
     * @param pageSize 每页size
     * @return 查询结果
     */
    public SearchResponse searchByPage(TransportClient transportClient, String index, String type, Integer pageIndex, Integer pageSize) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        return searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery())
                .setFrom(pageIndex)
                .setSize(pageSize)
                .execute()
                .actionGet();

    }


    /**
     * 分页查询 加排序
     * @param transportClient 连接客户端
     * @param index 索引组
     * @param type 类型组
     * @param pageIndex  页码起始值
     * @param pageSize 每页size
     * @return 查询结果
     */
    public SearchResponse searchBySort(TransportClient transportClient, String index, String type, Integer pageIndex, Integer pageSize) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        return searchRequestBuilder.setQuery(QueryBuilders.matchAllQuery())
                .addSort("key1", SortOrder.ASC)
                .addSort("key2", SortOrder.DESC)
                .setFrom(pageIndex)
                .setSize(pageSize)
                .execute()
                .actionGet();

    }




    /* 多条件查寻 */
    public static SearchResponse searchMulit(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        MatchPhraseQueryBuilder matchPhraseQueryBuilder = QueryBuilders.matchPhraseQuery("key1", "value1");
        MatchPhraseQueryBuilder matchPhraseQueryBuilder1 = QueryBuilders.matchPhraseQuery("key2", "value2");
        MatchPhraseQueryBuilder matchPhraseQueryBuilder2 = QueryBuilders.matchPhraseQuery("key3", "value3");
        MatchPhraseQueryBuilder matchPhraseQueryBuilder3 = QueryBuilders.matchPhraseQuery("key4", "value4");

        SearchResponse searchResponse = searchRequestBuilder.setQuery(QueryBuilders.boolQuery()
                .must(matchPhraseQueryBuilder)   //匹配
                .must(matchPhraseQueryBuilder1)   //匹配
                .should(matchPhraseQueryBuilder2) //根据条件提高匹配得分
                .filter(matchPhraseQueryBuilder3)   //过滤
                )
                // .highlighter(HighlightBuilder.fromXContent())
                .setFetchSource(new String[]{"title", "price"}, null)
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        Iterator<SearchHit> iterator = searchHits.iterator();
        while (iterator.hasNext()){
            System.out.println(iterator.next().toString());
        }
        return searchResponse;
    }


    /* 分词查询*/
    public static SearchResponse searchBySmchCn(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        SearchResponse searchResponse = searchRequestBuilder.setQuery(QueryBuilders
                .matchQuery("key", "value")
                .analyzer(ANALYZER_SMARTCN))
                .setFetchSource(new String[]{"title", "price"}, null)
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        Iterator<SearchHit> iterator = searchHits.iterator();
        while (iterator.hasNext()){
            System.out.println(iterator.next().toString());
        }
        return searchResponse;

    }




    /* 多字段 查询*/
    public static SearchResponse searchMulitWord(TransportClient transportClient, String index, String type) {
        SearchRequestBuilder searchRequestBuilder = transportClient.prepareSearch(index).setTypes(type);
        SearchResponse searchResponse = searchRequestBuilder.setQuery(QueryBuilders
                .multiMatchQuery("value", "key1", "key2")
                .analyzer(ANALYZER_SMARTCN))
                .setFetchSource(new String[]{"title", "price"}, null)
                .execute()
                .actionGet();

        SearchHits searchHits = searchResponse.getHits();
        Iterator<SearchHit> iterator = searchHits.iterator();
        while (iterator.hasNext()){
            System.out.println(iterator.next().toString());
        }
        return searchResponse;

    }





















}
