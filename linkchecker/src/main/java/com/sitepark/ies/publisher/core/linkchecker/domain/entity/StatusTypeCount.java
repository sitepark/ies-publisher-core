package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

/**
 * Number of links found for a given status type.
 *
 * @param statusType the status type being counted
 * @param count the number of links with this status type
 */
public record StatusTypeCount(StatusType statusType, int count) {}
