package com.library.catalog.domain.service;

import com.library.catalog.domain.exception.PublisherNotFoundException;
import com.library.catalog.domain.model.Publisher;
import com.library.catalog.domain.repository.PublisherRepository;
import com.library.shared.domain.model.PublisherId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublisherManagementServiceTest {

    @Mock
    private PublisherRepository publisherRepository;

    @InjectMocks
    private PublisherManagementService service;

    private PublisherId publisherId;

    @BeforeEach
    void setUp() {
        publisherId = PublisherId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    }

    // ------------------------------------------------------------------ //
    //  createPublisher
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("createPublisher")
    class CreatePublisherTests {

        @Test
        @DisplayName("should create publisher and return saved entity")
        void shouldCreatePublisher() {
            when(publisherRepository.save(any(Publisher.class))).thenAnswer(inv -> inv.getArgument(0));

            Publisher result = service.createPublisher("O'Reilly Media", "Technology publisher",
                "1005 Gravenstein Highway", "555-0100", "info@oreilly.com", "https://oreilly.com");

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("O'Reilly Media");
            assertThat(result.getDescription()).isEqualTo("Technology publisher");
            assertThat(result.getAddress()).isEqualTo("1005 Gravenstein Highway");
            assertThat(result.getPhone()).isEqualTo("555-0100");
            assertThat(result.getEmail()).isEqualTo("info@oreilly.com");
            assertThat(result.getWebsite()).isEqualTo("https://oreilly.com");
            verify(publisherRepository).save(any(Publisher.class));
        }

        @Test
        @DisplayName("should create publisher with minimal fields")
        void shouldCreatePublisher_minimalFields() {
            when(publisherRepository.save(any(Publisher.class))).thenAnswer(inv -> inv.getArgument(0));

            Publisher result = service.createPublisher("Minimal Press", null, null, null, null, null);

            assertThat(result.getName()).isEqualTo("Minimal Press");
            assertThat(result.getDescription()).isNull();
            assertThat(result.getAddress()).isNull();
        }
    }

    // ------------------------------------------------------------------ //
    //  updatePublisher
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("updatePublisher")
    class UpdatePublisherTests {

        @Test
        @DisplayName("should update publisher info")
        void shouldUpdatePublisher() {
            Publisher publisher = Publisher.create("Old Name", "Old desc",
                "Old Address", "111", "old@mail.com", "https://old.com");
            when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
            when(publisherRepository.save(any(Publisher.class))).thenAnswer(inv -> inv.getArgument(0));

            Publisher result = service.updatePublisher(publisherId, "New Name", "New desc",
                "New Address", "222", "new@mail.com", "https://new.com");

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New desc");
            assertThat(result.getAddress()).isEqualTo("New Address");
            assertThat(result.getPhone()).isEqualTo("222");
            assertThat(result.getEmail()).isEqualTo("new@mail.com");
            assertThat(result.getWebsite()).isEqualTo("https://new.com");
            verify(publisherRepository).save(publisher);
        }

        @Test
        @DisplayName("should throw PublisherNotFoundException when publisher not found")
        void shouldThrow_whenNotFound() {
            when(publisherRepository.findById(publisherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePublisher(publisherId, "Name", null,
                    null, null, null, null))
                .isInstanceOf(PublisherNotFoundException.class);
        }
    }

    // ------------------------------------------------------------------ //
    //  getPublisher
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getPublisher")
    class GetPublisherTests {

        @Test
        @DisplayName("should return publisher when found")
        void shouldReturnPublisher() {
            Publisher publisher = Publisher.create("O'Reilly", "Tech publisher",
                null, null, null, null);
            when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));

            Publisher result = service.getPublisher(publisherId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("O'Reilly");
        }

        @Test
        @DisplayName("should throw PublisherNotFoundException when not found")
        void shouldThrow_whenNotFound() {
            when(publisherRepository.findById(publisherId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPublisher(publisherId))
                .isInstanceOf(PublisherNotFoundException.class)
                .hasMessageContaining("Publisher not found");
        }
    }

    // ------------------------------------------------------------------ //
    //  searchPublishers
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("searchPublishers")
    class SearchPublishersTests {

        @Test
        @DisplayName("should return paginated search results")
        void shouldReturnPaginatedResults() {
            Publisher p1 = Publisher.create("O'Reilly Media", null, null, null, null, null);
            Publisher p2 = Publisher.create("O'Reilly Germany", null, null, null, null, null);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Publisher> page = new PageImpl<>(List.of(p1, p2));
            when(publisherRepository.findByNameContaining("O'Reilly", pageable)).thenReturn(page);

            Page<Publisher> result = service.searchPublishers("O'Reilly", pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).extracting(Publisher::getName)
                .containsExactly("O'Reilly Media", "O'Reilly Germany");
        }
    }

    // ------------------------------------------------------------------ //
    //  getAllPublishers
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("getAllPublishers")
    class GetAllPublishersTests {

        @Test
        @DisplayName("should return all publishers")
        void shouldReturnAllPublishers() {
            Publisher p1 = Publisher.create("Publisher One", null, null, null, null, null);
            Publisher p2 = Publisher.create("Publisher Two", null, null, null, null, null);
            when(publisherRepository.findAll()).thenReturn(List.of(p1, p2));

            List<Publisher> result = service.getAllPublishers();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no publishers exist")
        void shouldReturnEmptyList() {
            when(publisherRepository.findAll()).thenReturn(Collections.emptyList());

            List<Publisher> result = service.getAllPublishers();

            assertThat(result).isEmpty();
        }
    }

    // ------------------------------------------------------------------ //
    //  deletePublisher
    // ------------------------------------------------------------------ //

    @Nested
    @DisplayName("deletePublisher")
    class DeletePublisherTests {

        @Test
        @DisplayName("should delete publisher when exists")
        void shouldDeletePublisher() {
            when(publisherRepository.existsById(publisherId)).thenReturn(true);

            service.deletePublisher(publisherId);

            verify(publisherRepository).deleteById(publisherId);
        }

        @Test
        @DisplayName("should throw PublisherNotFoundException when publisher not found")
        void shouldThrow_whenNotFound() {
            when(publisherRepository.existsById(publisherId)).thenReturn(false);

            assertThatThrownBy(() -> service.deletePublisher(publisherId))
                .isInstanceOf(PublisherNotFoundException.class)
                .hasMessageContaining("Publisher not found");

            verify(publisherRepository, never()).deleteById(any());
        }
    }
}
