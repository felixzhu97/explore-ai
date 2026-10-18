package com.ai.scim.infra;

import com.unboundid.scim2.common.utils.JsonUtils;
import java.util.List;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

/**
 * Reads and writes UnboundID SCIM types with the SDK's own {@code JsonMapper}, so SCIM bodies never
 * depend on the application's Jackson settings.
 */
public class ScimJsonHttpMessageConverter extends JacksonJsonHttpMessageConverter {

  public static final String SCIM_JSON_VALUE = "application/scim+json";
  public static final MediaType SCIM_JSON = MediaType.parseMediaType(SCIM_JSON_VALUE);

  private static final String SCIM_PACKAGE = "com.unboundid.scim2.";

  /** Creates the converter for {@code application/scim+json} and {@code application/json}. */
  public ScimJsonHttpMessageConverter() {
    super(JsonUtils.createObjectMapper());
    setSupportedMediaTypes(List.of(SCIM_JSON, MediaType.APPLICATION_JSON));
  }

  @Override
  public boolean canRead(ResolvableType type, MediaType mediaType) {
    return isScimType(type.toClass()) && super.canRead(type, mediaType);
  }

  @Override
  public boolean canWrite(ResolvableType type, Class<?> clazz, MediaType mediaType) {
    return isScimType(clazz) && super.canWrite(type, clazz, mediaType);
  }

  private static boolean isScimType(Class<?> clazz) {
    return clazz != null && clazz.getName().startsWith(SCIM_PACKAGE);
  }
}
