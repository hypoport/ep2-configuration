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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static de.hypoport.ep2.support.configuration.properties.PropertiesLoader.PROPERTY_LOCATIONS;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Timmo Freudl-Gierke
 */
public class PropertiesLoaderTest {

  @BeforeMethod(alwaysRun = true)
  public void cleanupSystemProperties() {
    PropertiesLoader.initialized = false;
    System.clearProperty(PROPERTY_LOCATIONS);
    System.clearProperty("a");
    System.clearProperty("b");
    System.clearProperty("f");
    System.clearProperty("bleibt");
  }

  @Test
  public void propertyLocations_not_specified() {
    PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();
  }

  @Test
  public void propertyLocations_is_empty() {
    System.setProperty(PROPERTY_LOCATIONS, "");
    PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();
  }

  @Test
  public void specify_property_file_via_system_property() {
    System.setProperty(PROPERTY_LOCATIONS, "classpath:a.properties");
    PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();
    assertThat(System.getProperty("a")).isEqualTo("Anton");
  }

  @Test
  public void several_locations_can_be_loaded__properties_in_the_last_specified_file_will_overrule_properties_from_previous_files() {
    System.setProperty(PROPERTY_LOCATIONS, "classpath:a.properties,classpath:config/b.properties,");

    PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();

    assertThat(System.getProperty("a")).isEqualTo("Anne");
    assertThat(System.getProperty("b")).isEqualTo("berta");
  }

  @Test
  public void unknown_locations_are_handeled_gracefully() {
    Properties properties = PropertiesLoader.loadPropertiesLocations("classpath:config/gibtsNicht.properties");

    assertThat(properties).isNotNull();
    assertThat(properties).hasSize(0);
  }

  @Test
  public void file_sytem_resource_can_be_loaded() throws IOException {
    // given
    File tmpFile = new File(System.getProperty("java.io.tmpdir") + "/test.properties");
    org.apache.commons.io.IOUtils.write("f=Felix", new FileOutputStream(tmpFile));
    System.setProperty(PROPERTY_LOCATIONS, "file:" + tmpFile.getAbsolutePath());

    // when
    PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();

    // then
    assertThat(System.getProperty("f")).isEqualTo("Felix");

    // tear down
    try {
      tmpFile.delete();
    }
    catch (Exception ignore) {
    }
  }

  @Test
  public void existing_SystemProperties_will_not_be_overwritten() {
    System.setProperty("bleibt", "bestehen");
    System.setProperty(PROPERTY_LOCATIONS, "classpath:a.properties");

    PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();

    assertThat(System.getProperty("bleibt")).isEqualTo("bestehen");
  }

  @Test
  public void property_files_can_reference_more_propertyLocations__they_will_be_loaded_as_well() {
    Properties properties = PropertiesLoader.loadPropertiesLocations("classpath:config/b.properties");

    assertThat(properties.getProperty("c")).isEqualTo("Christoph");
  }

  @Test
  public void templating__properties_can_be_used_inside_other_properties() {
    Properties properties = new Properties();
    properties.put("firstname", "Max");
    properties.put("lastname", "Muetze");
    properties.put("fullname", "${firstname} ${lastname}");

    PropertiesLoader.replacePropertyPlaceHolder(properties);

    assertThat(properties.getProperty("fullname")).isEqualTo("Max Muetze");
  }

  @Test
  public void properties_templating_ignores_unspecified_properties() {
    Properties properties = new Properties();
    properties.put("fullname", "${lastname}");

    PropertiesLoader.replacePropertyPlaceHolder(properties);

    assertThat(properties.getProperty("fullname")).isEqualTo("");
  }

  @Test
  public void properties_templating_ignores_missing_closing_bracket() {
    Properties properties = new Properties();
    properties.put("fullname", "Herr ${name");

    PropertiesLoader.replacePropertyPlaceHolder(properties);

    assertThat(properties.getProperty("fullname")).isEqualTo("Herr name");
  }

  @Test
  public void stripSchema() {
    assertThat(PropertiesLoader.stripSchema("classpath:", "classpath:")).isEqualTo("");
    assertThat(PropertiesLoader.stripSchema("classpath:", "classpath:/")).isEqualTo("/");
  }
}
