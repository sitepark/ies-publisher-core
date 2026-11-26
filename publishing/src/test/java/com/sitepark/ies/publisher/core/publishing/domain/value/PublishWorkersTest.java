package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishWorkersTest {

  @Test
  void leftToTheServerHalfOfTheProcessorsPublish() {
    PublishWorkers workers =
        PublishWorkers.calculate(PublishWorkersSetting.auto(), new PublishingResources(16, 40));

    assertThat(workers.effective())
        .as("16 processors allow 8 workers, the pool of 40 would allow 10")
        .isEqualTo(8);
  }

  @Test
  void leftToTheServerTheConnectionPoolLimitsTheWorkers() {
    PublishWorkers workers =
        PublishWorkers.calculate(PublishWorkersSetting.auto(), new PublishingResources(32, 40));

    assertThat(workers.effective())
        .as("32 processors would allow 16 workers, the pool of 40 allows 10")
        .isEqualTo(10);
  }

  @Test
  void aSmallServerPublishesWithOneWorker() {
    PublishWorkers workers =
        PublishWorkers.calculate(PublishWorkersSetting.auto(), new PublishingResources(1, 5));

    assertThat(workers.effective())
        .as("there is always one worker, however little the server has")
        .isEqualTo(1);
  }

  @Test
  void aGivenNumberMayExceedTheProcessorLimit() {
    PublishWorkers workers =
        PublishWorkers.calculate(PublishWorkersSetting.fixed(6), new PublishingResources(4, 40));

    assertThat(workers.effective())
        .as("how much of the processors publishing takes is the administrator's call")
        .isEqualTo(6);
    assertThat(workers.capped()).as("the number was taken as given").isFalse();
  }

  @Test
  void aGivenNumberIsReducedToWhatTheConnectionPoolAllows() {
    PublishWorkers workers =
        PublishWorkers.calculate(PublishWorkersSetting.fixed(20), new PublishingResources(16, 40));

    assertThat(workers.effective())
        .as("more workers than the pool can serve would wait for each other")
        .isEqualTo(10);
    assertThat(workers.capped())
        .as("the administrator is told that the number was reduced")
        .isTrue();
  }

  @Test
  void tellsWhatTheNumberComesFrom() {
    PublishWorkers workers =
        PublishWorkers.calculate(PublishWorkersSetting.auto(), new PublishingResources(16, 40));

    assertThat(workers)
        .as("the administrator sees both limits and what they are made of")
        .isEqualTo(
            new PublishWorkers(PublishWorkersSetting.auto(), 8, 16, 8, 40, 10, 3, 10, false));
  }
}
