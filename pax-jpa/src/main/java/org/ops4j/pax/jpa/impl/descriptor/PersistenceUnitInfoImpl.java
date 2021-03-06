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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.ops4j.pax.jpa.impl.PaxJPA;
import org.ops4j.pax.jpa.impl.PersistenceBundle;
import org.ops4j.pax.jpa.impl.TemporaryBundleClassLoader;
import org.ops4j.pax.jpa.jaxb.Persistence.PersistenceUnit;
import org.ops4j.pax.jpa.jaxb.PersistenceUnitCachingType;
import org.ops4j.pax.jpa.jaxb.PersistenceUnitValidationModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of PersistenceUnitInfo used to create {@link EntityManagerFactory}s
 *
 * @author Harald Wellmann
 * @author Christoph Läubrich - move functional code out of this class
 *
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

	private static Logger LOG = LoggerFactory.getLogger(PersistenceUnitInfoImpl.class);
	private PersistenceBundle persitenceBundle;
	private String version;
	private PersistenceUnit persistenceUnit;
	private DataSource dataSource;
	private Properties persitenceProperties;
	private PersistenceProvider provider;
	private List<ClassTransformer> classTransformers = new CopyOnWriteArrayList<>();
	private PersistenceUnitTransactionType transactionType;

	public PersistenceUnitInfoImpl(PersistenceBundle bundle, String version, PersistenceUnit persistenceUnit,
			Properties props) {
		this.persitenceBundle = bundle;
		this.version = version;
		this.persistenceUnit = persistenceUnit;
		this.persitenceProperties = new Properties(props);
		org.ops4j.pax.jpa.jaxb.PersistenceUnitTransactionType xmlTransactionType = persistenceUnit.getTransactionType();
		transactionType = xmlTransactionType == null ? PersistenceUnitTransactionType.RESOURCE_LOCAL
				: PersistenceUnitTransactionType.valueOf(xmlTransactionType.toString());
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

		if (getTransactionType() == PersistenceUnitTransactionType.JTA) {
			return dataSource;
		}
		return null;
	}

	@Override
	public DataSource getNonJtaDataSource() {

		if (getTransactionType() == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
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

		return persitenceProperties;
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
		LOG.debug("adding ClassTransformer {} to persistence unit {}, must refresh persistence bundle {}",
				new Object[] { transformer.getClass().getName(), getPersistenceUnitName(),
						PaxJPA.getBundleName(persitenceBundle.getBundle()) });
		classTransformers.add(transformer);
		try {
			persitenceBundle.refresh();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public List<ClassTransformer> getClassTransformers() {

		return Collections.unmodifiableList(classTransformers);
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

	public String getJndiDataSourceName() {

		if (getTransactionType() == PersistenceUnitTransactionType.JTA) {
			return persistenceUnit.getJtaDataSource();
		} else {
			return persistenceUnit.getNonJtaDataSource();
		}
	}
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		classTransformers.clear();
	}
}
