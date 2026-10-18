package com.ai.scim.mapper;

import com.unboundid.scim2.common.types.Meta;
import java.time.Instant;

/** Builds SCIM {@code meta} blocks and weak ETags from aggregate versions. */
public final class ScimMeta {

  private ScimMeta() {}

  /**
   * Builds the {@code meta} attribute.
   *
   * @param resourceType SCIM resource type
   * @param location absolute resource URL
   * @param created creation time
   * @param lastModified last modification time
   * @param version aggregate {@code @Version}
   * @return meta block
   */
  public static Meta create(
      String resourceType, String location, Instant created, Instant lastModified, long version) {
    Meta meta = new Meta();
    meta.setResourceType(resourceType);
    meta.setLocationString(location);
    if (created != null) {
      meta.setCreatedMillis(created.toEpochMilli());
    }
    if (lastModified != null) {
      meta.setLastModifiedMillis(lastModified.toEpochMilli());
    }
    meta.setVersion(etag(version));
    return meta;
  }

  /**
   * Returns the weak ETag for an aggregate version.
   *
   * @param version aggregate {@code @Version}
   * @return ETag such as {@code W/"3"}
   */
  public static String etag(long version) {
    return "W/\"" + version + "\"";
  }

  /**
   * Parses an {@code If-Match} header into a version.
   *
   * @param ifMatch header value, may be {@code null} or {@code *}
   * @return version, {@code null} when no check applies, or {@code -1} when unparseable so the
   *     precondition fails
   */
  public static Long parseIfMatch(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank() || "*".equals(ifMatch.trim())) {
      return null;
    }
    String value = ifMatch.trim();
    if (value.startsWith("W/")) {
      value = value.substring(2);
    }
    value = value.replace("\"", "");
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return -1L;
    }
  }
}
