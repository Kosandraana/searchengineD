package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.*;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.services.parsing.UtilParsing;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.InterfacesServices.StatisticsService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl extends UtilParsing implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    @Override
    public StatisticsResponse getStatistics() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int pagesCount = pageRepository.findAll().size();
        int lemmasCount = lemmaRepository.findAll().size();

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        if(isIndexing(Status.INDEXING)) {
            total.setIndexing(true);
        }else {
            total.setIndexing(false);
        }

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = siteRepository.findAll();
        for(Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            item.setPages(pageRepository.countPagesBySiteId(site.getId()));
            item.setLemmas(lemmaRepository.countLemmasBySiteId(site.getId()));
            item.setStatus(site.getStatus().toString());
            item.setStatusTime(site.getStatusTime().format(formatter));
            item.setError(site.getLastError());
            total.setPages(pagesCount);
            total.setLemmas(lemmasCount);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
