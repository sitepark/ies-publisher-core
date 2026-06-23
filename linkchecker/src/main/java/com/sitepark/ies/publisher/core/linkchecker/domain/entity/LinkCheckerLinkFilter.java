package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Filter that restricts checked links by search terms and result status types. */
@JsonDeserialize(builder = LinkCheckerLinkFilter.Builder.class)
public final class LinkCheckerLinkFilter {

  private final List<String> terms;

  private final List<StatusType> statusTypes;

  private LinkCheckerLinkFilter(Builder builder) {
    this.terms = Collections.unmodifiableList(builder.terms);
    this.statusTypes = Collections.unmodifiableList(builder.statusTypes);
  }

  /**
   * Returns the search terms a link's URL must match.
   *
   * @return the search terms
   */
  public List<String> getTerms() {
    return this.terms;
  }

  /**
   * Returns the result status types to filter by.
   *
   * @return the status types
   */
  public List<StatusType> getStatusTypes() {
    return this.statusTypes;
  }

  /**
   * Creates a new builder for {@link LinkCheckerLinkFilter}.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder pre-populated with this instance's values.
   *
   * @return a builder initialized from this instance
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.terms, this.statusTypes);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LinkCheckerLinkFilter that)) {
      return false;
    }
    return Objects.equals(this.terms, that.terms)
        && Objects.equals(this.statusTypes, that.statusTypes);
  }

  @Override
  public String toString() {
    return "LinkCheckerLinkFilter{"
        + "terms='"
        + this.terms
        + '\''
        + ", statusTypes="
        + this.statusTypes
        + '}';
  }

  /** Builder for {@link LinkCheckerLinkFilter}. */
  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {

    private List<String> terms = new ArrayList<>();

    private final List<StatusType> statusTypes = new ArrayList<>();

    private Builder() {}

    private Builder(LinkCheckerLinkFilter instance) {
      this.terms = new ArrayList<>(instance.terms);
      this.statusTypes.addAll(instance.statusTypes);
    }

    /**
     * Replaces the search terms with the given collection.
     *
     * @param terms the search terms to filter by
     * @return this builder for chaining
     */
    @JsonSetter
    public Builder terms(Collection<String> terms) {
      Objects.requireNonNull(terms, "terms is null");
      this.terms.clear();
      for (String term : terms) {
        this.term(term);
      }
      return this;
    }

    /**
     * Replaces the search terms with the given values.
     *
     * @param terms the search terms to filter by
     * @return this builder for chaining
     */
    public Builder terms(String... terms) {
      Objects.requireNonNull(terms, "terms is null");
      this.terms.clear();
      for (String term : terms) {
        this.term(term);
      }
      return this;
    }

    /**
     * Adds a single search term.
     *
     * @param term the search term to add
     * @return this builder for chaining
     */
    public Builder term(String term) {
      Objects.requireNonNull(term, "term is null");
      this.terms.add(term);
      return this;
    }

    /**
     * Replaces the status types with the given list.
     *
     * @param statusTypes the result status types to filter by
     * @return this builder for chaining
     */
    public Builder statusTypes(List<StatusType> statusTypes) {
      Objects.requireNonNull(statusTypes, "statusTypes is null");
      this.statusTypes.clear();
      for (StatusType statusType : statusTypes) {
        this.statusType(statusType);
      }
      return this;
    }

    /**
     * Adds a single result status type.
     *
     * @param statusType the status type to add
     * @return this builder for chaining
     */
    public Builder statusType(StatusType statusType) {
      Objects.requireNonNull(statusType, "statusType is null");
      this.statusTypes.add(statusType);
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerLinkFilter} instance from this builder.
     *
     * @return the built instance
     */
    public LinkCheckerLinkFilter build() {
      return new LinkCheckerLinkFilter(this);
    }
  }
}
