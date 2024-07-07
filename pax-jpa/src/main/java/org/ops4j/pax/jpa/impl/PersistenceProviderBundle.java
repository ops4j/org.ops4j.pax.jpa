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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.spi.PersistenceProvider;

import org.ops4j.pax.jpa.JpaConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persistence provider bundle is a bundle that provides provides one or more
 * {@link PersistenceProvider}s
 * 
 * @author Christoph Läubrich
 *
 */
public class PersistenceProviderBundle {

	// This is mostly to support the TCK that require this, but maybe also useful for others, see https://github.com/osgi/osgi/issues/734
	private static final boolean REGISTER_PROVIDER_AS_SERVICE = Boolean.getBoolean("pax.jpa.registerSpiProviderAsService");

	private static Logger LOG = LoggerFactory.getLogger(PersistenceProviderBundle.class);

	private final Map<String, PersistenceProvider> providers;

	private final Bundle bundle;

	private final List<EntityManagerFactoryBuilderImpl> assignedFactories = new ArrayList<>();
	private List<ServiceRegistration<PersistenceProvider>> registrations;

	public PersistenceProviderBundle(Bundle bundle, Map<String, PersistenceProvider> provider) {
		this.bundle = bundle;
		this.providers = new LinkedHashMap<>(provider);
		update();
	}

	public synchronized void shutdown() {
		if(registrations != null) {
			registrations.forEach(ServiceRegistration::unregister);
			registrations = null;
		}
		for(EntityManagerFactoryBuilderImpl factoryBuilderImpl : assignedFactories) {
			factoryBuilderImpl.assignProvider(null);
		}
		assignedFactories.clear();
		providers.clear();
	}

	public synchronized boolean assignTo(EntityManagerFactoryBuilderImpl factoryBuilder) {

		String providerClassName = factoryBuilder.getPersistenceProviderClassName();
		for(Entry<String, PersistenceProvider> entry : providers.entrySet()) {
			PersistenceProvider persistenceProvider = entry.getValue();
			if(providerClassName == null || providerClassName.equals(entry.getKey()) || providerClassName.equals(persistenceProvider.getClass().getName())) {
				LOG.info("assign persistence provider {} from bundle {} to persistence unit {}", new Object[]{persistenceProvider.getClass().getName(), PaxJPA
						.getBundleName(bundle), factoryBuilder.getPersistenceUnitInfo().getPersistenceUnitName()});
				assignedFactories.add(factoryBuilder);
				factoryBuilder.assignProvider(entry.getValue());
				return true;
			}
		}
		return false;
	}

	public Collection<PersistenceProvider> getProviders() {
		return Collections.unmodifiableCollection(providers.values());
	}

	public synchronized void update() {
		if(REGISTER_PROVIDER_AS_SERVICE) {
			BundleContext bundleContext = bundle.getBundleContext();
			if(bundleContext == null) {
				if(registrations != null) {
					registrations.forEach(ServiceRegistration::unregister);
					registrations = null;
				}
			} else if(registrations == null) {
				registrations = providers.values()
						.stream()
						.map(provider -> bundleContext
								.registerService(PersistenceProvider.class, provider, FrameworkUtil.asDictionary(Map.of(JpaConstants.JPA_PROVIDER, provider.getClass().getName()))))
						.toList();
			}
		}
	}
}
