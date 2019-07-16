package at.porscheinformatik.zanata;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ZanataMessageSourceTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private ZanataMessageSource messageSource;
  private static final ZanataMessageSource.TextFlowTarget TEXT_1 = new ZanataMessageSource.TextFlowTarget();
  private static final ZanataMessageSource.TextFlowTarget TEXT_2 = new ZanataMessageSource.TextFlowTarget();
  private static final ZanataMessageSource.TextFlowTarget TEXT_3 = new ZanataMessageSource.TextFlowTarget();
  private static final ZanataMessageSource.TextFlowTarget TEXT_4 = new ZanataMessageSource.TextFlowTarget();
  private static final ZanataMessageSource.TextFlowTarget TEXT_5 = new ZanataMessageSource.TextFlowTarget();

  static {
    TEXT_1.resId = "text1";
    TEXT_1.content = "Hallo Welt";
    TEXT_1.state = ZanataMessageSource.ContentState.Translated;
    TEXT_2.resId = "text1";
    TEXT_2.content = "Hallo Welt 2";
    TEXT_2.state = ZanataMessageSource.ContentState.Translated;
    TEXT_3.resId = "text3";
    TEXT_3.content = "Hy there";
    TEXT_3.state = ZanataMessageSource.ContentState.Translated;
    TEXT_4.resId = "text3";
    TEXT_4.content = "Hi deer";
    TEXT_4.state = ZanataMessageSource.ContentState.Translated;
    TEXT_5.resId = "text5";
    TEXT_5.content = "My World";
    TEXT_5.state = ZanataMessageSource.ContentState.Translated;
  }

  private MockRestServiceServer mockServer;

  @Before
  public void setup() {
    messageSource = new ZanataMessageSource();
    messageSource.setProject("MyApp");
    messageSource.setZanataBaseUrl("https://my-zanata/zanata");
    messageSource.setIteration("myiteration");
    RestTemplate restTemplate = new RestTemplate();
    messageSource.setRestTemplate(restTemplate);
    messageSource.useAuthentcation("user", "token");

    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @After
  public void verify() {
    mockServer.verify();
  }

  @Test
  public void simpleReseourceAndCache() throws JsonProcessingException {
    mockCallLocales(Locale.GERMAN.toLanguageTag());
    mockCallTranslations(Locale.GERMAN, TEXT_1);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
    // try another time for caching
    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
  }

  @Test
  public void clearCache() throws JsonProcessingException {
    mockCallLocales(Locale.GERMAN.toLanguageTag());
    mockCallTranslations(Locale.GERMAN, TEXT_1);
    mockCallLocales(Locale.GERMAN.toLanguageTag());
    mockCallTranslations(Locale.GERMAN, TEXT_1);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
    messageSource.clearCache();
    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
  }

  @Test
  public void langAndCountry() throws JsonProcessingException {
    mockCallLocales(Locale.GERMAN.toLanguageTag(), Locale.GERMANY.toLanguageTag());
    mockCallTranslations(Locale.GERMANY, TEXT_2);
    mockCallTranslations(Locale.GERMAN, TEXT_1);

    assert "Hallo Welt 2".equals(messageSource.getMessage("text1", null, Locale.GERMANY));
  }

  @Test(expected = NoSuchMessageException.class)
  public void httpError() throws JsonProcessingException {
    mockCallLocales(Locale.ENGLISH.toLanguageTag());
    mockServer.expect(anything()).andRespond(MockRestResponseCreators.withServerError());

    messageSource.getMessage("text1", null, Locale.ENGLISH);
  }

  @Test
  public void multipleBaseNames() throws JsonProcessingException {
    mockCallLocales(Locale.FRENCH.toLanguageTag());
    messageSource.setBaseNames("bundle1", "bundle2");
    mockCallTranslations(Locale.FRENCH, "bundle1", TEXT_1);
    mockCallTranslations(Locale.FRENCH, "bundle2", TEXT_2);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.FRENCH));
  }

  @Test
  public void allProperties() throws JsonProcessingException {
    mockCallLocales(Locale.US.toLanguageTag(), Locale.ENGLISH.toLanguageTag());
    mockCallTranslations(Locale.US, TEXT_4);
    mockCallTranslations(Locale.ENGLISH, TEXT_1, TEXT_3);

    final Properties allProperties = messageSource.getAllProperties(Locale.US);
    assert "Hallo Welt".equals(allProperties.getProperty("text1"));
    assert "Hi deer".equals(allProperties.getProperty("text3"));
  }

  @Test
  public void withAuthentication() throws JsonProcessingException {
    mockCallLocales(Locale.GERMAN.toLanguageTag());
    mockServer
      .expect(header("X-Auth-User", "user"))
      .andExpect(header("X-Auth-Token", "token"))
      .andRespond(withSuccess());

    messageSource.getAllProperties(Locale.GERMAN);
  }

  @Test(expected = NoSuchMessageException.class)
  public void testLanguages() throws JsonProcessingException {
    Locale hu = new Locale("hu", "HU");
    mockCallLocales(Locale.GERMAN.toLanguageTag(), Locale.US.toLanguageTag());
    mockCallTranslations(Locale.GERMAN, TEXT_1);
    mockCallTranslations(Locale.US, TEXT_3);

    assert "Hallo Welt".equals(messageSource.getMessage("text1", null, Locale.GERMAN));
    assert "Hy there".equals(messageSource.getMessage("text3", null, Locale.US));
    messageSource.getMessage("text3", null, hu); // NoSuchMessageException
  }

  @Test
  public void testLanguageInheritance() throws JsonProcessingException
  {
    Locale locale3 = new Locale("hu", "HU", "VARIANT");
    Locale locale2 = new Locale("hu", "HU");
    Locale locale1 = new Locale("hu");
    mockCallLocales(locale3.toString().replaceAll("_", "-"), locale2.toLanguageTag(), locale1.toLanguageTag());
    mockCallTranslations(locale3, TEXT_2);
    mockCallTranslations(locale2, TEXT_1, TEXT_4);
    mockCallTranslations(locale1, TEXT_3, TEXT_5);

    assert TEXT_2.content.equals(messageSource.getMessage("text1", null, locale3));
    assert TEXT_4.content.equals(messageSource.getMessage("text3", null, locale3));
    assert TEXT_5.content.equals(messageSource.getMessage("text5", null, locale3));
  }

  private void mockCallTranslations(Locale locale, ZanataMessageSource.TextFlowTarget... textFlowTarget)
      throws JsonProcessingException {
    mockCallTranslations(locale, "messages", textFlowTarget);
  }

  private void mockCallTranslations(Locale locale, String resource, ZanataMessageSource.TextFlowTarget... text)
      throws JsonProcessingException {
    ZanataMessageSource.TranslationsResource answer = new ZanataMessageSource.TranslationsResource();
    answer.textFlowTargets.addAll(Arrays.asList(text));
    mockServer
      .expect(requestTo("https://my-zanata/zanata/rest/projects/p/"
        + messageSource.getProject()
        + "/iterations/i/myiteration/r/" + resource
        + "/translations/" + locale.toString().replaceAll("_", "-")))
      .andRespond(withSuccess(objectMapper.writeValueAsString(answer), MediaType.APPLICATION_JSON));
  }

  private void mockCallLocales(String... localeIds) throws JsonProcessingException {
    ZanataMessageSource.LocaleDetails[] answer = Arrays.stream(localeIds).map(localId -> {
      ZanataMessageSource.LocaleDetails detail = new ZanataMessageSource.LocaleDetails();
      detail.localeId = localId;
      return detail;
    }).toArray(ZanataMessageSource.LocaleDetails[]::new);
    mockServer
      .expect(requestTo("https://my-zanata/zanata/rest/projects/p/"
        + messageSource.getProject()
        + "/iterations/i/myiteration/locales"))
      .andRespond(withSuccess(objectMapper.writeValueAsString(answer), MediaType.APPLICATION_JSON));
  }
}
