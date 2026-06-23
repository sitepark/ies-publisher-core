package com.sitepark.ies.publisher.core.channel.port;

/**
 * Contract for authorizing channel-related operations against the current security context.
 *
 * <p>Implementations live outside this library in adapter modules and enforce access rules for the
 * channel use cases.
 */
public interface AccessControl {}
