package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.services.parsing.UtilParsing;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.InterfacesServices.IndexingService;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl extends UtilParsing implements IndexingService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private static final int CORE = Runtime.getRuntime().availableProcessors();
    @Override
    public Map<String,String> startedIndexing() {
        List<SiteConfig> listSites = sites.getSites();
        Map<String, String> response = new HashMap<>();
        if(isIndexing(Status.INDEXING)){
            Map<String, String> map = new HashMap<>();
            map.put("result", "false");
            map.put("error", "Индексация уже запущена");
            return map;
        }
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        ExecutorService executorService = Executors.newFixedThreadPool(CORE);
        try {
            for(SiteConfig site : listSites) {
                executorService.submit(
                        new Runnable() {
                    @Override
                    public void run() {
                       startIndexing(site.getUrl(), site.getName(), new ForkJoinPool());
                    }
                });
            }
        } catch (RejectedExecutionException ree) {
            ree.printStackTrace();
        } finally {
            executorService.shutdown();
        }
        response.put("result", "true");
        return response;
    }

    public Map<String,String> stopIndexing(){
        Map<String,String> response = new HashMap<>();
        if(!isIndexing(Status.INDEXING)){
            Map<String, String> map = new HashMap<>();
            map.put("result", "false");
            map.put("error", "Индексация не запущена");
            return map;
        }
        UtilParsing.doStop(true);
        response.put("result", "true");
        return response;
    }
}

