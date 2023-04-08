package searchengine.services.InterfacesServices;

import java.io.IOException;
import java.util.Map;

public interface IndexingPageService {
    Map<String,String> indexPage(String url) throws IOException;
}
