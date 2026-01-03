package nl.mallepetrus.jiptv.repository;

import nl.mallepetrus.jiptv.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    
    Optional<Invitation> findByInvitationCode(String invitationCode);
    
    Optional<Invitation> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT i FROM Invitation i WHERE i.used = false AND i.expiresAt > :now")
    List<Invitation> findValidInvitations(LocalDateTime now);
    
    @Query("SELECT i FROM Invitation i WHERE i.expiresAt < :now AND i.used = false")
    List<Invitation> findExpiredInvitations(LocalDateTime now);
}