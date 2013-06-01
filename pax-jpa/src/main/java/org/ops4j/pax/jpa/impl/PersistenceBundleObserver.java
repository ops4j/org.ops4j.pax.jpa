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
package org.ops4j.pax.jpa.impl;

import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.xml.bind.JAXBException;

import org.ops4j.pax.jpa.impl.descriptor.JpaConstants;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceDescriptorParser;
import org.ops4j.pax.jpa.impl.descriptor.PersistenceUnitInfoImpl;
import org.ops4j.pax.jpa.jaxb.Persistence;
import org.ops4j.pax.jpa.jaxb.Persistence.PersistenceUnit;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceBundleObserver implements BundleObserver<ManifestEntry>
{

    private static Logger log = LoggerFactory.getLogger( PersistenceBundleObserver.class );

    private PersistenceDescriptorParser parser = new PersistenceDescriptorParser();

    @Override
    public void addingEntries( Bundle bundle, List<ManifestEntry> entries )
    {
        log.info( "discovered persistence bundle {} {}", bundle.getSymbolicName(),
            bundle.getVersion() );
        ManifestEntry entry = entries.get( 0 );
        String value = entry.getValue();
        String[] resources = value.split(",");
        for (String resource : resources) {
            processPersistenceDescriptor(bundle, resource);
        }
    }

    private void processPersistenceDescriptor(Bundle bundle, String resource) {
        URL persistenceXml = bundle.getEntry( resource );
        try
        {
            Persistence descriptor = parser.parseDescriptor( persistenceXml );
            for( PersistenceUnit persistenceUnit : descriptor.getPersistenceUnit() )
            {
                processPersistenceUnit( bundle, persistenceUnit );
            }
        }
        catch ( JAXBException exc )
        {
            log.error("cannot parse persistence descriptor", exc);
        }
    }

    private void processPersistenceUnit( Bundle bundle, PersistenceUnit persistenceUnit )
    {
        String puName = persistenceUnit.getName();
        log.info( "processing persistence unit {}", puName );
        BundleContext bc = bundle.getBundleContext();
        PersistenceProvider provider = ServiceLookup.getService( bc, PersistenceProvider.class );
        log.info( "found persistence provider {}", provider.getClass().getName() );

        Properties puProps = parser.parseProperties( persistenceUnit );
        String driver = puProps.getProperty( JpaConstants.JPA_DRIVER );

        Map<String, String> dsfProps = new HashMap<String, String>();
        dsfProps.put( DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, driver );

        DataSourceFactory dsf =
            ServiceLookup.getService( bc, DataSourceFactory.class, 2000, dsfProps );
        PersistenceUnitInfoImpl puInfo =
            new PersistenceUnitInfoImpl( bundle, persistenceUnit, puProps, provider, dsf );

        Properties emfProps = (Properties) puProps.clone();
        emfProps.remove( JpaConstants.JPA_DRIVER );
        emfProps.remove( JpaConstants.JPA_URL );
        emfProps.remove( JpaConstants.JPA_PASSWORD );
        emfProps.remove( JpaConstants.JPA_USER );

        EntityManagerFactory emf =
            provider.createContainerEntityManagerFactory( puInfo, emfProps );

        Dictionary<String, String> emfReg = new Hashtable<String, String>();
        emfReg.put( EntityManagerFactoryBuilder.JPA_UNIT_NAME, persistenceUnit.getName() );
        emfReg.put( EntityManagerFactoryBuilder.JPA_UNIT_VERSION, bundle.getVersion()
            .toString() );
        emfReg.put( EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER, provider.getClass()
            .getName() );
        bc.registerService( EntityManagerFactory.class.getName(), emf, emfReg );
    }

    @Override
    public void removingEntries( Bundle bundle, List<ManifestEntry> entries )
    {
        log.info( "removed persistence bundle {} {}", bundle.getSymbolicName(),
            bundle.getVersion() );
    }
}
