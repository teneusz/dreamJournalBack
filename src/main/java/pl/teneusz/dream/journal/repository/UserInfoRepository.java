package pl.teneusz.dream.journal.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.teneusz.dream.journal.service.dto.UserInfoDto;

@Repository
public interface UserInfoRepository {

    @Query("select new pl.teneusz.dream.journal.service.dto.UserInfoDto(user.id,details, count(dream), count(comment)) " +
        "from User user " +
        "left join UserDetails details on details.user = user " +
        "left join Dream dream on dream.user = user " +
        "left join Comment comment on comment.user = user " +
        "where user.id = :userId " +
        "group by user.id")
    UserInfoDto getUserInfoByUserId(@Param("userId")Long id);
}
