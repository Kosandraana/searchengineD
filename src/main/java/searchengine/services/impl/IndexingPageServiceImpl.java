package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Connect;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.services.parsing.LemmaFinder;
import searchengine.services.parsing.UtilParsing;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.InterfacesServices.IndexingPageService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingPageServiceImpl extends UtilParsing implements IndexingPageService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesList;
    private final Connect connect;
    private final static Logger logger = UtilParsing.getLogger();
    private final static Marker INFO_PARSING = UtilParsing.getInfoMarker();
    private final static Marker ERROR_PARSING = UtilParsing.getErrorMarker();

    @Override
    public Map<String, String> indexPage(String url) {
        HashMap<String,String> response = new HashMap<>();

        SiteConfig siteConfig = getSiteConfig(url);
        if (siteConfig == null){
            HashMap<String,String> map = new HashMap<>();
            map.put("result","false");
            map.put("error", "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            return map;
        }
        if (siteConfig.getUrl().equals(url)) {
            HashMap<String,String> map = new HashMap<>();
            reindexingAllPage(url);
            map.put("result","true");
            return map;
        }
        Page page = null;
        if (pageRepository.existsByPath(url.replaceFirst(siteConfig.getUrl(), "/"))){
            page = deleteAllInfoFromPage(url.replaceFirst(siteConfig.getUrl(), "/"));
        }
        reindexingOnePage(url, siteConfig, page);

        response.put("result","true");
        return response;
    }

    public void reindexingAllPage(String path){
       if (siteRepository.findByUrl(path).isPresent()){
           Site siteEntity = siteRepository.findByUrl(path).get();
           siteRepository.delete(siteRepository.getByUrl(path));
           startIndexing(path, siteEntity.getName(), new ForkJoinPool());
       }else {
           logger.info(INFO_PARSING, "--- Такой страницы " + path + " в базе нет ---" + "\n");
       }
    }

    public void reindexingOnePage(String path, SiteConfig siteConfig, Page page) {
        Site site = siteRepository.findByUrl(siteConfig.getUrl()).get();
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        try {
            Document document = getDocument(path);
            page.setSite(site);
            page.setPath(path.replaceFirst(site.getUrl(), "/"));
            page.setCode(document.connection().response().statusCode());
            page.setContent(document.outerHtml());
            pageRepository.save(page);
            saveLemmaAndIndex(page);
        } catch (IOException e) {
            logger.error(ERROR_PARSING, e.getMessage() + "\n");
            setStatusSiteFailed(site, e.toString());
            throw new RuntimeException(e.getMessage());
        }

        logger.info("--- " + Thread.currentThread().getName() + " reindexing one page " + path + " FINISHED ---" + "\n");
        site.setLastError("NULL");
        site.setStatus(Status.INDEXED);
        siteRepository.save(site);
    }

    public void setStatusSiteFailed(Site site, String error) {
        site.setStatus(Status.FAILED);
        site.setLastError(error);
        siteRepository.save(site);
    }

    public SiteConfig getSiteConfig(String url) {
        SiteConfig siteConfig = null;
        for(SiteConfig site : sitesList.getSites()){
           if(url.contains(site.getUrl())) {
               siteConfig = site;
              return siteConfig;
           }
        }
        return siteConfig;
    }

    public Page deleteAllInfoFromPage(String pagePath) {
        LemmaFinder lemmaFinder = getLemmaFinder();
        Page page = pageRepository.findByPath(pagePath).get();
        Map<String,Integer> lemmaInfo = lemmaFinder.collectLemmas(page.getContent());
        for(Map.Entry<String, Integer> entry : lemmaInfo.entrySet()){
            lemmaRepository.decrementAllFrequencyBySiteIdAndLemma(page.getSite().getId(), entry.getKey());
        }
        indexRepository.deleteAllByPageId(page.getId());
        return page;
    }

    public Document getDocument(String path) throws IOException {
        return Jsoup.connect(path)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .userAgent(connect.getUserAgent())
                .referrer(connect.getReferrer())
                .timeout(30000)
                .get();
    }
}
