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

import org.osgi.framework.Bundle;
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

	private static Logger LOG = LoggerFactory.getLogger(PersistenceProviderBundle.class);

	private Map<String, PersistenceProvider> providers;

	private Bundle bundle;

	private List<EntityManagerFactoryBuilderImpl> assignedFactories = new ArrayList<>();

	public PersistenceProviderBundle(Bundle bundle, Map<String, PersistenceProvider> provider) {
		this.bundle = bundle;
		this.providers = new LinkedHashMap<>(provider);
	}

	public synchronized void shutdown() {
		for (EntityManagerFactoryBuilderImpl factoryBuilderImpl : assignedFactories) {
			factoryBuilderImpl.assignProvider(null);
		}
		assignedFactories.clear();
		providers.clear();
	}

	public synchronized boolean assignTo(EntityManagerFactoryBuilderImpl factoryBuilder) {

		String providerClassName = factoryBuilder.getPersistenceProviderClassName();
		for (Entry<String, PersistenceProvider> entry : providers.entrySet()) {
			PersistenceProvider persistenceProvider = entry.getValue();
			if (providerClassName == null || providerClassName.equals(entry.getKey())
					|| providerClassName.equals(persistenceProvider.getClass().getName())) {
				LOG.info("assign persistence provider {} from bundle {} to persistence unit {}",
						new Object[] { persistenceProvider.getClass().getName(), PaxJPA.getBundleName(bundle),
								factoryBuilder.getPersistenceUnitInfo().getPersistenceUnitName() });
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
}
