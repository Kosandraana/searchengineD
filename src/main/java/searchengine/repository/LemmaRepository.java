package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.io.Serializable;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer>, Serializable {
    Optional<Lemma> findFirstByLemma(String lemma);

    Optional<Lemma> findBySiteIdAndLemma(int siteId, String lemma);

    boolean existsBySiteIdAndLemma(int siteId, String lemma);

    @Transactional
    @Modifying
    @Query(value = "UPDATE `lemma` l SET l.frequency = l.frequency - 1 WHERE l.site_id = :siteId AND l.lemma = :lemma", nativeQuery = true)
    void decrementAllFrequencyBySiteIdAndLemma(@Param("siteId") int siteId, @Param("lemma") String lemma);

    @Query(value = "SELECT COUNT(*) FROM `lemma` WHERE `site_id` =:site_id", nativeQuery = true)
    int countLemmasBySiteId(@Param("site_id") int site_id);

    @Query(value = """
                  SELECT l.frequency / count(p.id) as percent
                  FROM `lemma` as l
                  JOIN `page` as p ON l.site_id = p.site_id
                  WHERE l.id = :lemmaId
                  """, nativeQuery = true)
    double percentageLemmaOnPagesById(@Param("lemmaId") int lemmaId);

    @Query(value = """
                  SELECT max(percentage_lemma) FROM
                  (
                  SELECT l.frequency / count(p.id) as percentage_lemma
                  FROM `lemma` as l
                  JOIN `page` as p ON l.site_id = p.site_id
                  WHERE l.site_id = :siteId
                  GROUP BY l.id
                  ) as percentage_lemmas_on_page
                  """, nativeQuery = true)
    double findMaxPercentageLemmaOnPagesBySiteId(@Param("siteId") int siteId);
}
