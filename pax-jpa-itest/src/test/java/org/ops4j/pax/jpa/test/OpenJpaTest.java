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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.jpa.test.TestConfiguration.regressionDefaults;
import static org.ops4j.pax.jpa.test.TestConfiguration.workspaceBundle;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OpenJpaTest {

    @Inject
    private BundleContext bc;

    @Inject
    @Filter("(osgi.unit.name=library)")
    private EntityManagerFactory emf;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(), //
            workspaceBundle("org.ops4j.pax.jpa", "pax-jpa"), //
            mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
            mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample1").versionAsInProject(),
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

            mavenBundle("org.apache.derby", "derby").versionAsInProject(),

            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject());
    }

    @Test
    public void createEntityManager() {
        assertNotNull(emf);
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
    }

    @Test
    public void stopAndStartJdbcDriverBundle() throws BundleException {
        Bundle derbyBundle = BundleUtils.getBundle(bc, "derby");
        derbyBundle.stop();
        assertThat(bc.getServiceReference(EntityManagerFactory.class), is(nullValue()));
        derbyBundle.start();
        assertThat(ServiceLookup.getService(bc, EntityManagerFactory.class), is(notNullValue()));
    }

    @Test
    public void stopAndStartPersistenceBundle() throws BundleException, InterruptedException {
        Bundle persistenceBundle = BundleUtils.getBundle(bc, "org.ops4j.pax.jpa.sample1.model");
        persistenceBundle.stop();
        assertThat(bc.getServiceReference(EntityManagerFactory.class), is(nullValue()));
        persistenceBundle.start();
        assertThat(ServiceLookup.getService(bc, EntityManagerFactory.class), is(notNullValue()));
    }

    @Test
    public void stopAndStartPersistenceProviderBundle() throws BundleException {
        Bundle providerBundle = BundleUtils.getBundle(bc, "org.apache.openjpa");
        providerBundle.stop();
        assertThat(bc.getServiceReference(EntityManagerFactory.class), is(nullValue()));
        providerBundle.start();
        assertThat(ServiceLookup.getService(bc, EntityManagerFactory.class), is(notNullValue()));
    }

    @Test
    public void stopAndStartExtenderBundle() throws BundleException {
        Bundle extenderBundle = BundleUtils.getBundle(bc, "org.ops4j.pax.jpa");
        extenderBundle.stop();
        assertThat(bc.getServiceReference(WeavingHook.class), is(nullValue()));
        assertThat(bc.getServiceReference(EntityManagerFactoryBuilder.class), is(nullValue()));
        assertThat(bc.getServiceReference(EntityManagerFactory.class), is(nullValue()));

        extenderBundle.start();
        assertThat(ServiceLookup.getService(bc, EntityManagerFactory.class), is(notNullValue()));
        assertThat(ServiceLookup.getService(bc, EntityManagerFactoryBuilder.class), is(notNullValue()));
        assertThat(ServiceLookup.getService(bc, WeavingHook.class), is(notNullValue()));
    }
}
