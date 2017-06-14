package at.porscheinformatik.zanata;

import org.springframework.context.MessageSource;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.*;

import static java.util.Collections.singletonList;

/**
 * {@link MessageSource} loading texts from Zanata translation server via REST API.
 */
public class ZanataMessageSource extends AbstractMessageSource {

  private RestOperations restTemplate;
  private String zanataBaseUrl;
  private String project;
  private String iteration = "master";

  private final Set<String> basenameSet = new LinkedHashSet<>(singletonList("messages"));
  private final Map<Locale, TranslationsResource[]> translationsCache = new HashMap<>();

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
    this.setBaseNames(baseName);
  }

  /**
   * Set the {@link RestTemplate} to use for getting data from Zanata REST API.
   *
   * @param restTemplate the {@link RestTemplate}
   */
  public void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public RestOperations getRestTemplate() {
    if (restTemplate == null) {
      restTemplate = new RestTemplate();
    }
    return restTemplate;
  }

  /**
   * Clears the cache for all locales and message bundles.
   */
  public void clearCache() {
    translationsCache.clear();
  }

  private TranslationsResource[] loadTranslations(Locale locale) {
    TranslationsResource[] translations = translationsCache.get(locale);

    if (translations != null) {
      return translations;
    }

    List<TranslationsResource> translationList = new ArrayList<>();

    for (String baseName : basenameSet) {

      if (!StringUtils.isEmpty(locale.getCountry())) {
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
    try {
      return getRestTemplate().getForObject(zanataBaseUrl
        + "/rest/projects/p/" + project
        + "/iterations/i/" + iteration
        + "/r/" + resourceName
        + "/translations/" + language, TranslationsResource.class);
    } catch (RestClientException e) {
      logger.warn("Could not load translations for lang " + language, e);
    }
    return null;
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
}
