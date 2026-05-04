package com.library.inventory.application.service;

import com.library.inventory.application.command.CreateLibraryCommand;
import com.library.inventory.application.dto.LibraryDTO;
import com.library.inventory.domain.exception.LibraryNotFoundException;
import com.library.inventory.domain.model.Library;
import com.library.inventory.domain.repository.LibraryRepository;
import com.library.shared.domain.model.LibraryId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LibraryApplicationService {

    private final LibraryRepository libraryRepository;

    public LibraryApplicationService(LibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    @Transactional
    public LibraryDTO createLibrary(CreateLibraryCommand command) {
        Library library = Library.create(command.getCode(), command.getName());
        library.updateContactInfo(command.getAddress(), command.getCity(), command.getProvince(),
            command.getPostalCode(), command.getPhone(), command.getEmail());
        if (command.getOpeningHours() != null || command.getTotalFloors() != null) {
            library.updateOperatingInfo(command.getOpeningHours(), command.getTotalFloors());
        }
        return LibraryDTO.fromDomain(libraryRepository.save(library));
    }

    public LibraryDTO getLibrary(String libraryId) {
        Library library = libraryRepository.findById(LibraryId.of(libraryId))
            .orElseThrow(() -> new LibraryNotFoundException(LibraryId.of(libraryId)));
        return LibraryDTO.fromDomain(library);
    }

    public List<LibraryDTO> getAllLibraries() {
        return libraryRepository.findAll().stream()
            .map(LibraryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    public List<LibraryDTO> getActiveLibraries() {
        return libraryRepository.findByActiveTrue().stream()
            .map(LibraryDTO::fromDomain)
            .collect(Collectors.toList());
    }

    @Transactional
    public LibraryDTO updateLibrary(String libraryId, CreateLibraryCommand command) {
        Library library = libraryRepository.findById(LibraryId.of(libraryId))
            .orElseThrow(() -> new LibraryNotFoundException(LibraryId.of(libraryId)));
        library.updateContactInfo(command.getAddress(), command.getCity(), command.getProvince(),
            command.getPostalCode(), command.getPhone(), command.getEmail());
        library.updateOperatingInfo(command.getOpeningHours(), command.getTotalFloors());
        return LibraryDTO.fromDomain(libraryRepository.save(library));
    }

    @Transactional
    public void deactivateLibrary(String libraryId) {
        Library library = libraryRepository.findById(LibraryId.of(libraryId))
            .orElseThrow(() -> new LibraryNotFoundException(LibraryId.of(libraryId)));
        library.deactivate();
        libraryRepository.save(library);
    }

    @Transactional
    public void activateLibrary(String libraryId) {
        Library library = libraryRepository.findById(LibraryId.of(libraryId))
            .orElseThrow(() -> new LibraryNotFoundException(LibraryId.of(libraryId)));
        library.activate();
        libraryRepository.save(library);
    }
}
