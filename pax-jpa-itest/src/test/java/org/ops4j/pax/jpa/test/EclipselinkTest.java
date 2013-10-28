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
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.jpa.test.TestConfiguration.regressionDefaults;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.ops4j.pax.jpa.sample1.model.Author;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class EclipselinkTest {

    @Inject
    private BundleContext bc;

    @Inject
    @Filter("(osgi.unit.name=library)")
    private EntityManagerFactory emf;

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            mavenBundle("org.ops4j.pax.jpa", "pax-jpa").versionAsInProject(),
            mavenBundle("org.ops4j.pax.jpa", "pax-jpa-eclipselink").versionAsInProject(),

            mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
            mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample1").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jpa_2.0_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-jta_1.1_spec").versionAsInProject(),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec")
                .versionAsInProject(),

            mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.antlr")
                .versionAsInProject(),
            mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.asm")
                .versionAsInProject(),
            mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.core")
                .versionAsInProject(),
            mavenBundle("org.eclipse.persistence", "org.eclipse.persistence.jpa")
                .versionAsInProject(),

            mavenBundle("org.apache.derby", "derby").versionAsInProject(),

            mavenBundle("org.osgi", "org.osgi.enterprise").versionAsInProject());
    }

    @Test
    public void createEntityManager() {
        assertNotNull(bc);
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            assertNotNull(em);
            CriteriaBuilder cb = emf.getCriteriaBuilder();
            CriteriaQuery<Author> q = cb.createQuery(Author.class);
            q.from(Author.class);
            TypedQuery<Author> typedQuery = em.createQuery(q);
            List<Author> list = typedQuery.getResultList();
            assertNotNull(list);
            assertTrue(list.isEmpty());
        } finally {
            em.getTransaction().rollback();
        }
        assertNotNull(em);
    }
}
