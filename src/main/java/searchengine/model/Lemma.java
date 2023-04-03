package searchengine.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "lemma",  uniqueConstraints = { @UniqueConstraint(name = "lemmaAndSite_id", columnNames = { "lemma", "site_id" } ) })
public class Lemma  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH}, fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    @BatchSize(size = 2)
    private List<Index> indexList;

    public Lemma(){};

    public Lemma(Site site, String lemma, int frequency){
        this.site = site;
        this.lemma = lemma;
        this.frequency = frequency;
    }

}
