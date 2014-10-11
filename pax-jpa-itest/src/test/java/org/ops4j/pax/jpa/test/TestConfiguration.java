/*
 * Copyright 2012 Harald Wellmann.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.jpa.test;

import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

import java.io.IOException;
import java.util.Properties;

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;

public class TestConfiguration {
    
    public static boolean debugConsole = true;

    public static Option regressionDefaults() {
        Properties props = new Properties();
        try {
            props.load(TestConfiguration.class.getResourceAsStream("/systemPackages.properties"));
        }
        catch (IOException exc) {
            throw new Ops4jException(exc);
        }

        return composite(

            frameworkProperty("org.osgi.framework.system.packages").value(
                props.getProperty("org.osgi.framework.system.packages")),

            // add SLF4J and logback bundles
            mavenBundle("org.slf4j", "slf4j-api").versionAsInProject().startLevel(
                START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-core").versionAsInProject().startLevel(
                START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject().startLevel(
                START_LEVEL_SYSTEM_BUNDLES),

            mavenBundle("org.apache.felix", "org.apache.felix.scr", "1.6.2"),
            mavenBundle("javax.annotation", "javax.annotation-api", "1.2"),

            // Set logback configuration via system property.
            // This way, both the driver and the container use the same configuration
            systemProperty("logback.configurationFile").value(
                "file:" + PathUtils.getBaseDir() + "/src/test/resources/logback.xml"),
            when(debugConsole).useOptions(    
                systemProperty("osgi.console").value("6666"), //
                systemProperty("eclipse.consoleLog").value("true")), //
            junitBundles());
    }
}
