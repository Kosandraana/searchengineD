package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import java.io.Serializable;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer>, Serializable {

    @Query(value = "SELECT sum(i.rank) FROM `index` i WHERE i.page_id = :pageId", nativeQuery = true)
    Double absoluteRelevanceByPageId(@Param("pageId") int pageId);

    @Transactional
    @Modifying
    void deleteAllByPageId(int pageId);
}