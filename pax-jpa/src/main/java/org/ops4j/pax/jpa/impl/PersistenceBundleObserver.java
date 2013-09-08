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
package org.ops4j.pax.jpa.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.xml.bind.JAXBException;

import org.ops4j.pax.jpa.impl.descriptor.JpaConstants;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceDescriptorParser;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceUnitInfoImpl;
import org.ops4j.pax.jpa.jaxb.Persistence;
import org.ops4j.pax.jpa.jaxb.Persistence.PersistenceUnit;
import org.ops4j.pax.swissbox.extender.BundleManifestScanner;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.extender.RegexKeyManifestFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceBundleObserver implements BundleObserver<ManifestEntry> {

    public static final String BUNDLE_NAME = "org.ops4j.pax.jpa";

    private static Logger log = LoggerFactory.getLogger(PersistenceBundleObserver.class);

    private PersistenceDescriptorParser parser = new PersistenceDescriptorParser();

    private BundleWatcher<ManifestEntry> watcher;

    private List<PersistenceUnitInfoImpl> boundPersistenceUnits = new ArrayList<PersistenceUnitInfoImpl>();
    private List<PersistenceUnitInfoImpl> unboundPersistenceUnits = new ArrayList<PersistenceUnitInfoImpl>();
    private List<ServiceReference<PersistenceProvider>> persistenceProviders = new ArrayList<ServiceReference<PersistenceProvider>>();
    private List<ServiceReference<DataSourceFactory>> dataSourceFactories = new ArrayList<ServiceReference<DataSourceFactory>>();

    public PersistenceBundleObserver() {
        log.debug("instantiating observer");
    }

    @SuppressWarnings("unchecked")
    public void activate(BundleContext bc) {
        log.debug("starting bundle {}", BUNDLE_NAME);

        RegexKeyManifestFilter manifestFilter = new RegexKeyManifestFilter("Meta-Persistence");
        BundleManifestScanner scanner = new BundleManifestScanner(manifestFilter);
        watcher = new BundleWatcher<ManifestEntry>(bc, scanner, this);
        watcher.start();
    }

    public void deactivate(BundleContext bc) {
        log.debug("stopping bundle {}", BUNDLE_NAME);
        watcher.stop();
    }

    public synchronized void addPersistenceProvider(
        ServiceReference<PersistenceProvider> persistenceProvider) {
        log.debug("adding persistence provider {}",
            persistenceProvider.getProperty("javax.persistence.provider"));
        persistenceProviders.add(persistenceProvider);
        scanUnboundPersistenceUnits();

    }

    private void scanUnboundPersistenceUnits() {
        for (PersistenceUnitInfoImpl puInfo : unboundPersistenceUnits) {
            if (isSatisfied(puInfo)) {
                activatePersistenceUnit(puInfo);
            }
        }
        unboundPersistenceUnits.removeAll(boundPersistenceUnits);
    }

    private boolean isSatisfied(PersistenceUnitInfoImpl puInfo) {
        BundleContext bc = puInfo.getBundle().getBundleContext();
        String providerClassName = puInfo.getPersistenceProviderClassName();
        if (providerClassName == null) {
            if (!persistenceProviders.isEmpty() && !dataSourceFactories.isEmpty()) {
                PersistenceProvider provider = bc.getService(persistenceProviders.get(0));
                puInfo.setProvider(provider);

                DataSourceFactory dsf = bc.getService(dataSourceFactories.get(0));
                puInfo.setDataSourceFactory(dsf);
                return true;
            }
        }
        return false;
    }

    private void activatePersistenceUnit(PersistenceUnitInfoImpl puInfo) {
        PersistenceProvider provider = puInfo.getProvider();
        Bundle bundle = puInfo.getBundle();
        Properties emfProps = (Properties) puInfo.getProperties().clone();
        emfProps.remove(JpaConstants.JPA_DRIVER);
        emfProps.remove(JpaConstants.JPA_URL);
        emfProps.remove(JpaConstants.JPA_PASSWORD);
        emfProps.remove(JpaConstants.JPA_USER);

        EntityManagerFactory emf = provider.createContainerEntityManagerFactory(puInfo, emfProps);

        Dictionary<String, String> emfReg = new Hashtable<String, String>();
        emfReg.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, puInfo.getPersistenceUnitName());
        emfReg.put(EntityManagerFactoryBuilder.JPA_UNIT_VERSION, bundle.getVersion().toString());
        emfReg.put(EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER, provider.getClass().getName());
        ServiceRegistration<EntityManagerFactory> reg = bundle.getBundleContext().registerService(
            EntityManagerFactory.class, emf, emfReg);
        puInfo.setEmfRegistration(reg);
        boundPersistenceUnits.add(puInfo);
    }

    private void deactivatePersistenceUnit(PersistenceUnitInfoImpl puInfo) {
        try {
            puInfo.getEmfRegistration().unregister();            
        }
        catch (IllegalStateException exc) {
            // ignore
        }
        puInfo.setEmfRegistration(null);
        unboundPersistenceUnits.add(puInfo);
    }

    public synchronized void removePersistenceProvider(
        ServiceReference<PersistenceProvider> persistenceProvider) {
        log.debug("removing persistence provider {}", persistenceProvider.getClass().getName());
        scanBoundPersistenceUnits();

    }

    private void scanBoundPersistenceUnits() {
        for (PersistenceUnitInfoImpl puInfo : boundPersistenceUnits) {
            if (!isSatisfied(puInfo)) {
                deactivatePersistenceUnit(puInfo);
            }
        }
        boundPersistenceUnits.removeAll(unboundPersistenceUnits);
    }

    public synchronized void addDataSourceFactory(ServiceReference<DataSourceFactory> dsf) {
        log.debug("adding data source factory {}", dsf.getProperty("osgi.jdbc.driver.class"));
        dataSourceFactories.add(dsf);
        scanUnboundPersistenceUnits();

    }

    public synchronized void removeDataSourceFactory(ServiceReference<DataSourceFactory> dsf) {
        log.debug("removing data source factory {}", dsf.getProperty("osgi.jdbc.driver.class"));
        dataSourceFactories.remove(dsf);
        scanBoundPersistenceUnits();
    }

    @Override
    public synchronized void addingEntries(Bundle bundle, List<ManifestEntry> entries) {
        log.info("discovered persistence bundle {} {}", bundle.getSymbolicName(),
            bundle.getVersion());
        ManifestEntry entry = entries.get(0);
        String value = entry.getValue();
        String[] resources = value.split(",");
        for (String resource : resources) {
            processPersistenceDescriptor(bundle, resource);
        }
        scanUnboundPersistenceUnits();
    }

    private void processPersistenceDescriptor(Bundle bundle, String resource) {
        URL persistenceXml = bundle.getEntry(resource);
        try {
            Persistence descriptor = parser.parseDescriptor(persistenceXml);
            for (PersistenceUnit persistenceUnit : descriptor.getPersistenceUnit()) {
                processPersistenceUnit(bundle, persistenceUnit);
            }
        }
        catch (JAXBException exc) {
            log.error("cannot parse persistence descriptor", exc);
        }
    }

    private void processPersistenceUnit(Bundle bundle, PersistenceUnit persistenceUnit) {
        String puName = persistenceUnit.getName();
        log.info("processing persistence unit {}", puName);

        Properties puProps = parser.parseProperties(persistenceUnit);
        String driver = puProps.getProperty(JpaConstants.JPA_DRIVER);

        Map<String, String> dsfProps = new HashMap<String, String>();
        dsfProps.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, driver);

        PersistenceUnitInfoImpl puInfo = new PersistenceUnitInfoImpl(bundle, persistenceUnit,
            puProps);
        unboundPersistenceUnits.add(puInfo);

    }

    @Override
    public synchronized void removingEntries(Bundle bundle, List<ManifestEntry> entries) {
        log.info("removed persistence bundle {} {}", bundle.getSymbolicName(), bundle.getVersion());
    }
}
