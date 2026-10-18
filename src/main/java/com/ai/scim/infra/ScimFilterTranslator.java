package com.ai.scim.infra;

import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.filters.EqualFilter;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.filters.FilterType;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Parses SCIM filters and exposes {@code attribute eq "value"} comparisons for lookups. */
@Component
public class ScimFilterTranslator {

  /** Maximum filter length accepted from clients. */
  static final int MAX_FILTER_LENGTH = 1024;

  /**
   * Parses a filter expression.
   *
   * @param expression raw {@code filter} query value, may be blank
   * @return parsed filter, or empty when no filter was given
   * @throws BadRequestException when the filter is invalid
   */
  public Optional<Filter> parse(String expression) throws BadRequestException {
    if (expression == null || expression.isBlank()) {
      return Optional.empty();
    }
    if (expression.length() > MAX_FILTER_LENGTH) {
      throw BadRequestException.invalidFilter("filter is too long");
    }
    return Optional.of(Filter.fromString(expression));
  }

  /**
   * Returns the string value when the filter is {@code attribute eq "value"}.
   *
   * @param filter parsed filter
   * @param attribute attribute name, case-insensitive
   * @return compared value, or empty for any other filter shape
   */
  public Optional<String> equalityValue(Filter filter, String attribute) {
    if (filter.getFilterType() != FilterType.EQUAL) {
      return Optional.empty();
    }
    EqualFilter equal = (EqualFilter) filter;
    if (equal.getAttributePath() == null
        || !attribute.equalsIgnoreCase(equal.getAttributePath().toString())
        || equal.getComparisonValue() == null
        || !equal.getComparisonValue().isString()) {
      return Optional.empty();
    }
    return Optional.of(equal.getComparisonValue().asString());
  }
}
