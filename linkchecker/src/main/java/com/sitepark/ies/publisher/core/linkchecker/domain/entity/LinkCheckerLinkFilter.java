package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = LinkCheckerLinkFilter.Builder.class)
public final class LinkCheckerLinkFilter {

  private final List<String> terms;

  private final List<StatusType> statusTypes;

  private LinkCheckerLinkFilter(Builder builder) {
    this.terms = Collections.unmodifiableList(builder.terms);
    this.statusTypes = Collections.unmodifiableList(builder.statusTypes);
  }

  public List<String> getTerms() {
    return this.terms;
  }

  public List<StatusType> getStatusTypes() {
    return this.statusTypes;
  }

  public static Builder builder() {
    return new Builder();
  }

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

  @JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
  public static class Builder {

    private List<String> terms = new ArrayList<>();

    private final List<StatusType> statusTypes = new ArrayList<>();

    private Builder() {}

    private Builder(LinkCheckerLinkFilter instance) {
      this.terms = new ArrayList<>(instance.terms);
      this.statusTypes.addAll(instance.statusTypes);
    }

    @JsonSetter
    public Builder terms(Collection<String> terms) {
      Objects.requireNonNull(terms, "terms is null");
      this.terms.clear();
      for (String term : terms) {
        this.term(term);
      }
      return this;
    }

    public Builder terms(String[] terms) {
      Objects.requireNonNull(terms, "terms is null");
      this.terms.clear();
      for (String term : terms) {
        this.term(term);
      }
      return this;
    }

    public Builder term(String term) {
      Objects.requireNonNull(term, "term is null");
      this.terms.add(term);
      return this;
    }

    public Builder statusTypes(List<StatusType> statusTypes) {
      Objects.requireNonNull(statusTypes, "statusTypes is null");
      this.statusTypes.clear();
      for (StatusType statusType : statusTypes) {
        this.statusType(statusType);
      }
      return this;
    }

    public Builder statusType(StatusType statusType) {
      Objects.requireNonNull(statusType, "statusType is null");
      this.statusTypes.add(statusType);
      return this;
    }

    public LinkCheckerLinkFilter build() {
      return new LinkCheckerLinkFilter(this);
    }
  }
}
