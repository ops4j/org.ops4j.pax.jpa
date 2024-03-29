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
package org.ops4j.pax.jpa.impl.descriptor;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;

public class DataSourceFactoryDescriptor {

	private final Object driverClass;
	private final Object driverName;
	private final Object driverVersion;

	public DataSourceFactoryDescriptor(ServiceReference<DataSourceFactory> reference) {

		driverClass = reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS);
		driverName = reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
		driverVersion = reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION);
	}

	public void update(Map<String, Object> properties) {

		if(driverClass == null) {
			properties.remove(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS);
		} else {
			properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, driverClass);
		}
		if(driverName == null) {
			properties.remove(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
		} else {
			properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, driverName);
		}
		if(driverVersion == null) {
			properties.remove(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION);
		} else {
			properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION, driverVersion);
		}
	}

	public DataSourceFactoryDescriptor(Object driverClass, Object driverName, Object driverVersion) {

		this.driverClass = driverClass;
		this.driverName = driverName;
		this.driverVersion = driverVersion;
	}
}
