package searchengine.services.InterfacesServices;

import java.util.Map;

public interface IndexingService {
    Map<String,String> startedIndexing();
    Map<String,String> stopIndexing();
}
