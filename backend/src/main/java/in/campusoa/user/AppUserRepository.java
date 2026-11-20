package in.campusoa.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByGoogleSub(String googleSub);

    Optional<AppUser> findByEmail(String email);

    /** Admin user search. JPQL (entity names, not table names); LOWER + LIKE for case-insensitive match. */
    @Query("""
            SELECT u FROM AppUser u
            WHERE :q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<AppUser> search(@Param("q") String q, Pageable pageable);
}
