package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The lists of a {@link PublishSchedule}: the ones going on and the ones over, how they are named,
 * and how long the ones over are kept.
 *
 * <p>Not thread-safe: only used under the lock of the schedule. What a list counts - its tasks
 * waiting, running, done - is the business of the schedule; this class only knows when a list
 * begins and when it is over.
 */
// guarded by the lock of the schedule
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.LawOfDemeter"})
final class PublishLists {

  private static final DateTimeFormatter ID_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS").withZone(ZoneOffset.UTC);

  /** The lists with tasks waiting or running, in the order they were requested. */
  private final Map<PublishListId, ListState> open = new LinkedHashMap<>();

  /** The lists that are over, youngest first. */
  private final Deque<ListState> finished = new ArrayDeque<>();

  /** Tells the time lists are requested and over, and when they expire. */
  private final Clock clock;

  /** Makes the ids of lists unique across restarts: the time the lists were created. */
  private final String idPrefix;

  /** How many of the lists that are over are kept, and for how long. */
  private PublishListRetentionSetting retention = PublishListRetentionSetting.DEFAULT;

  private long counter;

  PublishLists(Clock clock) {
    this.clock = clock;
    this.idPrefix = ID_TIME.format(clock.instant());
  }

  /** Sets how many of the lists that are over are kept, and for how long, and applies it. */
  void retain(PublishListRetentionSetting retention) {
    this.retention = retention;
    this.dropExpired();
  }

  /**
   * Registers a new list, going on until it is over and {@link #finishIfOver} is called. It comes after the lists it
   * names that are still going on; a list that is over, or was never known, is nothing to wait
   * for.
   */
  ListState register(PublishListRequest request, boolean foreground) {
    this.counter++;
    PublishListId id =
        new PublishListId(this.idPrefix + "-" + String.format("%012d", this.counter));
    ListState list = new ListState(id, request, foreground, this.clock.instant());
    for (PublishListId predecessor : request.after()) {
      ListState known = this.open.get(predecessor);
      if (known != null) {
        list.after.add(known);
      }
    }
    this.open.put(id, list);
    return list;
  }

  /** The list going on with the id, or {@code null}. */
  @Nullable ListState going(PublishListId id) {
    return this.open.get(id);
  }

  /** The lists going on, in the order they were requested; a copy. */
  List<ListState> going() {
    return new ArrayList<>(this.open.values());
  }

  /** The list with the id, going on or over and not yet forgotten, or {@code null}. */
  @Nullable ListState find(PublishListId id) {
    this.dropExpired();
    ListState active = this.open.get(id);
    if (active != null) {
      return active;
    }
    for (ListState over : this.finished) {
      if (over.id.equals(id)) {
        return over;
      }
    }
    return null;
  }

  /**
   * The lists going on, oldest first, followed by the lists over and not yet forgotten, youngest
   * first.
   */
  List<PublishList> snapshots() {
    this.dropExpired();
    List<PublishList> all = new ArrayList<>(this.open.size() + this.finished.size());
    this.open.values().forEach(list -> all.add(list.snapshot()));
    this.finished.forEach(list -> all.add(list.snapshot()));
    return all;
  }

  /** Moves the list to the ones over if none of its tasks is waiting or running any more. */
  void finishIfOver(ListState list) {
    if (list.isOver() && !list.isFinished()) {
      this.finish(list);
    }
  }

  private void finish(ListState list) {
    list.finish(this.clock.instant());
    this.open.remove(list.id);
    this.finished.addFirst(list);
    this.dropExpired();
  }

  /** Forgets the lists over the number or the age to be kept; the oldest are last. */
  private void dropExpired() {
    int kept = this.retention.keptLists();
    Duration lifetime = this.retention.lifetime();
    Instant oldest = this.clock.instant().minus(lifetime);
    while (!this.finished.isEmpty()) {
      Instant over = this.finished.getLast().finishedAt();
      boolean tooOld = over != null && !over.isAfter(oldest);
      if (this.finished.size() > kept || tooOld) {
        this.finished.removeLast();
      } else {
        break;
      }
    }
  }
}
