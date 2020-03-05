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

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

public class EntityManagerFactoryService implements EntityManagerFactory {

	private EntityManagerFactory delegate;

	public EntityManager createEntityManager() {
		return delegate.createEntityManager();
	}

	public EntityManager createEntityManager(@SuppressWarnings("rawtypes") Map map) {
		return delegate.createEntityManager(map);
	}

	public CriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	public Metamodel getMetamodel() {
		return delegate.getMetamodel();
	}

	public boolean isOpen() {
		return delegate.isOpen();
	}

	public void close() {
		// spec-ref 127.4.9 Entity Manager Factory Life Cycle : calls to the
		// close method of the EntityManagerFactory registered in the service
		// registry must not close the Entity Manager Factory
	}

	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	public Cache getCache() {
		return delegate.getCache();
	}

	public PersistenceUnitUtil getPersistenceUnitUtil() {
		return delegate.getPersistenceUnitUtil();
	}

	public EntityManagerFactoryService(EntityManagerFactory delegate) {
		this.delegate = delegate;
	}

}
