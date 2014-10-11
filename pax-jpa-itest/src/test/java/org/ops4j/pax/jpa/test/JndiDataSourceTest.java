/*
 * Copyright 2014 Harald Wellmann.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.jpa.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.jpa.test.TestConfiguration.workspaceBundle;

import java.sql.SQLException;
import java.util.Properties;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.jdbc.DataSourceFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JndiDataSourceTest {

    @Inject
    private BundleContext bc;

    @Inject
    private DataSourceFactory dsf;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(), //
            workspaceBundle("org.ops4j.pax.jpa", "pax-jpa"), //
            mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
            // mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample4").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec")
                .versionAsInProject(),

            mavenBundle("org.apache.openjpa", "openjpa").versionAsInProject(),
            mavenBundle("commons-lang", "commons-lang").versionAsInProject(),
            mavenBundle("commons-collections", "commons-collections").versionAsInProject(),
            mavenBundle("commons-pool", "commons-pool").versionAsInProject(),
            mavenBundle("commons-dbcp", "commons-dbcp").versionAsInProject(),
            mavenBundle("org.apache.xbean", "xbean-asm4-shaded").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp")
                .versionAsInProject(),

            mavenBundle("org.eclipse", "org.eclipse.gemini.naming", "1.0.3.RELEASE"),

            mavenBundle("org.apache.derby", "derby").versionAsInProject(),

            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject());
    }

    @Test
    public void createEntityManager() throws SQLException, BundleException {
        Properties props = new Properties();
        props.put(DataSourceFactory.JDBC_URL, "jdbc:derby:memory:library;create=true");
        DataSource dataSource = dsf.createDataSource(props);
        bc.registerService(DataSource.class, dataSource, null);
        Bundle b = bc.installBundle("mvn:org.ops4j.pax.jpa.samples/pax-jpa-sample4/0.3.0-SNAPSHOT");
        b.start();

        EntityManagerFactory emf = ServiceLookup.getService(bc, EntityManagerFactory.class);
        assertThat(emf, is(notNullValue()));
    }


    @Test
    public void lookupDataSourceFactoryViaJndi() throws Exception {
        InitialContext initialContext = new InitialContext();
        Object service = initialContext.lookup("osgi:service/org.osgi.service.jdbc.DataSourceFactory");
        assertThat(service, is(notNullValue()));
    }
}
