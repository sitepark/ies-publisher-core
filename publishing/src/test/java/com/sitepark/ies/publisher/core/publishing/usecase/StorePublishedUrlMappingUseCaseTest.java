package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import com.sitepark.ies.publisher.core.publishing.domain.value.OwnerUrlMappings;
import com.sitepark.ies.publisher.core.publishing.port.PublishedUrlMappingRepository;
import com.sitepark.ies.sharedkernel.domain.UrlMappingMode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class StorePublishedUrlMappingUseCaseTest {

  private PublishedUrlMappingRepository repository;

  private StorePublishedUrlMappingUseCase useCase;

  @BeforeEach
  void setUp() {
    this.repository = mock();
    this.useCase = new StorePublishedUrlMappingUseCase(this.repository);
  }

  @Test
  void noExistingMappings_createAll() {
    PublishedUrlMapping newMapping = mapping(null, "/new-path", UrlMappingMode.FORWARD);
    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of());
    when(this.repository.create(List.of(newMapping))).thenReturn("100");

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of(newMapping)));

    verify(this.repository).create(List.of(newMapping));
    verify(this.repository, never()).remove(anyList());
    assertThat(changes.added())
        .as("newly created mapping should be in added with the assigned id")
        .containsExactly(newMapping.toBuilder().id("100").build());
    assertThat(changes.removed()).as("removed should be empty when nothing was deleted").isEmpty();
  }

  @Test
  void emptyIncoming_deleteAll() {
    PublishedUrlMapping existing = mapping("1", "/old-path", UrlMappingMode.FORWARD);
    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of(existing));

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of()));

    verify(this.repository).remove(List.of("1"));
    verify(this.repository, never()).create(anyList());
    assertThat(changes.removed())
        .as("deleted mapping should be in removed")
        .containsExactly(existing);
    assertThat(changes.added()).as("added should be empty when nothing was created").isEmpty();
  }

  @Test
  void identicalMappings_nothingHappens() {
    PublishedUrlMapping mapping = mapping("1", "/path", UrlMappingMode.FORWARD);
    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of(mapping));

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of(mapping)));

    verify(this.repository, never()).create(anyList());
    verify(this.repository, never()).remove(anyList());
    assertThat(changes.isEmpty())
        .as("isEmpty() should return true for identical mappings")
        .isTrue();
  }

  @Test
  void partialChange_deleteRemovedAndCreateNew() {
    PublishedUrlMapping keep = mapping("1", "/keep", UrlMappingMode.FORWARD);
    PublishedUrlMapping toDelete = mapping("2", "/delete", UrlMappingMode.FORWARD);
    PublishedUrlMapping toCreate = mapping(null, "/new", UrlMappingMode.FORWARD);

    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of(keep, toDelete));
    when(this.repository.create(List.of(toCreate))).thenReturn("100");

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of(keep, toCreate)));

    verify(this.repository).remove(List.of("2"));
    verify(this.repository).create(List.of(toCreate));
    assertThat(changes.removed())
        .as("removed mapping should be in removed")
        .containsExactly(toDelete);
    assertThat(changes.added())
        .as("new mapping should be in added with the assigned id")
        .containsExactly(toCreate.toBuilder().id("100").build());
  }

  @Test
  void modeChanged_deleteOldAndCreateNew() {
    PublishedUrlMapping oldMapping = mapping("1", "/path", UrlMappingMode.FORWARD);
    PublishedUrlMapping newMapping = mapping(null, "/path", UrlMappingMode.REDIRECT_PERMANENT);

    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of(oldMapping));
    when(this.repository.create(List.of(newMapping))).thenReturn("100");

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of(newMapping)));

    verify(this.repository).remove(List.of("1"));
    verify(this.repository).create(List.of(newMapping));
    assertThat(changes.removed())
        .as("old mapping should be in removed on mode change")
        .containsExactly(oldMapping);
    assertThat(changes.added())
        .as("new mapping should be in added with the assigned id on mode change")
        .containsExactly(newMapping.toBuilder().id("100").build());
  }

  @Test
  void bothEmpty_nothingHappens() {
    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of());

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of()));

    verify(this.repository, never()).create(anyList());
    verify(this.repository, never()).remove(anyList());
    assertThat(changes.isEmpty())
        .as("isEmpty() should return true when there are no existing and no new mappings")
        .isTrue();
  }

  @Test
  void priorityChanged_deleteOldAndCreateNew() {
    PublishedUrlMapping oldMapping = mapping("1", "/path", UrlMappingMode.FORWARD, 0);
    PublishedUrlMapping newMapping = mapping(null, "/path", UrlMappingMode.FORWARD, 10);

    when(this.repository.getPublishedUrlMappingByOwner("channel-1", "owner-1"))
        .thenReturn(List.of(oldMapping));
    when(this.repository.create(List.of(newMapping))).thenReturn("100");

    StorePublishedUrlMappingResult changes =
        this.useCase.storePublishedUrlMapping(ownerMappings(List.of(newMapping)));

    verify(this.repository).remove(List.of("1"));
    verify(this.repository).create(List.of(newMapping));
    assertThat(changes.removed())
        .as("old mapping should be in removed on priority change")
        .containsExactly(oldMapping);
    assertThat(changes.added())
        .as("new mapping should be in added with the assigned id on priority change")
        .containsExactly(newMapping.toBuilder().id("100").build());
  }

  private static PublishedUrlMapping mapping(String id, String path, UrlMappingMode mode) {
    return mapping(id, path, mode, 0);
  }

  private static PublishedUrlMapping mapping(
      String id, String path, UrlMappingMode mode, int priority) {
    return PublishedUrlMapping.builder()
        .id(id)
        .siteId("site-1")
        .channelId("channel-1")
        .path(path)
        .ownerId("owner-1")
        .mode(mode)
        .priority(priority)
        .build();
  }

  private static OwnerUrlMappings ownerMappings(List<PublishedUrlMapping> mappings) {
    return OwnerUrlMappings.builder()
        .ownerId("owner-1")
        .channelId("channel-1")
        .mappings(mappings)
        .build();
  }
}
