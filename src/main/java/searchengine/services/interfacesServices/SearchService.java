package searchengine.services.interfacesServices;

import searchengine.dto.ApiResponse;
import searchengine.model.SearchFilter;

public interface SearchService {
    ApiResponse search(SearchFilter filter);

}
