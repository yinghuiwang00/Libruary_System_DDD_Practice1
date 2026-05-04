package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.PublisherNotFoundException;
import com.library.catalog.domain.model.Publisher;
import com.library.catalog.domain.repository.PublisherRepository;
import com.library.shared.domain.model.PublisherId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PublisherManagementService {

    private final PublisherRepository publisherRepository;

    public PublisherManagementService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    @Transactional
    public Publisher createPublisher(String name, String description, String address,
                                     String phone, String email, String website) {
        Publisher publisher = Publisher.create(name, description, address, phone, email, website);
        return publisherRepository.save(publisher);
    }

    @Transactional
    public Publisher updatePublisher(PublisherId publisherId, String name, String description,
                                     String address, String phone, String email, String website) {
        Publisher publisher = findPublisherOrThrow(publisherId);
        publisher.updateInfo(name, description, address, phone, email, website);
        return publisherRepository.save(publisher);
    }

    public Publisher getPublisher(PublisherId publisherId) {
        return findPublisherOrThrow(publisherId);
    }

    public Page<Publisher> searchPublishers(String name, Pageable pageable) {
        return publisherRepository.findByNameContaining(name, pageable);
    }

    public List<Publisher> getAllPublishers() {
        return publisherRepository.findAll();
    }

    @Transactional
    public void deletePublisher(PublisherId publisherId) {
        if (!publisherRepository.existsById(publisherId)) {
            throw new PublisherNotFoundException("Publisher not found: " + publisherId.getValue());
        }
        publisherRepository.deleteById(publisherId);
    }

    private Publisher findPublisherOrThrow(PublisherId publisherId) {
        return publisherRepository.findById(publisherId)
            .orElseThrow(() -> new PublisherNotFoundException("Publisher not found: " + publisherId.getValue()));
    }
}
