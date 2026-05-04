package com.library.catalog.application.dto;

import com.library.catalog.domain.model.Publisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PublisherDTO tests")
class PublisherDTOTest {

    // ---------------------------------------------------------------
    // PublisherDTO.from(Publisher) - full field mapping
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("PublisherDTO.from(Publisher) full mapping")
    class FullMappingTests {

        @Test
        @DisplayName("should map all fields from fully populated Publisher")
        void shouldMapAllFields() {
            Publisher publisher = Publisher.create(
                "Addison-Wesley",
                "A leading publisher of computer science textbooks",
                "75 Arlington Street, Boston, MA",
                "+1-617-847-1000",
                "info@addisonwesley.com",
                "https://www.addisonwesley.com"
            );

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.id()).isEqualTo(publisher.getId().getValue());
            assertThat(dto.name()).isEqualTo("Addison-Wesley");
            assertThat(dto.description()).isEqualTo("A leading publisher of computer science textbooks");
            assertThat(dto.address()).isEqualTo("75 Arlington Street, Boston, MA");
            assertThat(dto.phone()).isEqualTo("+1-617-847-1000");
            assertThat(dto.email()).isEqualTo("info@addisonwesley.com");
            assertThat(dto.website()).isEqualTo("https://www.addisonwesley.com");
        }

        @Test
        @DisplayName("should map id from PublisherId value")
        void shouldMapPublisherId() {
            Publisher publisher = Publisher.create("Publisher", null, null, null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.id()).isNotNull();
            assertThat(dto.id()).isEqualTo(publisher.getId().getValue());
        }

        @Test
        @DisplayName("should map name correctly")
        void shouldMapName() {
            Publisher publisher = Publisher.create("O'Reilly Media", null, null, null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.name()).isEqualTo("O'Reilly Media");
        }
    }

    // ---------------------------------------------------------------
    // Nullable field handling
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Nullable field handling")
    class NullableFieldTests {

        @Test
        @DisplayName("should map null description")
        void shouldMapNullDescription() {
            Publisher publisher = Publisher.create("Publisher", null, null, null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.description()).isNull();
        }

        @Test
        @DisplayName("should map null address")
        void shouldMapNullAddress() {
            Publisher publisher = Publisher.create("Publisher", "desc", null, null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.address()).isNull();
        }

        @Test
        @DisplayName("should map null phone")
        void shouldMapNullPhone() {
            Publisher publisher = Publisher.create("Publisher", null, "address", null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.phone()).isNull();
        }

        @Test
        @DisplayName("should map null email")
        void shouldMapNullEmail() {
            Publisher publisher = Publisher.create("Publisher", null, null, "phone", null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.email()).isNull();
        }

        @Test
        @DisplayName("should map null website")
        void shouldMapNullWebsite() {
            Publisher publisher = Publisher.create("Publisher", null, null, null, "email@test.com", null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.website()).isNull();
        }

        @Test
        @DisplayName("should map all nullable fields as null for minimal Publisher")
        void shouldMapAllNullableFieldsAsNull() {
            Publisher publisher = Publisher.create("Minimal Publisher", null, null, null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.description()).isNull();
            assertThat(dto.address()).isNull();
            assertThat(dto.phone()).isNull();
            assertThat(dto.email()).isNull();
            assertThat(dto.website()).isNull();
        }

        @Test
        @DisplayName("should map JPA-managed fields as null when not persisted")
        void shouldMapNullVersionAndTimestampsForNewEntity() {
            Publisher publisher = Publisher.create("Publisher", null, null, null, null, null);

            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.version()).isNull();
            assertThat(dto.createdAt()).isNull();
            assertThat(dto.updatedAt()).isNull();
        }
    }

    // ---------------------------------------------------------------
    // Record behavior and snapshot isolation
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("PublisherDTO record behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("should produce equal DTOs from same Publisher")
        void shouldProduceEqualDTOsFromSamePublisher() {
            Publisher publisher = Publisher.create("Publisher", "desc", null, null, null, null);

            PublisherDTO dto1 = PublisherDTO.from(publisher);
            PublisherDTO dto2 = PublisherDTO.from(publisher);

            assertThat(dto1).isEqualTo(dto2);
        }

        @Test
        @DisplayName("should produce different DTOs from different Publishers")
        void shouldProduceDifferentDTOsFromDifferentPublishers() {
            Publisher publisher1 = Publisher.create("Publisher A", null, null, null, null, null);
            Publisher publisher2 = Publisher.create("Publisher B", null, null, null, null, null);

            PublisherDTO dto1 = PublisherDTO.from(publisher1);
            PublisherDTO dto2 = PublisherDTO.from(publisher2);

            assertThat(dto1).isNotEqualTo(dto2);
            assertThat(dto1.name()).isEqualTo("Publisher A");
            assertThat(dto2.name()).isEqualTo("Publisher B");
        }

        @Test
        @DisplayName("should produce independent DTO snapshot")
        void shouldProduceIndependentSnapshot() {
            Publisher publisher = Publisher.create(
                "Original Name", "Original Desc",
                "Original Address", "123-456", "orig@test.com", "https://orig.com"
            );

            PublisherDTO dto = PublisherDTO.from(publisher);

            // Mutate the publisher after DTO creation
            publisher.updateInfo(
                "Updated Name", "Updated Desc",
                "Updated Address", "789-012", "upd@test.com", "https://upd.com"
            );

            // DTO should still hold original values
            assertThat(dto.name()).isEqualTo("Original Name");
            assertThat(dto.description()).isEqualTo("Original Desc");
            assertThat(dto.address()).isEqualTo("Original Address");
            assertThat(dto.phone()).isEqualTo("123-456");
            assertThat(dto.email()).isEqualTo("orig@test.com");
            assertThat(dto.website()).isEqualTo("https://orig.com");
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            Publisher publisher = Publisher.create("Publisher", "desc", null, null, null, null);
            PublisherDTO dto = PublisherDTO.from(publisher);

            assertThat(dto.hashCode()).isEqualTo(dto.hashCode());
        }
    }
}
