package com.library.patron.application.service;

import com.library.patron.application.command.*;
import com.library.patron.application.dto.PatronDTO;
import com.library.patron.domain.model.Patron;
import com.library.patron.domain.repository.PatronRepository;
import com.library.patron.domain.service.PatronManagementService;
import com.library.shared.domain.model.PatronId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PatronApplicationService {

    private final PatronManagementService patronManagementService;
    private final PatronRepository patronRepository;

    public PatronApplicationService(PatronManagementService patronManagementService,
                                     PatronRepository patronRepository) {
        this.patronManagementService = patronManagementService;
        this.patronRepository = patronRepository;
    }

    @Transactional
    public PatronDTO registerPatron(RegisterPatronCommand command) {
        Patron patron = patronManagementService.registerPatron(
            command.getFirstName(),
            command.getLastName(),
            command.getEmail(),
            command.getPhone(),
            command.getAddress(),
            command.getCity(),
            command.getPostalCode(),
            command.getPatronType()
        );
        return PatronDTO.fromDomain(patron);
    }

    @Transactional
    public PatronDTO updatePatron(UpdatePatronCommand command) {
        Patron patron = patronManagementService.updatePatronInfo(
            command.getPatronId(),
            command.getFirstName(),
            command.getLastName(),
            command.getEmail(),
            command.getPhone(),
            command.getAddress(),
            command.getCity(),
            command.getPostalCode()
        );
        return PatronDTO.fromDomain(patron);
    }

    @Transactional(readOnly = true)
    public PatronDTO getPatron(PatronId patronId) {
        Patron patron = patronManagementService.getPatron(patronId);
        return PatronDTO.fromDomain(patron);
    }

    @Transactional(readOnly = true)
    public List<PatronDTO> getAllPatrons() {
        return patronManagementService.getAllPatrons().stream()
            .map(PatronDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional
    public void suspendPatron(SuspendPatronCommand command) {
        patronManagementService.suspendPatron(command.getPatronId(), command.getReason());
    }

    @Transactional
    public void reactivatePatron(ReactivatePatronCommand command) {
        patronManagementService.reactivatePatron(command.getPatronId(), command.getReason());
    }

    @Transactional
    public void terminatePatron(TerminatePatronCommand command) {
        patronManagementService.terminatePatron(command.getPatronId(), command.getReason());
    }

    @Transactional
    public void extendMembership(ExtendMembershipCommand command) {
        patronManagementService.extendMembership(command.getPatronId(), command.getMonths(), command.getReason());
    }

    @Transactional
    public void addFine(AddFineCommand command) {
        patronManagementService.addFine(command.getPatronId(), command.getAmount(), command.getReason());
    }

    @Transactional
    public void payFine(PayFineCommand command) {
        patronManagementService.payFine(command.getPatronId(), command.getAmount());
    }

    @Transactional
    public void waiveFine(WaiveFineCommand command) {
        patronManagementService.waiveFine(command.getPatronId(), command.getAmount(), command.getReason());
    }

    @Transactional
    public void changePatronType(ChangePatronTypeCommand command) {
        patronManagementService.changePatronType(command.getPatronId(), command.getNewPatronType());
    }
}
