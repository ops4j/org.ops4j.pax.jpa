/*******************************************************************************
 * Copyright (c) 2020 Lablicate GmbH.
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
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.ops4j.pax.jpa.impl.tracker;

import static org.ops4j.pax.jpa.JpaConstants.JPA_MANIFEST_HEADER;
import static org.ops4j.pax.jpa.JpaConstants.JPA_PERSISTENCE_XML;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.persistence.spi.PersistenceUnitInfo;

import org.jcp.xmlns.xml.ns.persistence.Persistence;
import org.jcp.xmlns.xml.ns.persistence.Persistence.PersistenceUnit;
import org.ops4j.pax.jpa.impl.EntityManagerFactoryBuilderImpl;
import org.ops4j.pax.jpa.impl.JpaWeavingHook;
import org.ops4j.pax.jpa.impl.PaxJPA;
import org.ops4j.pax.jpa.impl.PersistenceBundle;
import org.ops4j.pax.jpa.impl.PersistenceProviderBundle;
import org.ops4j.pax.jpa.impl.PersistenceUnitDataSourceTracker;
import org.ops4j.pax.jpa.impl.PersistenceUnitPropertyManagedService;
import org.ops4j.pax.jpa.impl.PersistenceUnitProviderTracker;
import org.ops4j.pax.jpa.impl.PersitenceUnitManagedServiceFactory;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceDescriptorParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;

public class PersistenceBundleTrackerCustomizer implements BundleTrackerCustomizer<PersistenceBundle> {

	public static final int BUNDLE_TRACKING_STATE_MASK = Bundle.INSTALLED | Bundle.ACTIVE | Bundle.STARTING
			| Bundle.RESOLVED | Bundle.STOPPING;

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceBundleTrackerCustomizer.class);

	private final PersistenceDescriptorParser parser = new PersistenceDescriptorParser();

	private final BundleContext bundleContext;

	private final Iterable<PersistenceProviderBundle> persistenceProvider;

	private final BundleTracker<PersistenceProviderBundle> bundleTracker;

	public PersistenceBundleTrackerCustomizer(BundleContext bundleContext,
			Iterable<PersistenceProviderBundle> persistenceProvider, BundleTracker<PersistenceProviderBundle> bundleTracker) {
		this.bundleContext = bundleContext;
		this.persistenceProvider = persistenceProvider;
		this.bundleTracker = bundleTracker;
	}

	@Override
	public PersistenceBundle addingBundle(Bundle bundle, BundleEvent event) {

		Dictionary<String, String> headers = bundle.getHeaders("");
		String persistenceHeader = headers.get(JPA_MANIFEST_HEADER);
		if (persistenceHeader != null) {
			LOG.info("discovered persistence bundle {} ({} = {})",
					new Object[] { PaxJPA.getBundleName(bundle), JPA_MANIFEST_HEADER, persistenceHeader });
			final PersistenceBundle persistenceBundle = processPersistenceBundle(bundle, persistenceHeader);
			if (persistenceBundle != null) {
				persistenceBundle.stateChanged();
			}
			return persistenceBundle;
		} else {
			LOG.trace("ignore bundle {} no {} header found!", PaxJPA.getBundleName(bundle), JPA_MANIFEST_HEADER);
		}
		return null;
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, PersistenceBundle persistenceBundle) {
		LOG.info("persistence bundle {} is removed now", PaxJPA.getBundleName(bundle));
		persistenceBundle.shutdown();
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, PersistenceBundle persistenceBundle) {
		LOG.trace("persistence bundle {} changed state", PaxJPA.getBundleName(bundle));
		persistenceBundle.stateChanged();
	}

	private PersistenceBundle processPersistenceBundle(Bundle bundle, String persistenceHeader) {

		List<URL> resources = parseMetaPersistenceHeader(bundle, persistenceHeader);
		PersistenceBundle persistenceBundle = new PersistenceBundle(bundle);
		for (URL resource : resources) {
			try {
				Persistence descriptor = parser.parseDescriptor(resource);
				// TODO Spec-Ref 127.4.3 Processing : persistence provider must
				// validate all Persistence Descriptors against their schemas
				// TODO Spec-Ref 127.4.3 Processing : all entity classes
				// mentioned in the assigned Persistence Units must be on
				// Persistence Bundle's JAR
				if (descriptor != null) {
					outer: for (PersistenceUnit persistenceUnit : descriptor.getPersistenceUnit()) {
						LOG.debug("processing persistence unit {} in bundle {}", persistenceUnit.getName(),
								PaxJPA.getBundleName(bundle));
						EntityManagerFactoryBuilderImpl factory = new EntityManagerFactoryBuilderImpl(persistenceBundle,
								persistenceUnit, descriptor.getVersion(), bundleContext);
						PersistenceUnitPropertyManagedService persistenceUnitPropertyManagedService = new PersistenceUnitPropertyManagedService(
								factory, factory.getPersistenceProperties());
						PersitenceUnitManagedServiceFactory managedServiceFactory = new PersitenceUnitManagedServiceFactory(persistenceBundle, descriptor, persistenceUnit, bundleContext);
						String pid = "org.ops4j.pax.jpa.pu." + persistenceUnit.getName().replace(' ', '_');
						LOG.info("tracking configuration for persistence unit {} under pid {}, {}.",
								new Object[]{persistenceUnit.getName(), pid, PaxJPA.getPromotion(279)});
						LOG.info("tracking factory-configurations for persistence unit {} under factory.pid {} for multi-tenant support, {}.", new Object[]{persistenceUnit.getName(), pid, PaxJPA
								.getPromotion(280)});
						Dictionary<String, Object> serviceProperties = new Hashtable<>();
						serviceProperties.put(Constants.SERVICE_PID, pid);
						persistenceBundle.addServiceRegistration(bundleContext.registerService(ManagedService.class,
								persistenceUnitPropertyManagedService, serviceProperties));
						persistenceBundle.addServiceRegistration(bundleContext.registerService(ManagedServiceFactory.class, managedServiceFactory, serviceProperties));
						persistenceBundle.addServiceRegistration(
								bundleContext.registerService(WeavingHook.class, new JpaWeavingHook(bundle, persistenceUnit.getName(), new HashSet<>(persistenceUnit.getClazz()), persistenceBundle
										.getClassTransformers()), null));
						persistenceBundle.addTracker(new PersistenceUnitDataSourceTracker(bundleContext), PersistenceUnitInfo.class, bundleContext);
						persistenceBundle.addTracker(new PersistenceUnitProviderTracker(bundleContext, bundleTracker, persistenceBundle), PersistenceUnitInfo.class, bundleContext);
						for (PersistenceProviderBundle providerBundle : persistenceProvider) {
							if (providerBundle.assignTo(factory)) {
								persistenceBundle.addEntityManagerFactoryBuilder(factory);
								continue outer;
							}
						}
						LOG.warn(
								"persistence unit {} from bundle {} can't be assigned because no compatible PersistenceProvider is available",
								persistenceUnit.getName(), PaxJPA.getBundleName(bundle));
					}
				}
			} catch (JAXBException | IOException | SAXException exc) {
				// Spec-Ref 127.4.3 Processing : a bundle with parse errors in
				// any persistence descriptor must be ignored
				LOG.error("can not parse persistence descriptor {} from bundle {}, bundle is ignored",
						new Object[] { resource, PaxJPA.getBundleName(bundle), exc });
				persistenceBundle.shutdown();
				return null;
			}
		}
		if (persistenceBundle.isEmpty()) {
			// Spec-Ref 127.4.3 Processing : a bundle must have at least one
			// assigned Persistence Unit
			LOG.error("no assignable persistence units found in bundle {}, bundle is ignored",
					PaxJPA.getBundleName(bundle));
			persistenceBundle.shutdown();
			return null;
		}
		return persistenceBundle;
	}

	private static List<URL> parseMetaPersistenceHeader(Bundle bundle, String value) {

		URL defaultUrl = bundle.getEntry(JPA_PERSISTENCE_XML);
		boolean defaultUrlFound = false;
		List<URL> urls = new ArrayList<>();
		String[] parts = value.split(",\\s*");
		for (String part : parts) {
			String resource = part.trim();
			if (!resource.isEmpty()) {
				URL url = bundle.getEntry(resource);
				if (url != null) {
					urls.add(url);
					if (url.equals(defaultUrl)) {
						defaultUrlFound = true;
					}
				}
			}
		}
		if (defaultUrl != null && !defaultUrlFound) {
			urls.add(0, defaultUrl);
		}
		return urls;
	}
}
