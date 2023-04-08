package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.InterfacesServices.IndexingService;
import searchengine.services.InterfacesServices.IndexingPageService;
import searchengine.services.InterfacesServices.SearchService;
import searchengine.services.InterfacesServices.StatisticsService;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final IndexingPageService indexingPageService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, IndexingPageService indexingPageService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.indexingPageService = indexingPageService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String,String>> startIndexing(){
        Map<String, String> response = indexingService.startedIndexing();
        if(response.get("result").equals("false")){
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String,String>> stopIndexing(){
        Map<String, String> response = indexingService.stopIndexing();
        if(response.get("result").equals("false")){
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("indexPage")
    public ResponseEntity<Map<String,String>> indexPage(@RequestParam String url){
        Map<String, String> response = null;
        try {
            response = indexingPageService.indexPage(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(response.get("result").equals("false")){
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(name = "query", defaultValue = "") String query,
                                                 @RequestParam(name = "site", defaultValue = "") String siteUrl,
                                                 @RequestParam(name = "offset", defaultValue = "0") int offset,
                                                 @RequestParam(name = "limit", defaultValue = "20") int limit){
        return ResponseEntity.ok(searchService.search(query, siteUrl, offset, limit));
    }
}
