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

import java.sql.SQLException;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.exam.util.PathUtils;
import org.osgi.framework.BundleContext;

@RunWith( PaxExam.class )
public class PersistenceUnitTest
{
    @Inject
    private BundleContext bc;
    
    @Inject
    @Filter("(osgi.unit.name=library)")
    private EntityManagerFactory emf;
    
    @Configuration
    public Option[] config()
    {
        return options(
            regressionDefaults(),
            bundle( "reference:file:" + PathUtils.getBaseDir() + "/../pax-jpa/target/classes"),
            mavenBundle( "org.ops4j.base", "ops4j-base-io", "1.3.0" ),
            mavenBundle( "org.ops4j.pax.jdbc", "pax-jdbc", "0.1.0-SNAPSHOT" ),
            mavenBundle( "org.ops4j.pax.jpa.samples", "pax-jpa-sample1-model", "0.1.0-SNAPSHOT" ),
            mavenBundle( "org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),
            mavenBundle( "org.apache.geronimo.specs", "geronimo-jta_1.1_spec", "1.1.1"),
            mavenBundle( "org.apache.geronimo.specs", "geronimo-servlet_3.0_spec" ).versionAsInProject(),

            mavenBundle( "org.apache.openjpa", "openjpa").versionAsInProject(),
            mavenBundle( "commons-lang", "commons-lang").versionAsInProject(),
            mavenBundle( "commons-collections", "commons-collections").versionAsInProject(),
            mavenBundle( "commons-pool", "commons-pool").versionAsInProject(),
            mavenBundle( "commons-dbcp", "commons-dbcp").versionAsInProject(),
            mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.asm").versionAsInProject(),
            mavenBundle( "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.serp", "1.13.1_4"),

            mavenBundle( "org.apache.derby", "derby", "10.8.2.2"),
            
            mavenBundle( "org.osgi", "org.osgi.enterprise" ).versionAsInProject() );
    }

    @Test
    public void createDataSourceAndConnection() throws SQLException, InterruptedException
    {
        assertNotNull( emf );
        EntityManager em = emf.createEntityManager();
        assertNotNull( em );
    }
}
