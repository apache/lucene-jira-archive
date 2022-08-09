package com.dianxinos.opdamisc.index;

import com.dianxinos.opdamisc.service.db.NoSqlDBService;
import com.dianxinos.opdamisc.service.db.SqlDBService;
import com.dianxinos.opdamisc.utils.CommonUtils;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: wangweiwei
 * Date: 12/16/11
 * Time: 11:22 AM
 * To change this template use File | Settings | File Templates.
 */
@Singleton
public final class UpdateFastQueryIndex extends FastQueryIndex<List<Map<String, Object>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateFastQueryIndex.class);
    private static final String MATCH_ANY_VALUE = "ANY";
    private static final String DEFAULT_LOCALE = MATCH_ANY_VALUE;
    private static final String DEFAULT_CHANNEL = MATCH_ANY_VALUE;
    private static final String FIELD_ID = "id";
    private static final String FIELD_DB_V = "db_v";
    private static final String FIELD_DT_V = "dt_v";
    private static final String FIELD_URL = "url";
    private static final String FIELD_MD5 = "md5";
    private static final String FIELD_SIZE = "size";

    //    private static final String FIELD_PACKAGE="pkg";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_LOCALES = "locales";
    private static final String FIELD_CHANNELS = "channels";

    private final String indexPath = "index/update";

//    private static final String FIELD_UNIQUE="unique";


    //private Random random = new Random();
   // private RAMDirectory ramDirectory = null;
    private Directory directory = null;
    private IndexWriter indexWriter = null;
    private Analyzer indexAnalyzer = null;
    private IndexConfiguration indexConfiguration;
    private SqlDBService sqlDBService;
    private NoSqlDBService noSqlDBService;
//    private CacheService cacheService;

    @Inject
    private UpdateFastQueryIndex(SqlDBService sqlDBService, @Named("updateIndexConfig") String indexConfig, NoSqlDBService noSqlDBService) {
        super();
        this.sqlDBService = sqlDBService;
        this.noSqlDBService = noSqlDBService;
        this.indexConfiguration = IndexConfiguration.loadIndexConfiguration(indexConfig);
        //ramDirectory = new RAMDirectory();
        final File docDir = new File(indexPath);
        boolean isExit = docDir.exists() ? true : docDir.mkdirs();
        if (!isExit || !docDir.canRead()) {
            String msg = "Document directory '" + docDir.getAbsolutePath() + "' does not exist or is not readable, please check the path";
            LOGGER.warn(msg);
            throw new ExceptionInInitializerError(msg);
        }

        try {
            createIndex();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        //in the last
        rebuildIndex(new Date());
    }

    private void createIndex() throws IOException {

        if (indexAnalyzer != null)
            indexAnalyzer.close();

        if (indexWriter != null){
            LOGGER.warn("the IndexWriter is closed! now  is ready going to recreate ......");
            try {
                indexWriter.close();
            } catch (Exception e) {
                //忽略这条消息
            }
        }

        Directory dir = directory = FSDirectory.open(new File(indexPath));
        Map<String, Analyzer> fieldAnalyzers = new HashMap<String, Analyzer>();
        indexAnalyzer = new PerFieldAnalyzerWrapper(new KeywordAnalyzer(), fieldAnalyzers);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, indexAnalyzer);

        // Create a new index in the directory, removing any
        // previously indexed documents:
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        //
        iwc.setRAMBufferSizeMB(15);
        indexWriter = new IndexWriter(dir, iwc);


    }

    public void destroy() {
        super.destroy();
        try {
            indexWriter.close();
        } catch (IOException e) {
            LOGGER.warn("failed to close index", e);
        }
    }

    @Override
    public void clear() {
        try {
            indexWriter.deleteAll();
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("date", new Date().toString());
            userData.put("action", "drop");
            indexWriter.commit();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("dropped all recommend index data");
            }
        } catch (IOException e) {
            LOGGER.warn("failed to clear index", e);
        }
    }

    @Override
    public List<Map<String, Object>> query(Map<String, String> query) throws IOException {
        List<Map<String, Object>> updates = new ArrayList<Map<String, Object>>();
        String type = query.get(FIELD_TYPE);
        String locale = query.get("locale");
        String channel = query.get("channel");

        BooleanQuery booleanQuery = new BooleanQuery();
        if (null != type) {
            booleanQuery.add(new TermQuery(new Term(FIELD_TYPE, type)), BooleanClause.Occur.MUST);
        }
        if (null != locale) {
            BooleanQuery localesBooleanQuery = new BooleanQuery();
            localesBooleanQuery.add(new TermQuery(new Term(FIELD_LOCALES, DEFAULT_LOCALE)), BooleanClause.Occur.SHOULD);
            TermQuery termQuery = new TermQuery(new Term(FIELD_LOCALES, locale));
            termQuery.setBoost(2.0F);
            localesBooleanQuery.add(termQuery, BooleanClause.Occur.SHOULD);
            booleanQuery.add(localesBooleanQuery, BooleanClause.Occur.MUST);
        }
        if (null != channel) {
            BooleanQuery channelsBooleanQuery = new BooleanQuery();
            channelsBooleanQuery.add(new TermQuery(new Term(FIELD_CHANNELS, DEFAULT_CHANNEL)), BooleanClause.Occur.SHOULD);
            TermQuery termQuery = new TermQuery(new Term(FIELD_CHANNELS, channel));
            termQuery.setBoost(2.0F);
            channelsBooleanQuery.add(termQuery, BooleanClause.Occur.SHOULD);
            booleanQuery.add(channelsBooleanQuery, BooleanClause.Occur.MUST);
        }
        LOGGER.debug("query:{}", booleanQuery);
        QueryWrapperFilter queryWrapperFilter = new QueryWrapperFilter(booleanQuery);
        IndexReader indexReader = DirectoryReader.open(indexWriter, false);

        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        Sort sort = new Sort(SortField.FIELD_SCORE, new SortField(FIELD_TYPE, SortField.Type.STRING));
        TopDocs td = indexSearcher.search(new MatchAllDocsQuery(), queryWrapperFilter, 10, sort);
        int hits = td.totalHits;
        if (hits > 0) {
            Map<String, Map<String, Object>> filterMap = new HashMap<String, Map<String, Object>>();
            for (ScoreDoc scoreDoc : td.scoreDocs) {
                Document document = indexReader.document(scoreDoc.doc);
                Map<String, Object> update = new HashMap<String, Object>();
                //md5,type,db_v,dt_v,url
                String updateType = document.get(FIELD_TYPE);
                int updateDtv = Integer.parseInt(document.get(FIELD_DT_V));
                int updateDbv = Integer.parseInt(document.get(FIELD_DB_V));
                update.put(FIELD_MD5, document.get(FIELD_MD5));
                update.put(FIELD_TYPE, updateType);
                update.put(FIELD_DB_V, updateDbv);
                update.put(FIELD_DT_V, updateDtv);
                update.put(FIELD_URL, document.get(FIELD_URL));

                if(StringUtils.isNotBlank(document.get(FIELD_SIZE)))
                    update.put(FIELD_SIZE, Long.parseLong(document.get(FIELD_SIZE)));
                String key = updateType + updateDtv;
                //keep highest dbv for same type and dtv
                if (!filterMap.containsKey(key)) {
                    filterMap.put(key, update);
                } else {
                    Map<String, Object> pre = filterMap.get(key);
                    int preDbv = (Integer) pre.get(FIELD_DB_V);
                    if (updateDbv > preDbv) {
                        filterMap.put(key, update);
                    }
                }
            }
            updates.addAll(filterMap.values());
        }
        //call close this
        indexReader.close();
        return updates;
    }

    @Override
    public long getIndexSize() {
//        return ramDirectory.sizeInBytes();
        //cause this in file index now .....
        return -1;
    }

    @Override
    public long rebuildIndex(Date time) {
        try {
            List<Map<String, Object>> latestUpdate = sqlDBService.getLatestUpdate();
            indexWriter.deleteAll();
            int hits = latestUpdate.size();
            for (Map<String, Object> update : latestUpdate) {
                Map<String, Object> document = CommonUtils.JACKSON_OBJECT_MAPPER.convertValue(update, new TypeReference<Map<String, Object>>() {
                });
                String id = update.get(FIELD_ID).toString();
                addDocument(id, document, false);
//                cacheService.updateUpdate(Long.parseLong(id));
            }
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("date", time.toString());
            userData.put("action", "rebuild");
            indexWriter.commit();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("rebuild recommendation index complete for {} recommendation units modified after:{}", hits, time);
            }
            return hits;
        }catch (AlreadyClosedException e){
            //LOGGER.error("AlreadyClosedException,will be createIndex ");
            try {
                createIndex();
            } catch (IOException e1) {
                LOGGER.error("failed to  recreate index", e1);
            }
        }catch (Exception e) {
            LOGGER.error("failed to rebuild recommendation index", e);
        }
        return -1;
    }

    @Override
    public void addDocument(String id, Map<String, Object> document, boolean commit) throws IOException {
        //u1.db_v,CONCAT('${cdn.prefix}',u1.url) as url,u1.md5,u1.dt_v,u1.size
        document.put(FIELD_ID, id);
        document.put(FIELD_SIZE, document.get(FIELD_SIZE));
        document.put(FIELD_DB_V, document.get(FIELD_DB_V));
        document.put(FIELD_DT_V, document.get(FIELD_DT_V));
        document.put(FIELD_URL, document.get(FIELD_URL));
        document.put(FIELD_MD5, document.get(FIELD_MD5));
        document.put(FIELD_LOCALES, getLocaleIndexValues(document));
        document.put(FIELD_CHANNELS, getChannelsIndexValues(document));
        Document doc = toDocument(document, indexConfiguration);

        indexWriter.updateDocument(new Term(FIELD_ID, id), doc);
        if (commit) {
            Map<String, String> userData = new HashMap<String, String>();
            userData.put("date", new Date().toString());
            userData.put("action", "rebuild");
            indexWriter.commit();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("add document to index:{}", doc);
        }
    }

    private Collection<String> getLocaleIndexValues(Map<String, Object> document) {
        Collection<String> locales = new HashSet<String>();
        if (null != document.get(FIELD_LOCALES)) {
            String l = (String) document.get(FIELD_LOCALES);
            if (!StringUtils.isEmpty(l)) {
                locales.addAll(Arrays.asList(l.split(",")));
            }
        }
        if (locales.isEmpty()) {
            locales.add(DEFAULT_LOCALE);
        }
        return locales;
    }

    private Collection<String> getChannelsIndexValues(Map<String, Object> document) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("loading index channels for:{}", document);
        }
        Collection<String> channels = new HashSet<String>();
        if (document.containsKey(FIELD_CHANNELS)) {
            String l = (String) document.get(FIELD_CHANNELS);
            if (!StringUtils.isEmpty(l)) {
                List<String> list = Arrays.asList(l.split(","));

                List<String> cidList = new ArrayList<String>(list.size());
                for (String ch : list) {
                    String cid = ch.split(":")[0];
                    cidList.add(cid);
                }

                List<Map<String, Object>> leafChannels = noSqlDBService.getLeafChannels(cidList);
                for (Map<String, Object> channel : leafChannels) {
                    channels.add((String) channel.get("id"));
                }
            }
        }
        if (channels.isEmpty()) {
            channels.add(DEFAULT_CHANNEL);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("finish loading index channels for:{}", document);
        }
        return channels;
    }


    @Override
    public void deleteDocument(String id) throws IOException {
        indexWriter.deleteDocuments(new Term(FIELD_ID, id));
        Map<String, String> userData = new HashMap<String, String>();
        userData.put("date", new Date().toString());
        userData.put("action", "rebuild");
        indexWriter.commit();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("delete document:{}", id);
        }
    }

    @Override
    public long count() {
        try {
            IndexReader indexReader = DirectoryReader.open(indexWriter, true);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
            indexSearcher.search(new MatchAllDocsQuery(), totalHitCountCollector);
            long hits = totalHitCountCollector.getTotalHits();
            indexReader.close();
            return hits;
        } catch (Exception e) {
            LOGGER.error("failed to do index count query", e);
        }
        return 0;
    }

    public long query(String query, int count, List<Map<String, Object>> docs) {
        QueryParser queryParser = new QueryParser(Version.LUCENE_44, FIELD_ID, indexAnalyzer);
        try {
            Query q = query == null ? new MatchAllDocsQuery() : queryParser.parse(query);
            //IndexReader indexReader = getIndexReader();
            IndexReader indexReader = DirectoryReader.open(indexWriter, false);;
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            TopDocs td = indexSearcher.search(q, count);
            long hits = td.totalHits;
            ScoreDoc[] scoreDocs = td.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                Map<String, Object> doc = new HashMap<String, Object>();
                Document document = indexReader.document(scoreDoc.doc);
                doc.put("id", document.get("id"));
                doc.put("locales", Arrays.asList(document.getValues("locales")));
                doc.put("channels", Arrays.asList(document.getValues("channels")));
                doc.put("type", document.get("type"));
                docs.add(doc);
            }
            indexReader.close();
            return hits;
        } catch (Exception e) {
            LOGGER.warn("failed to do index query", e);
        }
        return 0;
    }

    public void deletedOutdatedIndex() {

    }


}
