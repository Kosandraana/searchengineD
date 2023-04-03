package searchengine.dto.search;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SearchDto implements Comparable<SearchDto>{
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private Double relevance;

    @Override
    public int compareTo(SearchDto searchDto) {
        return relevance.compareTo(searchDto.getRelevance());
    }
}
