/**
 * Copyright 2012 HYPOPORT AG
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.hypoport.ep2.support.configuration.properties;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;


/**
 * @author Timmo Freudl-Gierke
 */
public class PropertiesLoader {

  public static final String PROPERTY_LOCATIONS = "propertyLocations";

  private static final Logger LOG = Logger.getLogger(PropertiesLoader.class);

  static boolean initialized = false;
  private static final Set<Object> sslPropertiesKeys;

  static {
    sslPropertiesKeys = new HashSet<>();
    sslPropertiesKeys.add("javax.net.ssl.trustStore");
    sslPropertiesKeys.add("javax.net.ssl.trustStorePassword");
  }

  /** Evaluates system property "propertyLocations" to find property files. */
  public static void loadPropertiesLocationsIntoSystemProperties() {
    if (initialized) {
      return;
    }

    initializeInternal();
  }

  private static synchronized void initializeInternal() {
    if (!initialized) {
      String propertyLocations = System.getProperty(PROPERTY_LOCATIONS);

      Properties loadedProperties = loadPropertiesLocations(propertyLocations);
      Set<Object> loadedPropertyKeys = new HashSet<Object>(loadedProperties.keySet());

      mergePropertiesIntoSystemPropertiesWithoutOverwriting(loadedProperties);

      replacePropertyPlaceHolder(System.getProperties());

      logProperties(loadedPropertyKeys, System.getProperties());
      logProperties(sslPropertiesKeys, System.getProperties());
      initialized = true;
    }
  }

  private static void logProperties(Set<Object> filter, Properties properties) {
    String propertiesAsText = "";
    for (Object key : filter) {
      propertiesAsText = propertiesAsText + "\n" + key + "=" + properties.get(key);
    }
    LOG.info("Loaded properties: " + propertiesAsText);
  }

  static Properties loadPropertiesLocations(String propertyLocations) {
    if (isBlank(propertyLocations)) {
      return new Properties();
    }

    Properties loadedProperties = new Properties();
    for (String location : propertyLocations.split(",")) {
      try {
        Properties locationProperties = loadPropertiesFromLocation(location);
        if (propertyLocationsPresent(locationProperties)) {
          Properties recursiveLoadedProperties = loadPropertiesLocations(locationProperties.getProperty(PROPERTY_LOCATIONS));
          locationProperties.putAll(recursiveLoadedProperties);
        }
        loadedProperties.putAll(locationProperties);
      }
      catch (IOException e) {
        LOG.warn(format("Cannot load properties from location %s. Error: %s", location, e.getMessage()));
      }
    }

    return loadedProperties;
  }

  private static boolean propertyLocationsPresent(Properties locationProperties) {
    return locationProperties.containsKey(PROPERTY_LOCATIONS);
  }

  private static void mergePropertiesIntoSystemPropertiesWithoutOverwriting(Properties properties) {
    if (properties == null || properties.size() == 0) {
      return;
    }
    Properties systemProperties = System.getProperties();
    properties.putAll(systemProperties);
    systemProperties.putAll(properties);
  }

  private static Properties loadPropertiesFromLocation(String location) throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("loading from location " + location);
    }
    Properties properties = new Properties();
    if (location.startsWith("classpath:")) {
      properties = loadPropertiesFromClasspathLocation(stripSchema("classpath:", location));
    }
    else if (location.startsWith("file:")) {
      properties = loadPropertiesFromFile(stripSchema("file:", location));
    }
    else {
      throw new IOException("Use one schema from {\"classpath:\",\"file:\"}");
    }
    return properties;
  }

  static String stripSchema(String schema, String location) {
    return location.substring(schema.length(), location.length());
  }

  private static Properties loadPropertiesFromClasspathLocation(String location) throws IOException {
    if (isBlank(location)) {
      return new Properties();
    }
    InputStream inputStream = PropertiesLoader.class.getResourceAsStream(location);
    return loadPropertiesFromInputStream(inputStream);
  }

  private static Properties loadPropertiesFromFile(String location) throws IOException {
    if (isBlank(location)) {
      return new Properties();
    }
    FileInputStream inputStream = new FileInputStream(location);
    return loadPropertiesFromInputStream(inputStream);
  }

  private static Properties loadPropertiesFromInputStream(InputStream inputStream) throws IOException {
    Properties properties = new Properties();

    if (inputStream != null) {
      properties.load(inputStream);
    }

    return properties;
  }

  static void replacePropertyPlaceHolder(Properties properties) {

    Enumeration<?> keyEnum = properties.propertyNames();
    while (keyEnum.hasMoreElements()) {
      String key = (String) keyEnum.nextElement();
      Object value = properties.get(key);
      if (value != null && value instanceof String) {
        String valueString = (String) value;
        valueString = replacePropertyPlaceHolder(valueString, properties);
        properties.put(key, valueString);
      }
    }
  }

  private static String replacePropertyPlaceHolder(String value, Properties properties) {
    while (value.contains("${")) {
      int start = value.indexOf("${");
      int end = value.indexOf("}");
      if (start < end) {
        String propertyPlaceHolder = value.substring(start, end + 1);
        String propertyName = propertyPlaceHolder.substring(2, propertyPlaceHolder.length() - 1);
        Object propertyValue = properties.get(propertyName);
        if (propertyValue == null) {
          propertyValue = "";
        }
        value = value.replace(propertyPlaceHolder, (String) propertyValue);
      }
      else {
        value = value.replace("${", "");
        LOG.warn("Cannot resolve all properties in " + value);
      }
    }
    return value;
  }
}
