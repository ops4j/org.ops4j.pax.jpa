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
package org.ops4j.pax.jpa.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.spi.PersistenceProvider;

import org.ops4j.pax.jpa.impl.tracker.PersistenceBundleTrackerCustomizer;
import org.ops4j.pax.jpa.impl.tracker.PersistenceProviderBundleTrackerCustomizer;
import org.ops4j.pax.jpa.impl.tracker.PersistenceProviderServiceTrackerCustomizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaxJPA implements BundleActivator {

	private static Logger LOG = LoggerFactory.getLogger(PaxJPA.class);
	private ServiceTracker<PersistenceProvider, PersistenceProviderBundle> peristenceProviderServiceTracker;
	private BundleTracker<PersistenceProviderBundle> peristenceProviderBundleTracker;
	private BundleTracker<PersistenceBundle> persistenceBundleTracker;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		LOG.info("Starting");
		peristenceProviderServiceTracker = new ServiceTracker<>(bundleContext, PersistenceProvider.class,
				new PersistenceProviderServiceTrackerCustomizer(bundleContext));
		peristenceProviderBundleTracker = new BundleTracker<>(bundleContext,
				PersistenceProviderBundleTrackerCustomizer.BUNDLE_TRACKING_STATE_MASK,
				new PersistenceProviderBundleTrackerCustomizer());
		Iterable<PersistenceProviderBundle> persistenceProvider = new Iterable<>() {

			@Override
			public Iterator<PersistenceProviderBundle> iterator() {
				// we add services, highest rank first so it is always possible
				// to override default choices
				List<PersistenceProviderBundle> list = new ArrayList<>(
						peristenceProviderServiceTracker.getTracked().values());
				// then we add all discovered (static) services found in bundles
				list.addAll(peristenceProviderBundleTracker.getTracked().values());
				// TODO should we discover PersistenceProviderResolver also?
				return list.iterator();
			}
		};
		persistenceBundleTracker = new BundleTracker<>(bundleContext,
				PersistenceBundleTrackerCustomizer.BUNDLE_TRACKING_STATE_MASK,
				new PersistenceBundleTrackerCustomizer(bundleContext, persistenceProvider));
		peristenceProviderServiceTracker.open();
		peristenceProviderBundleTracker.open();
		persistenceBundleTracker.open();
		LOG.info("Started");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("Stopping");
		persistenceBundleTracker.close();
		peristenceProviderBundleTracker.close();
		peristenceProviderServiceTracker.close();
		LOG.info("Stopped");
	}

	public static boolean isValid(ServiceRegistration<?> registration) {
		if (registration == null) {
			return false;
		}
		try {
			ServiceReference<?> reference = registration.getReference();
			return reference.getBundle() != null;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	public static Object getServiceProperties(ServiceReference<?> reference) {
		if (reference != null) {
			String[] propertyKeys = reference.getPropertyKeys();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < propertyKeys.length; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				String key = propertyKeys[i];
				sb.append(key);
				sb.append('=');
				if (key.startsWith(".")) {
					// properties with a dot should not be propagated/displayed
					// as they are marked as private
					sb.append("*************");
				} else {
					sb.append(reference.getProperty(key));
				}
			}
			return sb;
		}
		return "(null)";
	}

	public static Object getBundleName(Bundle bundle) {
		if (bundle != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(bundle.getSymbolicName());
			sb.append(" (version: ");
			sb.append(bundle.getVersion());
			sb.append(", id: ");
			sb.append(bundle.getBundleId());
			sb.append(", state: ");
			sb.append(getBundleState(bundle.getState()));
			sb.append(")");
			return sb;
		}
		return "(null)";
	}

	public static Object getBundleState(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		}
		return String.valueOf(state);
	}

	public static void unregister(ServiceRegistration<?> registration) {

		try {
			if (registration != null) {
				registration.unregister();
			}
		} catch (IllegalStateException e) {
			// already unregistered then...
		}
	}
	
	public static String getPromotion(int id) {

		return "this is non standard behavior, if you find this feature useful visit https://github.com/osgi/osgi/issues/" + id + " and vote or comment so this could be standardized in a future release of OSGi";
	}

}
