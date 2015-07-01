package org.ops4j.pax.jpa.test;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;

import org.ops4j.pax.exam.MavenUtils;

import java.io.File;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.exam.util.Filter;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class PaxJpaFeaturesInstallTest {

    @Inject
    FeaturesService featuresService;

    @Inject
    @Filter("(osgi.unit.name=library)")
    private EntityManagerFactory emf;

    @Configuration
    public Option[] config() {

        MavenUrlReference karafUrl = CoreOptions.maven()
                .groupId("org.apache.karaf")
                .artifactId("apache-karaf")
                .versionAsInProject()
                .type("tar.gz");

        MavenUrlReference paxJpaRepo = CoreOptions.maven()
                .groupId("org.ops4j.pax.jpa")
                .artifactId("pax-jpa-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();

        return new Option[]{
                // KarafDistributionOption.debugConfiguration("5005", true),
                KarafDistributionOption.karafDistributionConfiguration()
                        .frameworkUrl(karafUrl)
                        .unpackDirectory(new File("target/exam"))
                        //.useDeployFolder(false)
                        .karafVersion(
                            MavenUtils.getArtifactVersion("org.apache.karaf",
                                "apache-karaf")),
                KarafDistributionOption.keepRuntimeFolder(),
                cleanCaches(),
                configureConsole().ignoreLocalConsole(),

                // don't use until released
                // KarafDistributionOption.features(paxJdbc, "pax-jdbc-derby"),
                mavenBundle("org.ops4j.pax.jpa.samples", "pax-jpa-sample1").versionAsInProject(),
                mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc-spec").versionAsInProject(),
                mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc").versionAsInProject(),
                mavenBundle("org.ops4j.pax.jdbc", "pax-jdbc-derby").versionAsInProject(),
                mavenBundle("org.apache.derby", "derby").versionAsInProject(),
                KarafDistributionOption.features(paxJpaRepo, "pax-jpa", "pax-jpa-eclipselink"),
        };
    }

    @Test
    public void testPaxJpaEclipseLinkFeatureIsDeployedAndUsable() throws Exception {
        Assert.assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-jpa-eclipselink")));

        Assert.assertThat(emf, notNullValue());
    }

}