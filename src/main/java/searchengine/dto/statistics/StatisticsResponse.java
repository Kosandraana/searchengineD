package searchengine.dto.statistics;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
