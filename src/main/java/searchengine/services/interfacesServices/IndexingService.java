package searchengine.services.interfacesServices;

import java.util.Map;

public interface IndexingService {
    Map<String,String> startedIndexing();
    Map<String,String> stopIndexing();
}
