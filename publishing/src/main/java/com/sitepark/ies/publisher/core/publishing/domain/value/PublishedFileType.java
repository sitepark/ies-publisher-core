package com.sitepark.ies.publisher.core.publishing.domain.value;

/** What a published file is: the page of an entity, a medium, or a file of the channel itself. */
public enum PublishedFileType {

  /** The file an entity is published as. */
  OBJECT,

  /** The file of a medium entity. */
  MEDIA,

  /** The file of a medium embedded in an entity's page. */
  EMBEDDED_MEDIA,

  /** The redirect rules of the channel. */
  REDIRECT_RULES,

  /** The access rules of the channel. */
  SECURITY_RULES,

  /** The users of the channel's access rules. */
  SECURITY_USERS,

  /** A configuration file of the channel. */
  CONFIG
}
