package com.library.circulation.domain.repository;

import com.library.circulation.domain.model.Hold;
import com.library.circulation.domain.model.enums.HoldStatus;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.HoldId;
import com.library.shared.domain.model.PatronId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoldRepository extends JpaRepository<Hold, HoldId> {

    List<Hold> findByPatronId(PatronId patronId);

    List<Hold> findByBookId(BookId bookId);

    List<Hold> findByBookIdAndStatus(BookId bookId, HoldStatus status);

    Optional<Hold> findFirstByBookIdAndStatusOrderByQueuePositionAsc(BookId bookId, HoldStatus status);

    boolean existsByBookIdAndPatronIdAndStatusIn(BookId bookId, PatronId patronId, List<HoldStatus> statuses);

    List<Hold> findByStatusAndExpirationDateBefore(HoldStatus status, LocalDateTime date);

    List<Hold> findByStatusAndAvailableUntilDateBefore(HoldStatus status, LocalDateTime date);

    long countByBookIdAndStatus(BookId bookId, HoldStatus status);
}
