package searchengine.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NormalFormWordAndIndex {
    private String word;
    private int index;

    public NormalFormWordAndIndex(String word, int index) {
        this.word = word;
        this.index = index;
    }
}
