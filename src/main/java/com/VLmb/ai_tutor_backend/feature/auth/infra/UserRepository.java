package com.VLmb.ai_tutor_backend.feature.auth.infra;

import com.VLmb.ai_tutor_backend.feature.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles
            where u.userName = :userName
            """)
    Optional<User> findByUserNameWithRoles(@Param("userName") String userName);

    Optional<User> findByEmail(String email);
}
