package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Long> {

    @Query(value = "SELECT l FROM Lemma l " +
            "WHERE l.site.id = :siteId AND l.lemma IN (:lemmas) " +
            "ORDER BY l.frequency")
    List<Lemma> getByLemma(Long siteId, Collection<String> lemmas);
    Optional<Lemma> findBySiteIdAndLemma(Long siteId, String lemma);

    boolean existsBySiteIdAndLemma(Long siteId, String lemma);

    @Query(value = "SELECT l FROM Lemma l WHERE l.lemma IN (:lemmas) " +
            "ORDER BY l.frequency")
    List<Lemma> getByLemma(Collection<String> lemmas);

    @Query(value = "SELECT COUNT(*) FROM lemma WHERE site_id = :siteId",
        nativeQuery = true)
    long countByLemmaBySiteId(Long siteId);

    @Modifying
    @Query(value = "UPDATE lemma l " +
            "JOIN `index` i ON i.lemma_id = l.id " +
            "SET l.frequency = IF(l.frequency > 0, l.frequency - 1, 0) " +
            "WHERE i.page_id = :pageId",
        nativeQuery = true)
    void updateByPage(Long pageId);

    @Modifying
    void deleteAllBySiteId(Long siteId);

    default void insertLemmaBatch(List<Lemma> lemmas) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.batchUpdate("INSERT INTO lemma (site_id, lemma, frequency) " +
                        "VALUES (?, ?, ?) AS new(s, l, f) " +
                        "ON DUPLICATE KEY UPDATE frequency = frequency + new.f",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Lemma lemma = lemmas.get(i);
                        int index = 0;
                        ps.setLong(++index, lemma.getSite().getId());
                        ps.setString(++index, lemma.getLemma());
                        ps.setInt(++index, lemma.getFrequency());
                    }
                    @Override
                    public int getBatchSize() {
                        return lemmas.size();
                    }
                }
        );
    }
}