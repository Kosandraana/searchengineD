package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchDto;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.parsing.LemmaFinder;
import searchengine.dto.NormalFormWordAndIndex;
import searchengine.services.parsing.UtilParsing;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.InterfacesServices.SearchService;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl extends UtilParsing implements SearchService {

    private final LemmaFinder lemmaFinder = getLemmaFinder();
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final PageRepository pageRepository;
    private final SitesList sites;
    private final static Logger logger = UtilParsing.getLogger();
    private final static Marker INFO_PARSING = UtilParsing.getInfoMarker();

    @Override
    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query.isEmpty()){
            return new SearchResponse()
                    .setResult(false)
                    .setError("Задан пустой поисковый запрос");
        }
        Pageable pageable = Pageable.ofSize(limit).withPage(offset / limit);
        List<SearchDto> data = new ArrayList<>();

        if(siteUrl.isEmpty()){
            data.addAll(searchAllSites(query, pageable));
        }else {
            data.addAll(searchSite(query, siteUrl, pageable));
        }
        if(data.isEmpty()){
            return new SearchResponse()
                    .setResult(false)
                    .setError("По вашему поисковому запросу ничего не найдено или поисковое слово превышает лимит");
        }

        return new SearchResponse()
                .setResult(true)
                .setCount(data.size())
                .setData(data)
                .setError("");
    }

    public List<SearchDto> searchAllSites(String query, Pageable pageable) {
        List<SiteConfig> siteList = sites.getSites();
        List<SearchDto> data = new ArrayList<>();
        for(SiteConfig site : siteList){
           data.addAll(searchSite(query, site.getUrl(), pageable));
        }
        return data;
    }

    public List<SearchDto> searchSite(String query, String siteUrl, Pageable pageable) {
        Site site = siteRepository.findByUrl(siteUrl).get();
        List<Lemma> filteredLemmas = getFrequencyFilteredLemmas(query, site);
//        if(filteredLemmas.isEmpty()){
//            List<SearchDto> data = new ArrayList<>();
//            return data;
//        }
        List<Page> pages = new ArrayList<>();
        for (Lemma lemma : filteredLemmas) {
            org.springframework.data.domain.Page<Page> pageEntities = pageRepository.findAllByLemmaId(lemma.getId(), pageable);
            if (pages.isEmpty()) {
                pages.addAll(pageEntities.stream().toList());
            }
            pages.retainAll(pageEntities.stream().toList());
        }
        TreeSet<Double> setRelevance = new TreeSet<>();
        for(Page page : pages){
            Double relevance = indexRepository.absoluteRelevanceByPageId(page.getId());
            if(relevance != null){
                setRelevance.add(relevance);
            }
        }
        Double maxRelevance = setRelevance.last();
        List<SearchDto> data = new ArrayList<>();
        for (Page page : pages) {
            String title = getTitle(page.getContent());
            String snippet = getSnippet(page ,filteredLemmas);
            double relativeRelevance = getRelativeRelevance(page.getId(), maxRelevance);
            data.add(new SearchDto()
                    .setSite(site.getUrl())
                    .setSiteName(site.getName())
                    .setUri(page.getPath())
                    .setTitle(title)
                    .setSnippet(snippet)
                    .setRelevance(relativeRelevance));
        }
        Collections.sort(data, Collections.reverseOrder());
        return data;
    }

    public String getSnippet(Page page, List<Lemma> queryLemmas) {
        String pageNormalFormContent = lemmaFinder.clearHTMLTags(page);
        String snippet = "";
        Map<String, NormalFormWordAndIndex> lemmaAndNormalFormWordAndIndex = lemmaFinder.getLemmaAndNormalFormWordAndIndex(pageNormalFormContent);
        for (Lemma lemmaEntity : queryLemmas) {
            for (Map.Entry<String, NormalFormWordAndIndex> entry : lemmaAndNormalFormWordAndIndex.entrySet()){
                int indexWordInPageContent = pageNormalFormContent.indexOf(entry.getValue().getWord());
                if(lemmaAndNormalFormWordAndIndex.containsKey(lemmaEntity.getLemma())
                        && entry.getKey().equals(lemmaEntity.getLemma())
                        && entry.getValue().getIndex() == indexWordInPageContent){
                    String trueForm = "<b>" + entry.getValue().getWord() + "</b>";
                    String trueContent = pageNormalFormContent.replace(entry.getValue().getWord(), trueForm);
                    String[] suggestions = trueContent.split("\\.");
                    for(String str : suggestions){
                        if(str.contains(entry.getValue().getWord())){
                            snippet = str;
                        }
                    }
                }
            }
        }
        return snippet;
    }

    public String getTitle(String content) {
        Document document = Jsoup.parse(content);
        return document.title();
    }

    public double getRelativeRelevance(int pageId, double maxRelevance) {
        double absRelevance = indexRepository.absoluteRelevanceByPageId(pageId);
        return absRelevance / maxRelevance;
    }

    public List<Lemma> getFrequencyFilteredLemmas(String query, Site site) {
        double maxPercentLemmaOnPage = lemmaRepository.findMaxPercentageLemmaOnPagesBySiteId(site.getId());
        double maxFrequencyPercentage = 0.75;
        double frequencyLimit = maxPercentLemmaOnPage * maxFrequencyPercentage;

        Set<String> queryLemmas = lemmaFinder.getLemmaSet(query);
        List<Lemma> lemmaEntityList = new ArrayList<>();
        for(String searchQuery : queryLemmas){
            Lemma lemma = lemmaRepository.findBySiteIdAndLemma(site.getId(), searchQuery).orElse(null);
            if(lemma != null) {
                lemmaEntityList.add(lemma);
            }
        }
        if(lemmaEntityList.isEmpty()){
            logger.info(INFO_PARSING, "--- " + query + " THERE IS NO SUCH LEMMA ---" + "\n");
            return lemmaEntityList;
        }

        List<Lemma> filterFrequency = new ArrayList<>();
        for(Lemma lemma : lemmaEntityList){
            if(lemmaRepository.percentageLemmaOnPagesById(lemma.getId()) < frequencyLimit){
                filterFrequency.add(lemma);
            }else{
                logger.info(INFO_PARSING, "--- " + lemma.getLemma() + " LEMMA IS ABOVE THE LIMIT ---" + "\n");
            }
        }
        return filterFrequency.stream().sorted(Comparator.comparing(Lemma::getFrequency)).toList();
    }
}