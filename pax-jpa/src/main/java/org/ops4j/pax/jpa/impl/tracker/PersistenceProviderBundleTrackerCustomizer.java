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

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.persistence.spi.PersistenceProvider;

import org.ops4j.pax.jpa.JpaConstants;
import org.ops4j.pax.jpa.impl.PaxJPA;
import org.ops4j.pax.jpa.impl.PersistenceProviderBundle;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks bundles that contain
 * META-INF/services/javax.persistence.spi.PersistenceProvider
 * 
 * @author Christoph Läubrich
 *
 */
public class PersistenceProviderBundleTrackerCustomizer implements BundleTrackerCustomizer<PersistenceProviderBundle> {

	public static final int BUNDLE_TRACKING_STATE_MASK = Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED;
	private static Logger LOG = LoggerFactory.getLogger(PersistenceProviderBundleTrackerCustomizer.class);

	@Override
	public PersistenceProviderBundle addingBundle(Bundle bundle, BundleEvent event) {

		URL entry = bundle.getEntry(JpaConstants.META_INF_SERVICES_PERSISTENCE_PROVIDER);
		if (entry != null) {
			Object bundleName = PaxJPA.getBundleName(bundle);
			LOG.info("discovered {} in bundle {}", JpaConstants.META_INF_SERVICES_PERSISTENCE_PROVIDER, bundleName);
			BundleClassLoader classLoader = new BundleClassLoader(bundle,
					PersistenceProviderBundle.class.getClassLoader());
			ServiceLoader<PersistenceProvider> serviceLoader = ServiceLoader.load(PersistenceProvider.class,
					classLoader);
			Iterator<PersistenceProvider> iterator = serviceLoader.iterator();
			Map<String, PersistenceProvider> list = new LinkedHashMap<>();
			while (iterator.hasNext()) {
				PersistenceProvider persistenceProvider = iterator.next();
				LOG.debug("loaded PersistenceProvider {} from bundle {}", persistenceProvider.getClass().getName(), bundleName);
				list.put(persistenceProvider.getClass().getName(), persistenceProvider);
			}
			if (!list.isEmpty()) {
				return new PersistenceProviderBundle(bundle, list);
			}
		}
		return null;
	}

	@Override
	public void modifiedBundle(Bundle bundle, BundleEvent event, PersistenceProviderBundle providerBundle) {

		// nothing to do here
		providerBundle.update();
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, PersistenceProviderBundle providerBundle) {
		LOG.info("{} from bundle {} is no longer avaiable", JpaConstants.META_INF_SERVICES_PERSISTENCE_PROVIDER, PaxJPA.getBundleName(bundle));
		providerBundle.shutdown();
	}
}
