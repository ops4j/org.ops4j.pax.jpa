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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.spi.ClassTransformer;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persistence bundle is a bundle that is discovered by pax-jpa to contain
 * persistence descriptors
 * 
 * @author Christoph Läubrich
 *
 */
public class PersistenceBundle {

	private static Logger LOG = LoggerFactory.getLogger(PersistenceProviderBundle.class);
	private final Bundle bundle;
	private final boolean lazy;
	private final List<EntityManagerFactoryBuilderImpl> factories = new ArrayList<>(1);
	private final List<ServiceRegistration<?>> registrations = new ArrayList<>();
	private final List<ServiceTracker<?, ?>> trackers = new ArrayList<>();
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final AtomicBoolean refreshing = new AtomicBoolean();
	private BundleClassLoader classLoader;
	private final Set<ClassTransformer> classTransformers = new CopyOnWriteArraySet<>();

	public PersistenceBundle(Bundle bundle) {

		this.bundle = bundle;
		lazy = Constants.ACTIVATION_LAZY.equalsIgnoreCase(bundle.getHeaders("").get(Constants.BUNDLE_ACTIVATIONPOLICY));
	}

	public void shutdown() {

		if(shutdown.compareAndSet(false, true)) {
			LOG.info("Shutdown persistence bundle {}", PaxJPA.getBundleName(bundle));
			for(ServiceTracker<?, ?> tracker : trackers) {
				tracker.close();
			}
			trackers.clear();
			for(ServiceRegistration<?> registration : registrations) {
				PaxJPA.unregister(registration);
			}
			registrations.clear();
			for(EntityManagerFactoryBuilderImpl factory : factories) {
				factory.assignProvider(null);
			}
			factories.clear();
		}
	}

	public void stateChanged() {

		if(shutdown.get() || refreshing.get()) {
			return;
		}
		if(isReady()) {
			for(EntityManagerFactoryBuilderImpl factory : factories) {
				factory.activate();
			}
		} else {
			for(EntityManagerFactoryBuilderImpl factory : factories) {
				factory.deactivate();
			}
		}
	}

	public boolean isReady() {

		// spec-ref 127.4.4 Ready Phase : A Persistence Bundle is ready when its
		// state is ACTIVE or, when a lazy activation policy is used, STARTING
		int state = bundle.getState();
		return state == Bundle.ACTIVE || (lazy && state == Bundle.STARTING);
	}

	public synchronized BundleClassLoader getClassLoader() {

		if(classLoader == null) {
			// TODO we might check the state of the bundle here?
			classLoader = new BundleClassLoader(bundle, PersistenceBundle.class.getClassLoader());
		}
		return classLoader;
	}

	public void addServiceRegistration(ServiceRegistration<?> registration) {

		registrations.add(registration);
	}

	public <S, T> ServiceTracker<S, T> addTracker(ServiceTrackerCustomizer<S, T> customizer, Class<S> service, BundleContext context) {

		ServiceTracker<S, T> tracker = new ServiceTracker<>(context, service, customizer);
		tracker.open();
		return tracker;
	}

	public void addEntityManagerFactoryBuilder(EntityManagerFactoryBuilderImpl factory) {

		factories.add(factory);
	}

	public boolean isEmpty() {

		return factories.isEmpty();
	}

	public Bundle getBundle() {

		return bundle;
	}

	public Set<ClassTransformer> getClassTransformers() {

		return classTransformers;
	}

	public void refresh() throws InterruptedException {

		BundleContext bundleContext = bundle.getBundleContext();
		if(bundleContext == null) {
			// not resolved, so no need to refresh
			LOG.trace("bundle {} has no BundleContext, refresh not necessary", PaxJPA.getBundleName(bundle));
			return;
		}
		if(refreshing.compareAndSet(false, true)) {
			FrameworkWiring frameworkWiring = bundleContext.getBundle(0).adapt(FrameworkWiring.class);
			final CountDownLatch latch = new CountDownLatch(1);
			LOG.debug("starting refresh of bundle {}", PaxJPA.getBundleName(bundle));
			frameworkWiring.refreshBundles(Collections.singleton(bundle), new FrameworkListener() {

				@Override
				public void frameworkEvent(FrameworkEvent event) {

					latch.countDown();
				}
			});
			LOG.debug("waiting for package refresh of bundle {} ...", PaxJPA.getBundleName(bundle));
			latch.await();
			refreshing.set(false);
			LOG.debug("... packages for bundle {} refreshed", PaxJPA.getBundleName(bundle));
			stateChanged();
		}
	}
}
