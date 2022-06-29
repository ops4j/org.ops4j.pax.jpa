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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;

import org.ops4j.pax.jpa.impl.descriptor.DataSourceFactoryDescriptor;
import org.ops4j.pax.jpa.impl.descriptor.OSGiPersistenceUnitInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceUnitEntityManagerFactory implements ServiceTrackerCustomizer<PersistenceUnitInfo, PersistenceUnitEntityManagerFactory.EntityManagerFactoryContainer> {

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceUnitEntityManagerFactory.class);
	private final BundleContext bundleContext;
	private final PersistenceBundle persistenceBundle;

	static final class EntityManagerFactoryContainer implements PersistenceBundleService<EntityManagerFactory> {

		private final Map<String, Object> serviceProps = new HashMap<>();
		private EntityManagerFactory managerFactory;
		private final OSGiPersistenceUnitInfo persistenceUnit;
		private final String version;

		public EntityManagerFactoryContainer(OSGiPersistenceUnitInfo persistenceUnit, String version) {

			this.persistenceUnit = persistenceUnit;
			this.version = version;
		}

		private boolean update() {

			PersistenceProvider persistenceProvider = persistenceUnit.getPersistenceProvider();
			if(persistenceProvider == null || persistenceUnit.getDataSource() == null) {
				return false;
			}
			try {
				managerFactory = persistenceProvider.createContainerEntityManagerFactory(persistenceUnit, Map.of());
				serviceProps.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, persistenceUnit.getPersistenceUnitName());
				serviceProps.put(EntityManagerFactoryBuilder.JPA_UNIT_VERSION, version);
				serviceProps.put(EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER, persistenceProvider.getClass().getName());
				DataSourceFactoryDescriptor dataSourceFactoryDescriptor = persistenceUnit.getDataSourceFactoryDescriptor();
				if(dataSourceFactoryDescriptor != null) {
					dataSourceFactoryDescriptor.update(serviceProps);
				}
			} catch(RuntimeException e) {
				LOG.error("Creation of EntityManagerFactory failed for persistence unit {}", persistenceUnit.getName(), e);
				return false;
			}
			return true;
		}

		@Override
		public Class<EntityManagerFactory> getType() {

			return EntityManagerFactory.class;
		}

		@Override
		public EntityManagerFactory getService() {

			return new EntityManagerFactoryService(managerFactory);
		}

		@Override
		public Map<String, ?> getProperties() {

			return serviceProps;
		}

		@Override
		public void dispose() {

			if(managerFactory != null) {
				try {
					managerFactory.close();
				} catch(RuntimeException e) {
					// ignore;
				}
				managerFactory = null;
			}
			serviceProps.clear();
		}
	}

	public PersistenceUnitEntityManagerFactory(BundleContext bundleContext, PersistenceBundle persistenceBundle) {

		this.bundleContext = bundleContext;
		this.persistenceBundle = persistenceBundle;
	}

	@Override
	public EntityManagerFactoryContainer addingService(ServiceReference<PersistenceUnitInfo> reference) {

		if(reference.getBundle() == bundleContext.getBundle()) {
			PersistenceUnitInfo unitInfo = bundleContext.getService(reference);
			if(unitInfo instanceof OSGiPersistenceUnitInfo) {
				OSGiPersistenceUnitInfo impl = (OSGiPersistenceUnitInfo)unitInfo;
				EntityManagerFactoryContainer container = new EntityManagerFactoryContainer(impl, persistenceBundle.getBundle().getVersion().toString());
				if(container.update()) {
					persistenceBundle.addBundleService(container);
					return container;
				}
			}
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<PersistenceUnitInfo> reference, EntityManagerFactoryContainer container) {

		// something has changed, either the datasource or the provider, we must refresh
		persistenceBundle.removeBundleService(container);
		if(container.update()) {
			persistenceBundle.addBundleService(container);
		}
	}

	@Override
	public void removedService(ServiceReference<PersistenceUnitInfo> reference, EntityManagerFactoryContainer container) {

		try {
			persistenceBundle.removeBundleService(container);
		} finally {
			bundleContext.ungetService(reference);
		}
	}
}
