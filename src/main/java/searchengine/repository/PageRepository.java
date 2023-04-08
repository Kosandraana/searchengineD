package searchengine.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer>, Serializable {
    int countAllByPath(String path);
    Optional<Page> findByPath(String path);
    boolean existsByPath(String path);
    Optional<Page> findByPathAndSiteId(String path, int site_id);
    List<Page> findByPathStartingWith(String path);
    @Query(value = "SELECT COUNT(*) FROM `page` WHERE `site_id` =:site_id", nativeQuery = true)
    int countPagesBySiteId(@Param("site_id") int site_id);

    @Query(value = """
           SELECT * FROM `page` p
           JOIN `index` i ON p.id = i.page_id
           WHERE i.lemma_id = :lemma_id
           """,
            countQuery = "SELECT count(*) FROM `page` p " +
                         "JOIN `index` i ON p.id = i.page_id " +
                         "WHERE i.lemma_id = :lemma_id",
            nativeQuery = true)
    org.springframework.data.domain.Page<Page> findAllByLemmaId(@Param("lemma_id") int lemmaId, Pageable pageable);
}
