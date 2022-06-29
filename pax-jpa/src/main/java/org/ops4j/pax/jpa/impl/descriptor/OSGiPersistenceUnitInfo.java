/*
 * Copyright 2012 Harald Wellmann.
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
package org.ops4j.pax.jpa.impl.descriptor;

import java.net.URL;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.jcp.xmlns.xml.ns.persistence.Persistence.PersistenceUnit;
import org.jcp.xmlns.xml.ns.persistence.PersistenceUnitCachingType;
import org.jcp.xmlns.xml.ns.persistence.PersistenceUnitValidationModeType;
import org.ops4j.pax.jpa.impl.EntityManagerFactoryBuilderImpl;
import org.ops4j.pax.jpa.impl.PersistenceBundle;
import org.ops4j.pax.jpa.impl.TemporaryBundleClassLoader;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PersistenceUnitInfo used to create {@link EntityManagerFactory}s
 *
 * @author Harald Wellmann
 * @author Christoph Läubrich - move functional code out of this class
 *
 */
public class OSGiPersistenceUnitInfo implements PersistenceUnitInfo {

	public static final String SERVICE_PROPERTY_PERSISTENCE_PROVIDER = "persistenceProvider";
	public static final String SERVICE_PROPERTY_DATA_SOURCE_FACTORY = "dataSourceFactory";
	public static final String SERVICE_PROPERTY_DATA_SOURCE = "dataSource";

	private static final Logger LOG = LoggerFactory.getLogger(OSGiPersistenceUnitInfo.class);
	private final PersistenceBundle persitenceBundle;
	private final String version;
	private final PersistenceUnit persistenceUnit;
	private final Properties persitenceProperties;
	private final PersistenceUnitTransactionType transactionType;
	protected volatile DataSource dataSource;
	protected volatile PersistenceProvider provider;
	protected volatile DataSourceFactory dataSourceFactory;
	protected volatile DataSourceFactoryDescriptor dataSourceFactoryDescriptor;

	public OSGiPersistenceUnitInfo(PersistenceBundle bundle, String version, PersistenceUnit persistenceUnit, Properties props) {

		this.persitenceBundle = bundle;
		this.version = version;
		this.persistenceUnit = persistenceUnit;
		this.persitenceProperties = props;
		org.jcp.xmlns.xml.ns.persistence.PersistenceUnitTransactionType xmlTransactionType = persistenceUnit.getTransactionType();
		transactionType = xmlTransactionType == null ? PersistenceUnitTransactionType.RESOURCE_LOCAL : PersistenceUnitTransactionType.valueOf(xmlTransactionType.toString());
	}

	@Override
	public String getPersistenceUnitName() {

		return persistenceUnit.getName();
	}

	@Override
	public String getPersistenceProviderClassName() {

		return persistenceUnit.getProvider();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {

		return transactionType;
	}

	@Override
	public DataSource getJtaDataSource() {

		if(getTransactionType() == PersistenceUnitTransactionType.JTA) {
			return dataSource;
		}
		return null;
	}

	@Override
	public DataSource getNonJtaDataSource() {

		if(getTransactionType() == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
			return dataSource;
		}
		return null;
	}

	@Override
	public List<String> getMappingFileNames() {

		return persistenceUnit.getMappingFile();
	}

	@Override
	public List<URL> getJarFileUrls() {

		return Collections.emptyList();
	}

	@Override
	public URL getPersistenceUnitRootUrl() {

		return persitenceBundle.getBundle().getEntry("/");
	}

	@Override
	public List<String> getManagedClassNames() {

		return Collections.unmodifiableList(persistenceUnit.getClazz());
	}

	@Override
	public boolean excludeUnlistedClasses() {

		Boolean exclude = persistenceUnit.isExcludeUnlistedClasses();
		return (exclude == null) ? false : exclude;
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {

		PersistenceUnitCachingType sharedCacheMode = persistenceUnit.getSharedCacheMode();
		return (sharedCacheMode == null) ? SharedCacheMode.NONE : SharedCacheMode.valueOf(sharedCacheMode.toString());
	}

	@Override
	public ValidationMode getValidationMode() {

		PersistenceUnitValidationModeType validationMode = persistenceUnit.getValidationMode();
		return (validationMode == null) ? ValidationMode.NONE : ValidationMode.valueOf(validationMode.toString());
	}

	@Override
	public Properties getProperties() {

		// we copy here for two reasons:
		// 1. not allow anyone to modify our internal Properties
		// 2. someone might use the Properties as a map, what has undesired results, see for example:
		// https://github.com/eclipse-ee4j/eclipselink/issues/1564
		Properties properties = new Properties();
		for(String name : persitenceProperties.stringPropertyNames()) {
			String value = persitenceProperties.getProperty(name);
			if(value == null) {
				continue;
			}
			properties.setProperty(name, value);
		}
		return properties;
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {

		return version;
	}

	@Override
	public ClassLoader getClassLoader() {

		return persitenceBundle.getClassLoader();
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {

		if(persitenceBundle.getClassTransformers().add(transformer)) {
			LOG.debug("adding ClassTransformer {} to persistence unit {}, must refresh persistence bundle {}", new Object[]{transformer.getClass().getName(), getPersistenceUnitName(), persitenceBundle
					.getBundle()});
			try {
				persitenceBundle.refresh();
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Override
	public ClassLoader getNewTempClassLoader() {

		return new TemporaryBundleClassLoader(persitenceBundle.getBundle(), provider.getClass().getClassLoader());
	}

	public void setProvider(PersistenceProvider provider) {

		this.provider = provider;
	}

	public DataSource getDataSource() {

		return dataSource;
	}

	public DataSourceFactory getDataSourceFactory() {

		return dataSourceFactory;
	}

	public PersistenceProvider getPersistenceProvider() {

		return provider;
	}

	public String getJndiDataSourceName() {

		if(getTransactionType() == PersistenceUnitTransactionType.JTA) {
			return persistenceUnit.getJtaDataSource();
		} else {
			return persistenceUnit.getNonJtaDataSource();
		}
	}

	public void setDataSource(DataSource dataSource) {

		this.dataSource = dataSource;
	}

	public DataSourceFactoryDescriptor getDataSourceFactoryDescriptor() {

		return dataSourceFactoryDescriptor;
	}

	public void setDataSourceFactory(DataSourceFactory dataSourceFactory, DataSourceFactoryDescriptor descriptor) {

		this.dataSourceFactory = dataSourceFactory;
		this.dataSourceFactoryDescriptor = descriptor;
		if(dataSourceFactory == null) {
			this.dataSource = null;
			this.dataSourceFactoryDescriptor = null;
			LOG.info("no DataSourceFactory available, persistence unit {} is incomplete", getName());
		} else {
			try {
				LOG.info("bind persistence unit {} to DataSourceFactory {}", getName(), dataSourceFactory.getClass().getName());
				this.dataSource = EntityManagerFactoryBuilderImpl.createDataSource(dataSourceFactory, getProperties());
			} catch(SQLException e) {
				this.dataSource = null;
				this.dataSourceFactoryDescriptor = null;
				LOG.error("can't bind datasource", e);
				return;
			}
		}
	}

	public String getName() {

		return getPersistenceUnitName();
	}
}
