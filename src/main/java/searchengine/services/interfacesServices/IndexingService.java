package searchengine.services.interfacesServices;

public interface IndexingService {
    void startIndexing();
    void stopIndexing();
    void indexPage(String url);
}
