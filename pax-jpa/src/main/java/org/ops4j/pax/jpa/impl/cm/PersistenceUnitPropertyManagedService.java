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
package org.ops4j.pax.jpa.impl.cm;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.ops4j.pax.jpa.impl.EntityManagerFactoryBuilderImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class PersistenceUnitPropertyManagedService implements ManagedService{

	private Properties persitenceProperties;
	private EntityManagerFactoryBuilderImpl factory;

	public PersistenceUnitPropertyManagedService(EntityManagerFactoryBuilderImpl factory, Properties properties) {
		this.factory = factory;
		this.persitenceProperties = properties;
	}

	@Override
	public void updated(Dictionary<String, ?> configurationProperties) throws ConfigurationException {
		persitenceProperties.clear();
		if (configurationProperties != null) {
			Enumeration<?> keys = configurationProperties.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				Object obj = configurationProperties.get(key);
				if (obj != null) {
					persitenceProperties.setProperty(String.valueOf(key), String.valueOf(obj));
				}
			}
		}
		factory.propertiesChanged();
	}
}
