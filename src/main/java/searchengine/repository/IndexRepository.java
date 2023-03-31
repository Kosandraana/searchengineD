package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Long> {

    @Query(value = "SELECT i.page.id, SUM(i.rank) FROM Index i " +
            "WHERE i.page.id IN (:pageIds) AND i.lemma.id IN (:lemmaIds) " +
            "GROUP BY i.page.id")
    List<Object[]> relevanceByLemmas(List<Long> pageIds, List<Long> lemmaIds);

    @Query(value = "SELECT SUM(i.rank) FROM Index i " +
            "WHERE i.page.id IN (:pageIds)")
    int totalRelevance(List<Long> pageIds);

    @Modifying
    void deleteAllByPageId(Long pageId);

    @Modifying
    @Query(value = "DELETE i FROM `index` i " +
            "JOIN site_page p ON i.page_id = p.id " +
            "WHERE p.site_id = :siteId",
        nativeQuery = true)
    void deleteBySiteId(Long siteId);

    default void insertIndexBatch(List<Index> indices) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.batchUpdate(
                "INSERT INTO `index` (lemma_id, page_id, index_rank) " +
                        "VALUES (?, ?, ?) AS new(l, p, r) " +
                        "ON DUPLICATE KEY UPDATE index_rank = index_rank + new.r",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Index indexPage = indices.get(i);
                        int index = 0;
                        ps.setLong(++index, indexPage.getLemma().getId());
                        ps.setLong(++index, indexPage.getPage().getId());
                        ps.setDouble(++index, indexPage.getRank());
                    }
                    @Override
                    public int getBatchSize() {
                        return indices.size();
                    }
                }
        );
    }
}
