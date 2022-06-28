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

import java.util.Collection;
import java.util.Collections;

import javax.persistence.spi.PersistenceProvider;

import org.ops4j.pax.jpa.JpaConstants;
import org.ops4j.pax.jpa.impl.PaxJPA;
import org.ops4j.pax.jpa.impl.PersistenceProviderBundle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link PersistenceProvider} services and transform them into
 * {@link PersistenceProviderBundle}
 * 
 * @author Christoph Läubrich
 *
 */
public class PersistenceProviderServiceTrackerCustomizer
		implements ServiceTrackerCustomizer<PersistenceProvider, PersistenceProviderBundle> {

	private static Logger LOG = LoggerFactory.getLogger(PersistenceProviderServiceTrackerCustomizer.class);
	private final BundleContext bundleContext;

	public PersistenceProviderServiceTrackerCustomizer(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public PersistenceProviderBundle addingService(ServiceReference<PersistenceProvider> reference) {
		PersistenceProvider persistenceProvider = bundleContext.getService(reference);
		if (persistenceProvider != null) {
			Bundle bundle = reference.getBundle();
			String property = (String) reference.getProperty(JpaConstants.JPA_PROVIDER);
			if (property == null) {
				property = persistenceProvider.getClass().getName();
			}
			LOG.info("discovered PersistenceProvider service {} (mapped to class {}) from bundle {} with properties {}",
					new Object[] { persistenceProvider.getClass().getName(), property, PaxJPA.getBundleName(bundle),
							PaxJPA.getServiceProperties(reference) });
			return new PersistenceProviderBundle(bundle, Collections.singletonMap(property, persistenceProvider));
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<PersistenceProvider> reference,
			PersistenceProviderBundle persistenceProviderBundle) {
		// not supported yet
	}

	@Override
	public void removedService(ServiceReference<PersistenceProvider> reference,
			PersistenceProviderBundle persistenceProviderBundle) {

		try {
			Collection<PersistenceProvider> providers = persistenceProviderBundle.getProviders();
			PersistenceProvider persistenceProvider = null;
			for (PersistenceProvider p : providers) {
				persistenceProvider = p;
			}
			persistenceProviderBundle.shutdown();
			if (persistenceProvider != null) {
				Bundle bundle = reference.getBundle();
				LOG.info("persistenceProvider service {} from bundle {}  with properties {} is no longer available",
						new Object[] { persistenceProviderBundle.getClass().getName(), PaxJPA.getBundleName(bundle),
								PaxJPA.getServiceProperties(reference) });
			}
		} finally {
			bundleContext.ungetService(reference);
		}
	}
}
