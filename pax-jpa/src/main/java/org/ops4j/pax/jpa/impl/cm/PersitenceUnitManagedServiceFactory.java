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
package org.ops4j.pax.jpa.impl.cm;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.jcp.xmlns.xml.ns.persistence.Persistence;
import org.jcp.xmlns.xml.ns.persistence.Persistence.PersistenceUnit;
import org.ops4j.pax.jpa.impl.PaxJPA;
import org.ops4j.pax.jpa.impl.PersistenceBundle;
import org.ops4j.pax.jpa.impl.descriptor.DataSourceFactoryDescriptor;
import org.ops4j.pax.jpa.impl.descriptor.OSGiPersistenceUnitInfo;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceDescriptorParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.jdbc.DataSourceFactory;

/**
 * 
 * The {@link PersitenceUnitManagedServiceFactory} reads factory configurations of a persistence unit and create {@link PersistenceUnitInfo} services from them, service properties are used to track
 * the state of the {@link PersistenceUnitInfo}.
 */
public class PersitenceUnitManagedServiceFactory implements ManagedServiceFactory {

	private final Map<String, FactoryPersistenceUnitInfo> factoryUnitMap = new ConcurrentHashMap<>();
	private final PersistenceUnit persistenceUnit;
	private final PersistenceBundle persistenceBundle;
	private final Persistence descriptor;
	private final Properties baseProperties;
	private final BundleContext bundleContext;

	public PersitenceUnitManagedServiceFactory(PersistenceBundle persistenceBundle, Persistence descriptor, PersistenceUnit persistenceUnit, BundleContext bundleContext) {

		this.persistenceBundle = persistenceBundle;
		this.descriptor = descriptor;
		this.persistenceUnit = persistenceUnit;
		this.bundleContext = bundleContext;
		this.baseProperties = PersistenceDescriptorParser.parseProperties(persistenceUnit);
	}

	@Override
	public String getName() {

		return "Factory for persistence unit " + persistenceUnit.getName();
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {

		FactoryPersistenceUnitInfo unitInfo = factoryUnitMap.compute(pid, (fpid, oldInfo) -> {
			if(oldInfo != null) {
				oldInfo.dispose();
			}
			Properties mergedProperties = new Properties(baseProperties);
			Enumeration<String> keys = properties.keys();
			while(keys.hasMoreElements()) {
				String key = keys.nextElement();
				if(key.startsWith(".") || Constants.SERVICE_PID.equals(key) || "service.factoryPid".equals(key)) {
					continue;
				}
				mergedProperties.setProperty(key, String.valueOf(properties.get(key)));
			}
			return new FactoryPersistenceUnitInfo(fpid, persistenceBundle, descriptor.getVersion(), persistenceUnit, mergedProperties);
		});
		ServiceRegistration<PersistenceUnitInfo> registration = bundleContext.registerService(PersistenceUnitInfo.class, unitInfo, null);
		persistenceBundle.addServiceRegistration(registration);
		unitInfo.init(registration);
	}

	@Override
	public void deleted(String pid) {

		FactoryPersistenceUnitInfo unitInfo = factoryUnitMap.remove(pid);
		if(unitInfo != null) {
			unitInfo.dispose();
		}
	}

	private static final class FactoryPersistenceUnitInfo extends OSGiPersistenceUnitInfo {

		private ServiceRegistration<PersistenceUnitInfo> registration;
		private final String pid;

		public FactoryPersistenceUnitInfo(String pid, PersistenceBundle bundle, String version, PersistenceUnit persistenceUnit, Properties props) {

			super(bundle, version, persistenceUnit, props);
			this.pid = pid;
		}

		void init(ServiceRegistration<PersistenceUnitInfo> registration) {

			this.registration = registration;
			update();
		}

		private void update() {

			if(registration == null) {
				return;
			}
			Map<String, Object> properties = new HashMap<>();
			if(dataSource != null) {
				properties.put(SERVICE_PROPERTY_DATA_SOURCE, dataSource.getClass().getName());
			}
			if(dataSourceFactory != null) {
				properties.put(SERVICE_PROPERTY_DATA_SOURCE_FACTORY, dataSourceFactory.getClass().getName());
				if(dataSourceFactoryDescriptor != null) {
					dataSourceFactoryDescriptor.update(properties);
				}
			}
			if(provider != null) {
				properties.put(SERVICE_PROPERTY_PERSISTENCE_PROVIDER, provider.getClass().getName());
			}
			registration.setProperties(FrameworkUtil.asDictionary(properties));
		}

		void dispose() {

			PaxJPA.unregister(registration);
		}

		@Override
		public void setDataSource(DataSource dataSource) {

			super.setDataSource(dataSource);
			update();
		}

		@Override
		public void setProvider(PersistenceProvider provider) {

			super.setProvider(provider);
			update();
		}

		@Override
		public void setDataSourceFactory(DataSourceFactory dataSourceFactory, DataSourceFactoryDescriptor descriptor) {

			super.setDataSourceFactory(dataSourceFactory, descriptor);
			update();
		}

		@Override
		public String getName() {

			return super.getName() + " (" + pid + ")";
		}
	}
}
