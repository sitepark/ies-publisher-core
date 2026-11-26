package com.sitepark.ies.publisher.core.publishing.domain.value;

/** Who asked for a list of publish tasks. */
public enum PublishListOrigin {

  /** A user working on content: saving, publishing or revoking an entity. */
  EDITOR,

  /** A change made elsewhere that other entities depend on. */
  DEPENDENCY,

  /** Time: entities due to be published, expired, or checked regularly. */
  SCHEDULE,

  /** An administrator, or a tool acting for one: a full generation, a repair. */
  ADMINISTRATION
}
