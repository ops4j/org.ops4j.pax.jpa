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

import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.persistence.spi.PersistenceUnitInfo;

import org.ops4j.pax.jpa.JpaConstants;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceUnitInfoImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link PersistenceUnitInfo}s of this bundle and corresponding {@link DataSourceFactory}s completing the {@link PersistenceUnitInfo} with a matching factory.
 *
 */
public class PersistenceUnitDataSourceTracker implements ServiceTrackerCustomizer<PersistenceUnitInfo, PersistenceUnitDataSourceTracker.PersistenceUnitInfoDataSource> {

	private static final Logger LOG = LoggerFactory.getLogger(PersistenceUnitDataSourceTracker.class);
	private final BundleContext bundleContext;

	public PersistenceUnitDataSourceTracker(BundleContext bundleContext) {

		this.bundleContext = bundleContext;
	}

	static final class PersistenceUnitInfoDataSource implements ServiceTrackerCustomizer<DataSourceFactory, DataSourceFactory> {

		private ServiceTracker<DataSourceFactory, DataSourceFactory> serviceTracker;
		private final BundleContext bundleContext;
		private final PersistenceUnitInfoImpl puInfo;
		private ServiceReference<DataSourceFactory> reference;

		public PersistenceUnitInfoDataSource(PersistenceUnitInfoImpl puInfo, BundleContext bundleContext) {

			this.puInfo = puInfo;
			this.bundleContext = bundleContext;
			String jndiDataSourceName = puInfo.getJndiDataSourceName();
			if(jndiDataSourceName != null) {
				// spec-ref : non standard behavior, acquire the desired DataSource
				// from the JNDI registry instead of the DataSourceFactory Service
				LOG.info("binding persistence unit {} to JNDI DataSource {}, {}.", new Object[]{puInfo.getPersistenceUnitName(), jndiDataSourceName, PaxJPA.getPromotion(455)});
				puInfo.setDataSourceFactory(new JndiDataSourceFactory(jndiDataSourceName));
			} else {
				String driver = puInfo.getProperties().getProperty(JpaConstants.JPA_DRIVER);
				if(driver == null || driver.isEmpty()) {
					LOG.info("No {} property specified, persistence unit {} is incomplete", new Object[]{JpaConstants.JPA_DRIVER, puInfo.getName()});
				} else {
					LOG.debug("Tracking DataSourceFactory {} specified by persistence unit {} ...", new Object[]{driver, puInfo.getName()});
					Filter filter;
					try {
						filter = bundleContext.createFilter("(&(objectClass=" + DataSourceFactory.class.getName() + ")(" + DataSourceFactory.OSGI_JDBC_DRIVER_CLASS + "=" + driver + "))");
					} catch(InvalidSyntaxException e) {
						throw new AssertionError("should never happen", e);
					}
					serviceTracker = new ServiceTracker<>(bundleContext, filter, this);
					serviceTracker.open();
				}
			}
		}

		public void dispose() {

			if(serviceTracker != null) {
				serviceTracker.close();
			}
		}

		@Override
		public DataSourceFactory addingService(ServiceReference<DataSourceFactory> reference) {

			DataSourceFactory dataSourceFactory = bundleContext.getService(reference);
			if(this.reference == null || this.reference.compareTo(reference) < 0) {
				bindFactory(reference, dataSourceFactory);
			}
			return dataSourceFactory;
		}

		private void bindFactory(ServiceReference<DataSourceFactory> reference, DataSourceFactory dataSourceFactory) {

			this.reference = reference;
			LOG.info("Bind DataSourceFactory {}(of type {}) to persistence unit {}...", new Object[]{reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS), dataSourceFactory.getClass()
					.getName(), puInfo.getName()});
			puInfo.setDataSourceFactory(dataSourceFactory);
		}

		@Override
		public void modifiedService(ServiceReference<DataSourceFactory> reference, DataSourceFactory service) {

			// we don't mind...
		}

		@Override
		public void removedService(ServiceReference<DataSourceFactory> reference, DataSourceFactory dataSourceFactory) {

			try {
				if(puInfo.getDataSourceFactory() == dataSourceFactory) {
					SortedMap<ServiceReference<DataSourceFactory>, DataSourceFactory> tracked = serviceTracker.getTracked();
					Set<Entry<ServiceReference<DataSourceFactory>, DataSourceFactory>> entrySet = tracked.entrySet();
					if(entrySet.isEmpty()) {
						LOG.info("Unbind DataSourceFactory {}(of type {}) from persistence unit {}...", new Object[]{reference
								.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS), dataSourceFactory.getClass().getName(), puInfo.getName()});
					} else {
						for(Entry<ServiceReference<DataSourceFactory>, DataSourceFactory> entry : entrySet) {
							bindFactory(entry.getKey(), entry.getValue());
							break;
						}
					}
				}
			} finally {
				bundleContext.ungetService(reference);
			}
		}
	}

	@Override
	public PersistenceUnitInfoDataSource addingService(ServiceReference<PersistenceUnitInfo> reference) {

		if(reference.getBundle() == bundleContext.getBundle()) {
			PersistenceUnitInfo unitInfo = bundleContext.getService(reference);
			if(unitInfo instanceof PersistenceUnitInfoImpl) {
				PersistenceUnitInfoImpl impl = (PersistenceUnitInfoImpl)unitInfo;
				return new PersistenceUnitInfoDataSource(impl, bundleContext);
			}
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<PersistenceUnitInfo> reference, PersistenceUnitInfoDataSource service) {

		// nothing to do here
	}

	@Override
	public void removedService(ServiceReference<PersistenceUnitInfo> reference, PersistenceUnitInfoDataSource service) {

		service.dispose();
		bundleContext.ungetService(reference);
	}
}
