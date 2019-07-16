package at.porscheinformatik.zanata;

import static java.util.Collections.singletonList;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * {@link MessageSource} loading texts from Zanata translation server via REST API.
 *
 * <p>
 * If you use the {@link ZanataMessageSource} with a parent {@link org.springframework.context.support.ReloadableResourceBundleMessageSource}
 * and want to resolve all properties you can use the {@link AllPropertiesReloadableResourceBundleMessageSource}
 * instead.
 * </p>
 */
public class ZanataMessageSource extends AbstractMessageSource implements AllPropertiesSource {

  private RestTemplate restTemplate;
  private String zanataBaseUrl;
  private String project;
  private String iteration = "master";

  private Set<String> existingLocales;
  private final Object existingLocalesLock = new Object();
  private final Set<String> basenameSet = new LinkedHashSet<>(singletonList("messages"));
  private final Map<Locale, TranslationsResource[]> translationsCache = new ConcurrentHashMap<>();

  /**
   * @return the Zanata URL
   */
  public String getZanataBaseUrl() {
    return zanataBaseUrl;
  }

  /**
   * Set Zanata URL.
   *
   * @param zanataBaseUrl the URL of your Zanata instance without trailing /
   */
  public void setZanataBaseUrl(String zanataBaseUrl) {
    this.zanataBaseUrl = zanataBaseUrl;
  }

  /**
   * @return project id in Zanata
   */
  public String getProject() {
    return project;
  }

  /**
   * Set project id.
   *
   * @param project project id in Zanata
   */
  public void setProject(String project) {
    this.project = project;
  }

  /**
   * @return iteration/version in Zanata
   */
  public String getIteration() {
    return iteration;
  }

  /**
   * Set iteration.
   *
   * @param iteration iteration/version in Zanata
   */
  public void setIteration(String iteration) {
    this.iteration = iteration;
  }

  /**
   * Set multiple base names (message file resources)
   *
   * @param baseNames list of message resources
   */
  public void setBaseNames(String... baseNames) {
    this.basenameSet.clear();
    for (String baseName : baseNames) {
      this.basenameSet.add(baseName);
    }
  }

  /**
   * Set single base name (message file resource)
   *
   * @param baseName message resource
   */
  public void setBaseName(String baseName) {
    setBaseNames(baseName);
  }

  /**
   * Set the {@link RestTemplate} to use for getting data from Zanata REST API.
   *
   * @param restTemplate the {@link RestTemplate}
   */
  public void setRestTemplate(RestTemplate restTemplate) {
    if (this.restTemplate != null) {
      throw new IllegalStateException("Rest template already set (maybe throgh useAuthentcation(.");
    }
    this.restTemplate = restTemplate;
  }

  /**
   * Sets {@link ZanataAuthenticationInterceptor} for calling the REST API. <b>Be aware</b>: this
   * replaces all interceptors in the {@link RestTemplate}.
   *
   * @param authUser Zanata API user
   * @param authToken Zanata API token
   */
  public void useAuthentcation(String authUser, String authToken) {
    if (restTemplate == null) {
      restTemplate = new RestTemplate();
    }
    restTemplate.setInterceptors(singletonList(new ZanataAuthenticationInterceptor(authUser, authToken)));
  }

  /**
   * Clears the cache for all locales and message bundles.
   */
  public void clearCache() {
    logger.info("Going to clear cache...");
    synchronized (existingLocalesLock) {
      existingLocales = null;
    }
    translationsCache.clear();
  }

  private TranslationsResource[] loadTranslations(Locale locale) {
    TranslationsResource[] translations = translationsCache.get(locale);

    if (translations != null) {
      return translations;
    }

    List<TranslationsResource> translationList = new ArrayList<>();

    for (String baseName : basenameSet) {

      if (StringUtils.hasText(locale.getVariant())) {
        TranslationsResource translation = loadTranslation(locale.getLanguage() + "-" + locale.getCountry() + "-" + locale.getVariant(), baseName);
        if (translation != null) {
          translationList.add(translation);
        }
      }

      if (StringUtils.hasText(locale.getCountry())) {
        TranslationsResource translation = loadTranslation(locale.getLanguage() + "-" + locale.getCountry(), baseName);
        if (translation != null) {
          translationList.add(translation);
        }
      }

      TranslationsResource translation = loadTranslation(locale.getLanguage(), baseName);
      if (translation != null) {
        translationList.add(translation);
      }
    }

    translations = translationList.toArray(new TranslationsResource[translationList.size()]);
    translationsCache.put(locale, translations);

    return translations;
  }

  private TranslationsResource loadTranslation(String language, String resourceName) {
    synchronized (existingLocalesLock) {
      if (existingLocales == null) {
        existingLocales = Arrays.stream(loadLocales())
          .map(locale -> locale.localeId)
          .collect(Collectors.toSet());
      }

      if (!existingLocales.contains(language)) {
        logger.info("Locale not exists " + language);
        return null;
      }
    }

    try {
      URI uri = new URI(zanataBaseUrl
        + "/rest/projects/p/" + project
        + "/iterations/i/" + iteration
        + "/r/" + resourceName
        + "/translations/" + language);

      RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();

      if (restTemplate == null) {
        restTemplate = new RestTemplate();
      }
      ResponseEntity<TranslationsResource> response = restTemplate.exchange(request, TranslationsResource.class);

      return response.getBody();
    } catch (RestClientException | URISyntaxException e) {
      logger.warn("Could not load translations for lang " + language, e);
    }
    return null;
  }

  private LocaleDetails[] loadLocales() {
    try {
      URI uri = new URI(zanataBaseUrl
        + "/rest/projects/p/" + project
        + "/iterations/i/" + iteration
        + "/locales");

      RequestEntity request = RequestEntity.get(uri).accept(MediaType.APPLICATION_JSON).build();

      if (restTemplate == null) {
        restTemplate = new RestTemplate();
      }

      ResponseEntity<LocaleDetails[]> response = restTemplate.exchange(request, LocaleDetails[].class);

      return response.getBody();
    } catch (RestClientException | URISyntaxException e) {
      logger.warn("Could not load languages", e);
    }
    return new LocaleDetails[0];
  }

  @Override
  protected MessageFormat resolveCode(String code, Locale locale) {
    TranslationsResource[] translations = loadTranslations(locale);

    return Arrays.stream(translations)
      .flatMap(translation -> translation.textFlowTargets.stream())
      .filter(textFlowTarget -> textFlowTarget.resId.equals(code))
      .map(tf -> new MessageFormat(tf.content, locale))
      .findFirst()
      .orElse(null);
  }

  @Override
  protected String resolveCodeWithoutArguments(String code, Locale locale) {
    TranslationsResource[] translations = loadTranslations(locale);

    return Arrays.stream(translations)
      .flatMap(translation -> translation.textFlowTargets.stream())
      .filter(textFlowTarget -> textFlowTarget.resId.equals(code))
      .map(tf -> tf.content)
      .findFirst()
      .orElse(null);
  }

  @Override
  public Properties getAllProperties(Locale locale) {
    Properties allProperties = new Properties();

    final TranslationsResource[] translationsResources = loadTranslations(locale);
    if (translationsResources != null && translationsResources.length > 0) {
      for (TranslationsResource translationsResource : translationsResources) {
        translationsResource.textFlowTargets.stream()
          .collect(Collectors.toMap(t -> t.resId, t -> t.content))
          .forEach(allProperties::putIfAbsent);
      }
    }

    MessageSource parentMessageSource = getParentMessageSource();
    if (parentMessageSource instanceof AllPropertiesSource) {
      ((AllPropertiesSource) parentMessageSource).getAllProperties(locale)
        .forEach(allProperties::putIfAbsent);
    }

    return allProperties;
  }

  /**
   * Represents the translation of a document into a single locale.
   */
  static class TranslationsResource {
    public List<TextFlowTarget> textFlowTargets = new ArrayList<>();
  }

  /**
   * This class contains string contents for a single translatable message.
   */
  static class TextFlowTarget {
    public String resId;
    public ContentState state;
    public String content;
  }

  /**
   * State of {@link TextFlowTarget}
   */
  enum ContentState {
    New, NeedReview, Translated, Approved, Rejected
  }

  /**
   * Represents the metadata of a single locale for the project.
   */
  static class LocaleDetails {
    public String displayName;
    public Boolean enabled;
    public Boolean enabledByDefault;
    public String localeId;
    public String nativeName;
    public String pluralForms;
    public Boolean rtl;
  }
}
