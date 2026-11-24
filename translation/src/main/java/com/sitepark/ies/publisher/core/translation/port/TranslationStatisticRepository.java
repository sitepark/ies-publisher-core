package com.sitepark.ies.publisher.core.translation.port;

import com.sitepark.ies.publisher.core.translation.domain.entity.TranslatableStatisticEntry;
import com.sitepark.ies.publisher.core.translation.domain.entity.TranslatedStatisticEntry;
import com.sitepark.ies.publisher.core.translation.domain.entity.TranslationStatisticFilter;
import java.util.List;

/**
 * Read access to the recorded translation statistics.
 *
 * <p>Implementations live outside this library in adapter modules.
 */
public interface TranslationStatisticRepository {

  /**
   * Returns the volume of texts that would have been translated, per period and publisher.
   *
   * @param filter the requested periods
   * @return the entries ordered by period and publisher, or an empty list
   */
  List<TranslatableStatisticEntry> getTranslatable(TranslationStatisticFilter filter);

  /**
   * Returns the volume of texts that were translated, per period and target language.
   *
   * @param filter the requested periods
   * @return the entries ordered by period and language, or an empty list
   */
  List<TranslatedStatisticEntry> getTranslated(TranslationStatisticFilter filter);
}
