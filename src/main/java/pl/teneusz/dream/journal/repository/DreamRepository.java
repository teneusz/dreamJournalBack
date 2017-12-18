package pl.teneusz.dream.journal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.teneusz.dream.journal.domain.Dream;
import pl.teneusz.dream.journal.service.dto.DiagramDto;

import java.util.List;

/**
 * Spring Data JPA repository for the Dream entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DreamRepository extends JpaRepository<Dream, Long> {

    @Query("select dream from Dream dream where dream.user.login = ?#{principal.username} order by dream.id desc")
    Page<Dream> findByUserIsCurrentUser(Pageable pageable);

    @Query("select dream from Dream dream where dream.visibility = true and dream.user is not null order by dream.id desc")
    Page<Dream> getAllDreams(Pageable pageable);

    @Query("select dream from Dream dream left join fetch dream.tags where dream.id =:id")
    Dream findOneWithEagerRelationships(@Param("id") Long id);

    @Query("select new pl.teneusz.dream.journal.service.dto.DiagramDto(dream.score, count(dream.score)) from Dream dream inner join UserDetails details on details.user.id = dream.user.id where year(details.birthDate) between :down and :up group by dream.score order by dream.score")
    List<DiagramDto> getDiagramScoreByBirthDate(@Param("down") Integer down, @Param("up") Integer up);

}
