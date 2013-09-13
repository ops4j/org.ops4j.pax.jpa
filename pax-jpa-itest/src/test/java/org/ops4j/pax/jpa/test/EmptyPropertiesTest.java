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

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.jpa.test.TestConfiguration.regressionDefaults;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class EmptyPropertiesTest {

    @Inject
    @Filter("(osgi.unit.name=emptyProperties)")
    private EntityManagerFactoryBuilder emfBuilder;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(), //
            bundle("reference:file:" + PathUtils.getBaseDir() + "/../pax-jpa/target/classes"),
            mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
            mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample3").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec")
                .versionAsInProject(),

            mavenBundle("org.apache.openjpa", "openjpa").versionAsInProject(),
            mavenBundle("commons-lang", "commons-lang").versionAsInProject(),
            mavenBundle("commons-collections", "commons-collections").versionAsInProject(),
            mavenBundle("commons-pool", "commons-pool").versionAsInProject(),
            mavenBundle("commons-dbcp", "commons-dbcp").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm")
                .versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp")
                .versionAsInProject(),

            mavenBundle("org.apache.derby", "derby").versionAsInProject(),
            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject());
    }

    @Test
    public void checkEntityManagerFactoryBuilder() {
        assertNotNull(emfBuilder);
    }

}
