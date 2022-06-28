/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich.
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
package org.ops4j.pax.jpa.impl;

import java.util.Objects;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.ops4j.pax.jpa.JpaConstants;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceUnitInfoImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * tracks {@link PersistenceUnitInfo}s of this bundle and assigns {@link PersistenceProvider}s to them
 */
public class PersistenceUnitProviderTracker implements ServiceTrackerCustomizer<PersistenceUnitInfo, PersistenceUnitProviderTracker.PersistenceUnitInfoProvider> {

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceUnitProviderTracker.class);
	private final BundleContext bundleContext;
	private final BundleTracker<PersistenceProviderBundle> bundleTracker;
	private final PersistenceBundle persistenceBundle;

	final static class PersistenceUnitInfoProvider implements ServiceTrackerCustomizer<PersistenceProvider, PersistenceProvider> {

		private final PersistenceUnitInfoImpl puInfo;
		private final ServiceTracker<PersistenceProvider, PersistenceProvider> serviceTracker;
		private final BundleContext bundleContext;
		private ServiceReference<PersistenceProvider> reference;
		private final BundleTracker<PersistenceProviderBundle> bundleTracker;
		private final PersistenceBundle bundle;

		public PersistenceUnitInfoProvider(PersistenceUnitInfoImpl puInfo, PersistenceBundle bundle, BundleContext bundleContext, BundleTracker<PersistenceProviderBundle> bundleTracker) {

			this.puInfo = puInfo;
			this.bundle = bundle;
			this.bundleContext = bundleContext;
			this.bundleTracker = bundleTracker;
			serviceTracker = new ServiceTracker<>(bundleContext, PersistenceProvider.class, this);
			serviceTracker.open();
			// TODO replace with Aries SPIFly!
			if(puInfo.getPersistenceProvider() == null) {
				for(PersistenceProviderBundle pb : bundleTracker.getTracked().values()) {
					for(PersistenceProvider provider : pb.getProviders()) {
						if(assign(provider, null)) {
							return;
						}
					}
				}
				LOG.warn("persistence unit {} from bundle {} can't be assigned because no compatible PersistenceProvider is available", puInfo.getName(), PaxJPA.getBundleName(bundle.getBundle()));
			}
		}

		@Override
		public PersistenceProvider addingService(ServiceReference<PersistenceProvider> reference) {

			PersistenceProvider persistenceProvider = bundleContext.getService(reference);
			if(this.reference == null || this.reference.compareTo(reference) < 0) {
				if(assign(persistenceProvider, (String)reference.getProperty(JpaConstants.JPA_PROVIDER))) {
					this.reference = reference;
				}
			}
			return persistenceProvider;
		}

		@Override
		public void modifiedService(ServiceReference<PersistenceProvider> reference, PersistenceProvider service) {

			// never mind
		}

		@Override
		public void removedService(ServiceReference<PersistenceProvider> reference, PersistenceProvider persistenceProvider) {

			try {
				if(puInfo.getPersistenceProvider() == persistenceProvider) {
					for(PersistenceProvider provider : serviceTracker.getTracked().values()) {
						if(assign(provider, (String)reference.getProperty(JpaConstants.JPA_PROVIDER))) {
							return;
						}
					}
					for(PersistenceProviderBundle pb : bundleTracker.getTracked().values()) {
						for(PersistenceProvider provider : pb.getProviders()) {
							if(assign(provider, null)) {
								return;
							}
						}
					}
					LOG.warn("persistence unit {} from bundle {} can't be assigned because no compatible PersistenceProvider is available", puInfo.getName(), PaxJPA.getBundleName(bundle.getBundle()));
					puInfo.setProvider(null);
				}
			} finally {
				bundleContext.ungetService(reference);
			}
		}

		private boolean assign(PersistenceProvider provider, String property) {

			String providerClassName = puInfo.getPersistenceProviderClassName();
			if(providerClassName == null || providerClassName.equals(property) || providerClassName.equals(provider.getClass().getName())) {
				LOG.info("Assign persistence provider {}(of type {}) to persistence unit {}", new Object[]{Objects.requireNonNullElse(property, provider.getClass().getName()), provider.getClass()
						.getName(), puInfo.getName()});
				puInfo.setProvider(provider);
				return true;
			}
			return false;
		}

		public void dispose() {

			serviceTracker.close();
		}
	}

	public PersistenceUnitProviderTracker(BundleContext bundleContext, BundleTracker<PersistenceProviderBundle> bundleTracker, PersistenceBundle persistenceBundle) {

		this.bundleContext = bundleContext;
		this.bundleTracker = bundleTracker;
		this.persistenceBundle = persistenceBundle;
	}

	@Override
	public PersistenceUnitInfoProvider addingService(ServiceReference<PersistenceUnitInfo> reference) {

		if(reference.getBundle() == bundleContext.getBundle()) {
			PersistenceUnitInfo unitInfo = bundleContext.getService(reference);
			if(unitInfo instanceof PersistenceUnitInfoImpl) {
				PersistenceUnitInfoImpl impl = (PersistenceUnitInfoImpl)unitInfo;
				return new PersistenceUnitInfoProvider(impl, persistenceBundle, bundleContext, bundleTracker);
			}
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<PersistenceUnitInfo> reference, PersistenceUnitInfoProvider service) {

		// we don't care
	}

	@Override
	public void removedService(ServiceReference<PersistenceUnitInfo> reference, PersistenceUnitInfoProvider persistenceUnitInfoProvider) {

		try {
			persistenceUnitInfoProvider.dispose();
		} finally {
			bundleContext.ungetService(reference);
		}
	}
}
