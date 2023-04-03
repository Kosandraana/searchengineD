package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Component
public class UtilParsing {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private LemmaFinder lemmaFinder = getLemmaFinder();
    private static volatile boolean DoSTOP = false;
    private static final ArrayList<ForkJoinPool> pools = new ArrayList<>();
    private static final Logger logger = LogManager.getRootLogger();
    private static final Marker INFO_PARSING = MarkerManager.getMarker("INFO_PARSING");
    private static final Marker ERROR_PARSING = MarkerManager.getMarker("ERROR_PARSING");

    public void startIndexing(String path, String name, ForkJoinPool forkJoinPool){
        forkJoinPool = new ForkJoinPool();
        pools.add(forkJoinPool);
        searchengine.model.Site newSite = new searchengine.model.Site(SiteStatus.INDEXING, LocalDateTime.now(), "NULL", path, name);
        siteRepository.save(newSite);
        SiteIndexingAction siteIndexingAction = new SiteIndexingAction(path, newSite, pageRepository, siteRepository, UtilParsing.this);
        forkJoinPool.invoke(siteIndexingAction);
        logger.info(INFO_PARSING,"--- Indexing of all pages of the site " + newSite.getUrl() + " is FINISHED ---" + "\n");
        forkJoinPool.shutdown();
        if(!isStopped()){
            setStatusSiteIndexedAndStatusTime(newSite);
        }
        if(isStopped()){
            stopPools();
            setStatusAllSitesFailed();
            doStop(false);
        }
    }

    public synchronized void saveLemmaAndIndex(Page page){
        List<Lemma> lemmas = new ArrayList<>();
        List<Index> indexes = new ArrayList<>();

        int siteId = page.getSite().getId();

        Map<String, Integer> lemmaInfo = lemmaFinder.collectLemmas(page.getContent());
        for (Map.Entry<String, Integer> entry : lemmaInfo.entrySet()) {

            String lemma = entry.getKey();
            float rank = (float) entry.getValue();

            Lemma lemmaEntity = null;
            if (lemmaRepository.existsBySiteIdAndLemma(siteId, lemma)) {
                lemmaEntity = lemmaRepository.findBySiteIdAndLemma(siteId, lemma).get();
                lemmaEntity.setFrequency(Math.incrementExact(lemmaEntity.getFrequency()));
                lemmas.add(lemmaEntity);
            } else {
                lemmaEntity = new Lemma(page.getSite(), lemma, 1);
                lemmas.add(lemmaEntity);
            }
            if (lemmas.size() >= 100) {
                lemmaRepository.saveAll(lemmas);
                lemmas.clear();
            }
            indexes.add(new Index(page, lemmaEntity, rank));
            if (indexes.size() >= 5000) {
                indexRepository.saveAll(indexes);
                indexes.clear();
            }
        }
        lemmaRepository.saveAll(lemmas);
        indexRepository.saveAll(indexes);
    }

    public void setStatusAllSitesFailed(){
        List<searchengine.model.Site> siteList = siteRepository.findAll();
        for (searchengine.model.Site site : siteList) {
            site.setLastError("Индексация остановлена пользователем");
            site.setStatus(SiteStatus.FAILED);
            siteRepository.save(site);
        }
    }

    public void setStatusSiteIndexedAndStatusTime(Site newSite){
        newSite.setStatus(SiteStatus.INDEXED);
        newSite.setStatusTime(LocalDateTime.now());
        siteRepository.save(newSite);
    }

    public synchronized boolean isIndexing(SiteStatus status){
        List<Site> listSites = siteRepository.findByStatus(status);
        return !listSites.isEmpty();
    }

    public static void doStop(boolean flag) {
        DoSTOP = flag;
    }

    public static boolean isStopped(){
        return DoSTOP;
    }

    public void closePool(ForkJoinPool forkJoinPool){
        logger.info(INFO_PARSING, "--- " + Thread.currentThread().getName() + " INTERRUPT ---" + "\n");
        while (!forkJoinPool.isTerminated()){
            forkJoinPool.shutdownNow();
        }
    }

    public void stopPools(){
        for(ForkJoinPool pool : pools){
            closePool(pool);
        }
    }

    public LemmaFinder getLemmaFinder(){
        LemmaFinder lemmaFinder = null;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lemmaFinder;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static Marker getErrorMarker(){
        return ERROR_PARSING;
    }

    public static Marker getInfoMarker(){
        return INFO_PARSING;
    }
}
