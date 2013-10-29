/*
 * Copyright 2013 Harald Wellmann.
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
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.jpa.test.TestConfiguration.regressionDefaults;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class HibernateTest {

    @Inject
    private BundleContext bc;

    @Inject
    @Filter(value = "(osgi.unit.name=library)", timeout = 1000000)
    private EntityManagerFactory emf;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            
            systemProperty("org.jboss.logging.provider").value("slf4j"),
            systemPackages("javax.xml.stream; version=\"1.0.0\"", 
                    "javax.xml.stream.events; version=\"1.0.0\"",
                    "javax.xml.stream.util; version=\"1.0.0\""),
            
            
            bundle("reference:file:" + PathUtils.getBaseDir() + "/../pax-jpa/target/classes"), //

            mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
            mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample1").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").versionAsInProject(),

            //mavenBundle("org.ops4j.pax.jpa", "pax-jpa-hibernate").versionAsInProject(),
            bundle("reference:file:" + PathUtils.getBaseDir() + "/../pax-jpa-hibernate/target/classes"), //
            mavenBundle("org.hibernate", "hibernate-core").versionAsInProject(),
            mavenBundle("org.hibernate", "hibernate-entitymanager").versionAsInProject(),
                
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.ant", "1.8.2_2"),    
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.antlr", "2.7.7_5"),    
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.dom4j", "1.6.1_5"),    
            wrappedBundle(mavenBundle("org.javassist", "javassist", "3.18.1-GA")),
            mavenBundle("com.fasterxml", "classmate", "0.5.4"),
            wrappedBundle(mavenBundle("org.jboss", "jandex", "1.1.0.Final")),
            mavenBundle("org.jboss.logging", "jboss-logging", "3.1.0.GA"),
            mavenBundle("org.hibernate.common", "hibernate-commons-annotations", "4.0.2.Final"),

            mavenBundle("org.apache.derby", "derby").versionAsInProject(),

            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject());
    }

    @Test
    public void createEntityManager() {
        assertNotNull(bc);
        EntityManager em = emf.createEntityManager();
        assertNotNull(em);
    }
}
