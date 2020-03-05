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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ops4j.pax.jpa.impl.EntityManagerFactoryBuilderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class DataSourceFactoryServiceTracker
		implements ServiceTrackerCustomizer<DataSourceFactory, DataSourceFactory> {

	private String driver;
	private BundleContext bundleContext;
	private DataSourceFactory boundFactory;
	private List<DataSourceFactory> factories = new CopyOnWriteArrayList<>();
	private ServiceTracker<DataSourceFactory, DataSourceFactory> serviceTracker;
	private EntityManagerFactoryBuilderImpl builder;

	public DataSourceFactoryServiceTracker(String driver, BundleContext bundleContext,
			EntityManagerFactoryBuilderImpl builder) {
		this.driver = driver;
		this.bundleContext = bundleContext;
		this.builder = builder;
		Filter filter;
		try {
			filter = bundleContext.createFilter(
					"(&(objectClass=org.osgi.service.jdbc.DataSourceFactory)(osgi.jdbc.driver.class=" + driver + "))");
		} catch (InvalidSyntaxException e) {
			throw new AssertionError("should never happen", e);
		}
		serviceTracker = new ServiceTracker<>(bundleContext, filter, this);
	}

	@Override
	public DataSourceFactory addingService(ServiceReference<DataSourceFactory> reference) {

		DataSourceFactory service = bundleContext.getService(reference);
		if (service != null) {
			synchronized (this) {
				builder.bindDataSource(service);
				boundFactory = service;
			}
			factories.add(service);
		}
		return service;
	}

	@Override
	public void modifiedService(ServiceReference<DataSourceFactory> reference, DataSourceFactory service) {

		// don't mind
	}

	@Override
	public void removedService(ServiceReference<DataSourceFactory> reference, DataSourceFactory oldService) {

		try {
			factories.remove(oldService);
			synchronized (this) {
				if (boundFactory == oldService) {
					for (DataSourceFactory dataSourceFactory : factories) {
						builder.bindDataSource(dataSourceFactory);
						boundFactory = dataSourceFactory;
						return;
					}
					boundFactory = null;
					builder.bindDataSource(null);
				}
			}
		} finally {
			bundleContext.ungetService(reference);
		}
	}

	public String getDriver() {

		return driver;
	}

	public void open() {
		serviceTracker.open();
	}

	public void close() {
		synchronized (this) {
			if (boundFactory != null) {
				builder.bindDataSource(null);
				boundFactory = null;
			}
		}
		serviceTracker.close();
	}
}
