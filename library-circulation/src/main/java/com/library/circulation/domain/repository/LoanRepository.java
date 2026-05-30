package com.library.circulation.domain.repository;

import com.library.circulation.domain.model.Loan;
import com.library.circulation.domain.model.enums.LoanStatus;
import com.library.shared.domain.model.BookId;
import com.library.shared.domain.model.CopyId;
import com.library.shared.domain.model.LoanId;
import com.library.shared.domain.model.PatronId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, LoanId> {

    List<Loan> findByPatronId(PatronId patronId);

    List<Loan> findByPatronIdAndStatus(PatronId patronId, LoanStatus status);

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByCopyId(CopyId copyId);

    List<Loan> findByBookIdAndStatus(BookId bookId, LoanStatus status);

    List<Loan> findByDueDateBeforeAndStatus(LocalDateTime date, LoanStatus status);

    List<Loan> findByDueDateBeforeAndStatusIn(LocalDateTime date, List<LoanStatus> statuses);

    boolean existsByPatronIdAndStatus(PatronId patronId, LoanStatus status);

    boolean existsByCopyIdAndStatus(CopyId copyId, LoanStatus status);

    long countByPatronIdAndStatus(PatronId patronId, LoanStatus status);
}
