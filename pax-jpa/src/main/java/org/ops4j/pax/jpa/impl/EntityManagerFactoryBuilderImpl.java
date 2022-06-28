/*
 * Copyright 2013 Harald Wellmann.
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

import java.sql.SQLException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.sql.DataSource;

import org.jcp.xmlns.xml.ns.persistence.Persistence.PersistenceUnit;
import org.ops4j.pax.jpa.JpaConstants;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceDescriptorParser;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceUnitInfoImpl;
import org.ops4j.pax.jpa.impl.tracker.DataSourceFactoryServiceTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the OSGi JPA {@link EntityManagerFactoryBuilder} service.
 * 
 * NOTE: This implementation only supports container managed entity manager
 * factories.
 * 
 * @author Harald Wellmann
 * @author Christoph Läubrich refactor to handle state changes and be more spec
 *         compliant
 *
 */
public class EntityManagerFactoryBuilderImpl implements EntityManagerFactoryBuilder {

	private static Logger LOG = LoggerFactory.getLogger(EntityManagerFactoryBuilderImpl.class);
	private final PersistenceUnitInfoImpl puInfo;
	private ServiceRegistration<EntityManagerFactoryBuilder> emfBuilderRegistration;
	private ServiceRegistration<EntityManagerFactory> emfRegistration;
	private PersistenceProvider persistenceProvider;
	private final BundleContext jpaBundleContext;
	private DataSourceFactoryServiceTracker dataSourceFactoryServiceTracker;
	private DataSourceFactory dataSourceFactory;
	private EntityManagerFactory entityManagerFactory;
	private final PersistenceBundle persistenceBundle;
	private final AtomicBoolean activating = new AtomicBoolean();
	private final Properties puProps;

	public EntityManagerFactoryBuilderImpl(PersistenceBundle persistenceBundle, PersistenceUnit persistenceUnit, String schemaVersion,
			BundleContext jpaBundleContext) {

		this.puProps = new Properties(PersistenceDescriptorParser.parseProperties(persistenceUnit));
		this.persistenceBundle = persistenceBundle;
		this.puInfo = new PersistenceUnitInfoImpl(persistenceBundle, schemaVersion, persistenceUnit, puProps);
		this.jpaBundleContext = jpaBundleContext;
	}

	@Override
	public synchronized EntityManagerFactory createEntityManagerFactory(Map<String, Object> props) {
		Properties userProperties = new Properties(puInfo.getProperties());
		for (Entry<String, Object> entry : props.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof String) {
				userProperties.setProperty(key, (String) value);
			}
		}
		// TODO we must check for rebinding/create a datasource/register as service!
		return persistenceProvider.createContainerEntityManagerFactory(puInfo, props);
	}

	public PersistenceBundle getPersistenceBundle() {
		return persistenceBundle;
	}

	public PersistenceUnitInfoImpl getPersistenceUnitInfo() {

		return puInfo;
	}

	public String getPersistenceProviderClassName() {

		return puInfo.getPersistenceProviderClassName();
	}

	public void assignProvider(PersistenceProvider persistenceProvider) {

		this.persistenceProvider = persistenceProvider;
		puInfo.setProvider(persistenceProvider);
		if (persistenceProvider == null) {
			closeTracker();
			deactivate();
		} else {
			propertiesChanged();
		}
	}

	private void closeTracker() {
		if (dataSourceFactoryServiceTracker != null) {
			dataSourceFactoryServiceTracker.close();
			dataSourceFactoryServiceTracker = null;
		}
	}

	public void activate() {

		if (persistenceProvider == null) {
			// unassigned factories can't be activated
			return;
		}
		if (activating.compareAndSet(false, true)) {
			registerEntityManagerFactory();
			registerEntityManagerFactoryBuilder();
			activating.set(false);
		}
	}

	public void deactivate() {
		if (activating.get()) {
			return;
		}
		unregisterEntityManagerFactory();
		PaxJPA.unregister(emfBuilderRegistration);
		emfBuilderRegistration = null;
	}

	public synchronized void propertiesChanged() {
		if (persistenceProvider == null) {
			// unassigned factories can't track
			return;
		}
		String jndiDataSourceName = puInfo.getJndiDataSourceName();
		if (jndiDataSourceName != null) {
			// spec-ref : non standard behavior, acquire the desired DataSource
			// from the JNDI registry instead of the DataSourceFactory Service
			if (dataSourceFactory instanceof JndiDataSourceFactory) {
				JndiDataSourceFactory jndi = (JndiDataSourceFactory) dataSourceFactory;
				if (jndiDataSourceName.equals(jndi.getJndiName())) {
					return;
				}
			}
			LOG.info("binding persistence unit {} to JNDI DataSource {}, {}.", new Object[]{puInfo.getPersistenceUnitName(),
					jndiDataSourceName, PaxJPA.getPromotion(455)});
			bindDataSource(new JndiDataSourceFactory(jndiDataSourceName));
		} else {
			String driver = puInfo.getProperties().getProperty(JpaConstants.JPA_DRIVER);
			if (driver == null || driver.isEmpty()) {
				LOG.info("no {} property specified persistence unit {} is incomplete",
						new Object[] { JpaConstants.JPA_DRIVER, puInfo.getPersistenceUnitName() });
				closeTracker();
			} else if (dataSourceFactoryServiceTracker == null
					|| !driver.equals(dataSourceFactoryServiceTracker.getDriver())) {
				closeTracker();
				LOG.debug("now tracking DataSourceFactory {} specified by persistence unit {} ...",
						new Object[] { driver, puInfo.getPersistenceUnitName() });
				dataSourceFactoryServiceTracker = new DataSourceFactoryServiceTracker(driver, jpaBundleContext, this);
				dataSourceFactoryServiceTracker.open();
			}
		}
		//TODO must refresh the whole EMF on property change!
	}

	private static DataSource createDataSource(DataSourceFactory dataSourceFactory, Properties persitenceProperties)
			throws SQLException {
		String url = persitenceProperties.getProperty(JpaConstants.JPA_URL);
		String user = persitenceProperties.getProperty(JpaConstants.JPA_USER);
		String password = persitenceProperties.getProperty(JpaConstants.JPA_PASSWORD);
		Properties dsfProps = new Properties();
		if (url != null) {
			dsfProps.setProperty(DataSourceFactory.JDBC_URL, url);
		}
		if (user != null) {
			dsfProps.setProperty(DataSourceFactory.JDBC_USER, user);
		}
		if (password != null) {
			dsfProps.setProperty(DataSourceFactory.JDBC_PASSWORD, password);
		}
		return dataSourceFactory.createDataSource(dsfProps);
	}

	public synchronized void bindDataSource(DataSourceFactory dataSourceFactory) {
		if (this.dataSourceFactory != dataSourceFactory) {
			this.dataSourceFactory = dataSourceFactory;
			unregisterEntityManagerFactory();
			puInfo.setDataSource(null);
			try {
				if (dataSourceFactory != null) {
					LOG.info("bind persistence unit {} to DataSourceFactory {}", puInfo.getPersistenceUnitName(), dataSourceFactory.getClass().getName());
					puInfo.setDataSource(createDataSource(dataSourceFactory, puInfo.getProperties()));
					registerEntityManagerFactory();
				} else {
					LOG.info("no DataSourceFactory available, persistence unit {} is incomplete", puInfo.getPersistenceUnitName());
				}
			} catch (SQLException e) {
				puInfo.setDataSource(null);
				LOG.error("can't bind datasource", e);
				return;
			}
		}
	}

	synchronized EntityManagerFactory createEntityManagerFactoryInternal(Map<String, ?> userProperties) {
		if (persistenceProvider == null) {
			throw new IllegalStateException("This factory is not assigned");
		}
		Properties persitenceProperties = puInfo.getProperties();
		// we must copy here to a map to take properties defaults into
		// account
		Map<String, Object> emfProps = new HashMap<>();
		for (String key : persitenceProperties.stringPropertyNames()) {
			emfProps.put(key, persitenceProperties.getProperty(key));
		}
		emfProps.putAll(userProperties);
		return persistenceProvider.createContainerEntityManagerFactory(puInfo, emfProps);
	}

	private void registerEntityManagerFactoryBuilder() {
		if (!PaxJPA.isValid(emfBuilderRegistration)) {
			// spec-ref 127.4.5 Service Registrations : The JPA Services must be
			// registered through the Bundle Context of the corresponding
			// Persistence Bundle
			Bundle bundle = persistenceBundle.getBundle();
			BundleContext bundleContext = bundle.getBundleContext();
			if (bundleContext != null) {
				LOG.info("register EntityManagerFactoryBuilder service for persistence unit {}...",
						puInfo.getPersistenceUnitName());
				Dictionary<String, String> emfBuilderServiceProps = new Hashtable<>();
				emfBuilderServiceProps.put(JPA_UNIT_NAME, puInfo.getPersistenceUnitName());
				emfBuilderServiceProps.put(JPA_UNIT_VERSION, bundle.getVersion().toString());
				emfBuilderServiceProps.put(JPA_UNIT_PROVIDER, persistenceProvider.getClass().getName());
				emfBuilderRegistration = bundleContext.registerService(EntityManagerFactoryBuilder.class, this,
						emfBuilderServiceProps);
			} else {
				LOG.info(
						"persistence bundle {} is not started yet, EntityManagerFactoryBuilder service registration for persistence unit {} is delayed...",
						PaxJPA.getBundleName(bundle), puInfo.getPersistenceUnitName());
			}
		}
	}

	private void registerEntityManagerFactory() {
		if (entityManagerFactory == null) {
			if (puInfo.getDataSource() == null) {
				return;
			}
			LOG.info("Create EntityManagerFactory...");
			try {
				entityManagerFactory = createEntityManagerFactoryInternal(Collections.<String, Object>emptyMap());
			} catch (RuntimeException e) {
				LOG.error("can't create EntityManagerFactory with current properties", e);
				return;
			}
		}
		// the creation of an EMF might has caused a package refresh and made
		// our builder registration invalid, so we must ensure here that the
		// service is still there
		registerEntityManagerFactoryBuilder();
		if (!PaxJPA.isValid(emfRegistration)) {
			// spec-ref 127.4.5 Service Registrations : The JPA Services must be
			// registered through the Bundle Context of the corresponding
			// Persistence Bundle
			Bundle bundle = getPersistenceBundle().getBundle();
			BundleContext bundleContext = bundle.getBundleContext();
			if (bundleContext != null) {
				LOG.info("register EntityManagerFactory Service for persistence unit {}...",
						puInfo.getPersistenceUnitName());
				Dictionary<String, String> emfServiceProps = new Hashtable<>();
				emfServiceProps.put(EntityManagerFactoryBuilder.JPA_UNIT_NAME, puInfo.getPersistenceUnitName());
				emfServiceProps.put(EntityManagerFactoryBuilder.JPA_UNIT_VERSION, bundle.getVersion().toString());
				emfServiceProps.put(EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER,
						persistenceProvider.getClass().getName());
				emfRegistration = bundleContext.registerService(EntityManagerFactory.class,
						new EntityManagerFactoryService(entityManagerFactory), emfServiceProps);
			} else {
				LOG.info("persistence bundle {} is not started yet, service registration is delayed...",
						PaxJPA.getBundleName(bundle));
			}
		}
	}

	private void unregisterEntityManagerFactory() {
		PaxJPA.unregister(emfRegistration);
		emfRegistration = null;
		if (entityManagerFactory != null) {
			try {
				entityManagerFactory.close();
			} catch (RuntimeException e) {
				LOG.debug("Closing EntityManagerFactory throws an exception", e);
			}
			entityManagerFactory = null;
		}
	}

	public Properties getPersistenceProperties() {

		return puProps;
	}
}
