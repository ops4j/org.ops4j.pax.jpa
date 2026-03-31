/*
 * Copyright 2026 Christoph Läubrich.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.jpa.impl;

import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

/**
 * EntityManagerFactory wrapper returned from
 * {@link org.osgi.service.jpa.EntityManagerFactoryBuilder#createEntityManagerFactory(Map)}.
 * <p>
 * Closing this EMF also unregisters the corresponding EMF service from the
 * OSGi service registry (spec-ref §127.4.7).
 */
class BuilderEntityManagerFactory implements EntityManagerFactory {

	private final EntityManagerFactory delegate;
	private final EntityManagerFactoryBuilderImpl builder;

	BuilderEntityManagerFactory(EntityManagerFactory delegate, EntityManagerFactoryBuilderImpl builder) {
		this.delegate = delegate;
		this.builder = builder;
	}

	@Override
	public void close() {
		builder.unregisterEntityManagerFactory();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public EntityManager createEntityManager() {
		return delegate.createEntityManager();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public EntityManager createEntityManager(Map map) {
		return delegate.createEntityManager(map);
	}

	@Override
	public CriteriaBuilder getCriteriaBuilder() {
		return delegate.getCriteriaBuilder();
	}

	@Override
	public Metamodel getMetamodel() {
		return delegate.getMetamodel();
	}

	@Override
	public Map<String, Object> getProperties() {
		return delegate.getProperties();
	}

	@Override
	public Cache getCache() {
		return delegate.getCache();
	}

	@Override
	public PersistenceUnitUtil getPersistenceUnitUtil() {
		return delegate.getPersistenceUnitUtil();
	}
}
