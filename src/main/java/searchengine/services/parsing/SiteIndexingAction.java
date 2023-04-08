package searchengine.services.parsing;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import static java.lang.Thread.sleep;

public class SiteIndexingAction extends RecursiveTask<Set<String>> {

    private final String url;
    private final String rootUrl;
    private final Site site;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final SiteRepository siteRepository;
    private final UtilParsing utilParsing;
    private final static Logger logger = UtilParsing.getLogger();
    private final static Marker INFO_PARSING = UtilParsing.getInfoMarker();
    private final static Marker ERROR_PARSING = UtilParsing.getErrorMarker();
    private static volatile Set<String> allLinks = Collections.synchronizedSet(new HashSet<>());
    private final static  String URL_IS_FILE = "(\\S+(\\.(?i)(jpg|jpeg|JPG|png|gif|bmp|pdf|xml))$)";

    public SiteIndexingAction(String rootUrl, Site site, PageRepository pageRepository, SiteRepository siteRepository, UtilParsing utilParsing){
        this.url = rootUrl;
        this.rootUrl = rootUrl;
        this.site = site;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.utilParsing = utilParsing;
        allLinks.clear();
    }

    public SiteIndexingAction(String url, Site site, String rootUrl, PageRepository pageRepository, SiteRepository siteRepository, UtilParsing utilParsing) {
        this.url = url;
        this.site = site;
        this.rootUrl = rootUrl;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.utilParsing = utilParsing;
    }

    @Override
    protected Set<String> compute() {
        Set<String> links = getLinks(url);
        ArrayList<SiteIndexingAction> taskList = new ArrayList<>();
        for (String link : links) {
            if (stopped()) break;
            SiteIndexingAction task = new SiteIndexingAction(link, site, rootUrl, pageRepository, siteRepository, utilParsing);
            task.fork();
            taskList.add(task);
        }
        for (SiteIndexingAction task : taskList){
            if (stopped()) break;
            links.addAll(task.join());
        }
        return links;
    }

    public Set<String> getLinks(String url){
        Set<String> resultList = new HashSet<>();
        Document document = null;
        Connection.Response response = null;
        Connection connection = getConnection(url);
        try {
            sleep(150);
            try {
                response = connection.execute();
            } catch (Exception e){
                logger.error(ERROR_PARSING, e.getMessage() + "\n");
            }
            if (response != null) {
                document = response.parse();
                Elements elements = document.select("a[href]");
                for (Element element : elements) {
                    String absUrl = element.absUrl("href");
                    if (!isCorrectUrl(absUrl)) continue;
                    logger.info(INFO_PARSING,"--- " + Thread.currentThread().getName() + " got correct abs:href " + absUrl + " ---");
                    if (stopped()) break;
                    savePageAndLemmaAndIndex(site, absUrl.replaceFirst(rootUrl, "/"), response.statusCode(), document.outerHtml());
                    setStatusTimeSite(site);
                    logger.info(INFO_PARSING,"--- add to set " + site.getUrl() + " " + site.getName() + " " + site.getStatus() + " ---");
                    allLinks.add(absUrl);
                    resultList.add(absUrl);
                    if (stopped()) break;
                }
            }
        } catch (Exception e) {
            logger.error(ERROR_PARSING, e.getMessage() + "\n");
        }
        return resultList;
    }

    public boolean isCorrectUrl(String url){
        return !url.isEmpty()
                && !linkIsFile(url)
                && url.startsWith(rootUrl)
                && !allLinks.contains(url)
                && !url.contains("#")
                && !url.contains("?method=")
                && !url.contains("?");
    }

    public synchronized void savePageAndLemmaAndIndex(Site site, String path, int statusCode, String content){
        if(!pageRepository.findByPathAndSiteId(path.replaceFirst(rootUrl, "/"), site.getId()).isPresent()) {
            Page page = new Page(site, path, statusCode, content);
            pageRepository.save(page);
            utilParsing.saveLemmaAndIndex(page);
        }
    }

    public Connection getConnection(String url){
        Connection connection = Jsoup.connect(url)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .timeout(30000);
        return connection;
    }

    public void setStatusTimeSite(Site site){
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    public boolean linkIsFile(String link) {
        return link.matches(URL_IS_FILE);
    }

    public boolean stopped(){
        return UtilParsing.isStopped();
    }

}
