package at.porscheinformatik.zanata;

import java.util.Locale;
import java.util.Properties;

public interface AllPropertiesSource
{
  /**
   * @param locale the locale to load the properties
   * @return all properties provided from zanata for this locale
   */
  Properties getAllProperties(Locale locale);
}
