package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * How many workers publish in the background, and what that number comes from.
 *
 * <p>Two things limit the number. Workers compute, so more of them than half the processors leave
 * too little for editors, the database and everything else the server does. And publishing an
 * entity holds several database connections at once, so more workers than the pool can serve -
 * with a reserve for everybody else - wait for each other with a connection in hand.
 *
 * <p>Left to the server, the number is the smaller of both limits. A number given by an
 * administrator may exceed the processor limit, which is a matter of taste, but not the limit of
 * the connection pool, beyond which publishing fails instead of slowing down.
 *
 * @param setting what an administrator set; must not be {@code null}
 * @param effective the number of workers that publish, at least 1
 * @param processors the processors available to the server
 * @param processorLimit the workers half of the processors allow, at least 1
 * @param connectionPoolSize the database connections the server may hold at the same time
 * @param connectionPoolReserve the connections kept free for everything but publishing
 * @param connectionsPerWorker the connections a worker holds at once while it publishes
 * @param connectionPoolLimit the workers the connection pool allows, at least 1
 * @param capped whether the number given by an administrator was reduced to the limit of the
 *     connection pool
 */
public record PublishWorkers(
    PublishWorkersSetting setting,
    int effective,
    int processors,
    int processorLimit,
    int connectionPoolSize,
    int connectionPoolReserve,
    int connectionsPerWorker,
    int connectionPoolLimit,
    boolean capped) {

  /** The share of the processors workers may use: one in this many. */
  private static final int PROCESSOR_SHARE = 2;

  /** The database connections kept free for editors and other services. */
  private static final int CONNECTION_POOL_RESERVE = 10;

  /** The database connections publishing one entity holds at the same time. */
  private static final int CONNECTIONS_PER_WORKER = 3;

  /**
   * Works out how many workers publish with a setting on a server.
   *
   * @param setting what an administrator set
   * @param resources what the server has
   * @return the number of workers and what it comes from
   */
  public static PublishWorkers calculate(
      PublishWorkersSetting setting, PublishingResources resources) {
    int processorLimit = Math.max(1, resources.processors() / PROCESSOR_SHARE);
    int connectionPoolLimit =
        Math.max(
            1, (resources.connectionPoolSize() - CONNECTION_POOL_RESERVE) / CONNECTIONS_PER_WORKER);
    Integer given = setting.workers();
    int wanted = given == null ? processorLimit : given;
    int effective = Math.min(wanted, connectionPoolLimit);
    return new PublishWorkers(
        setting,
        effective,
        resources.processors(),
        processorLimit,
        resources.connectionPoolSize(),
        CONNECTION_POOL_RESERVE,
        CONNECTIONS_PER_WORKER,
        connectionPoolLimit,
        given != null && given > connectionPoolLimit);
  }
}
