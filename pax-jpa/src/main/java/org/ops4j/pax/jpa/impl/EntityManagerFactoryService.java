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
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;

public class EntityManagerFactoryService implements EntityManagerFactory {

	private final EntityManagerFactory delegate;

	@Override
	public EntityManager createEntityManager() {
		return delegate.createEntityManager();
	}

	@Override
	public EntityManager createEntityManager(@SuppressWarnings("rawtypes") Map map) {
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
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public void close() {
		// spec-ref 127.4.9 Entity Manager Factory Life Cycle : calls to the
		// close method of the EntityManagerFactory registered in the service
		// registry must not close the Entity Manager Factory
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

	public EntityManagerFactoryService(EntityManagerFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType) {

		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {

		return delegate.createEntityManager(synchronizationType, map);
	}

	@Override
	public void addNamedQuery(String name, Query query) {

		delegate.addNamedQuery(name, query);
	}

	@Override
	public <T> T unwrap(Class<T> cls) {

		return delegate.unwrap(cls);
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {

		delegate.addNamedEntityGraph(graphName, entityGraph);
	}
}
