/*
 * Copyright 2012 Harald Wellmann.
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

import org.ops4j.lang.Ops4jException;
import org.ops4j.pax.jpa.impl.JpaWeavingHook;
import org.ops4j.pax.jpa.impl.TemporaryBundleClassLoader;
import org.ops4j.pax.jpa.jaxb.Persistence.PersistenceUnit;
import org.ops4j.pax.jpa.jaxb.PersistenceUnitCachingType;
import org.ops4j.pax.jpa.jaxb.PersistenceUnitValidationModeType;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;

public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    private Bundle bundle;

    private PersistenceUnit persistenceUnit;

    private DataSourceFactory dataSourceFactory;

    private Properties props;

    private BundleClassLoader cl;

    private PersistenceProvider provider;

    private ServiceRegistration<EntityManagerFactoryBuilder> emfBuilderRegistration;
    private ServiceRegistration<EntityManagerFactory> emfRegistration;
    private ServiceRegistration<WeavingHook> hookRegistration;
    
    private boolean ready;

    public PersistenceUnitInfoImpl(Bundle bundle, PersistenceUnit persistenceUnit, Properties props) {
        this.bundle = bundle;
        this.persistenceUnit = persistenceUnit;
        this.props = props;
        this.cl = new BundleClassLoader(bundle);
    }

    public Bundle getBundle() {
        return bundle;
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
        org.ops4j.pax.jpa.jaxb.PersistenceUnitTransactionType transactionType = persistenceUnit
            .getTransactionType();
        return transactionType == null ? null : PersistenceUnitTransactionType
            .valueOf(transactionType.toString());
    }

    @Override
    public DataSource getJtaDataSource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        String url = props.getProperty(JpaConstants.JPA_URL);
        String user = props.getProperty(JpaConstants.JPA_USER);
        String password = props.getProperty(JpaConstants.JPA_PASSWORD);
        Properties dsfProps = new Properties();
        dsfProps.setProperty(DataSourceFactory.JDBC_URL, url);

        if (user != null) {
            dsfProps.setProperty(DataSourceFactory.JDBC_USER, user);
        }
        if (password != null) {
            dsfProps.setProperty(DataSourceFactory.JDBC_PASSWORD, password);
        }
        try {
            return dataSourceFactory.createDataSource(dsfProps);
        }
        catch (SQLException exc) {
            throw new Ops4jException(exc);
        }
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
        return bundle.getEntry("/");
    }

    @Override
    public List<String> getManagedClassNames() {
        return persistenceUnit.getClazz();
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return persistenceUnit.isExcludeUnlistedClasses();
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        PersistenceUnitCachingType sharedCacheMode = persistenceUnit.getSharedCacheMode();
        return (sharedCacheMode == null) ? null : SharedCacheMode.valueOf(sharedCacheMode
            .toString());
    }

    @Override
    public ValidationMode getValidationMode() {
        PersistenceUnitValidationModeType validationMode = persistenceUnit.getValidationMode();
        return (validationMode == null) ? null : ValidationMode.valueOf(validationMode.toString());
    }

    @Override
    public Properties getProperties() {
        return props;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "2.0";
    }

    @Override
    public ClassLoader getClassLoader() {
        return cl;
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        JpaWeavingHook hook = new JpaWeavingHook(this, transformer);
        hookRegistration = bundle.getBundleContext().registerService(WeavingHook.class, hook, null);
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return new TemporaryBundleClassLoader(bundle, provider.getClass().getClassLoader());
    }

    public PersistenceProvider getProvider() {
        return provider;
    }

    public void setProvider(PersistenceProvider provider) {
        this.provider = provider;
    }

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    public ServiceRegistration<EntityManagerFactory> getEmfRegistration() {
        return emfRegistration;
    }

    public void setEmfRegistration(ServiceRegistration<EntityManagerFactory> emfRegistration) {
        this.emfRegistration = emfRegistration;
    }

    public ServiceRegistration<EntityManagerFactoryBuilder> getEmfBuilderRegistration() {
        return emfBuilderRegistration;
    }
    
    public void setEmfBuilderRegistration(
        ServiceRegistration<EntityManagerFactoryBuilder> emfBuilderRegistration) {
        this.emfBuilderRegistration = emfBuilderRegistration;
    }

    public boolean isReady() {
        return ready;
    }

    
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void unregister() {
        this.ready = false;
        try {
            emfBuilderRegistration.unregister();
        }
        catch (IllegalStateException exc) {
            // ignore
        }

        try {
            emfRegistration.unregister();
        }
        catch (IllegalStateException exc) {
            // ignore
        }

        try {
            hookRegistration.unregister();
        }
        catch (IllegalStateException exc) {
            // ignore
        }
        this.emfBuilderRegistration = null;
        this.emfRegistration = null;
        this.hookRegistration = null;
    }
}
