/* 
 * Copyright 2011 Harald Wellmann.
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
package org.ops4j.pax.jpa.eclipselink.impl;

import java.util.Hashtable;

import org.eclipse.persistence.jpa.PersistenceProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    public static final String PERSISTENCE_PROVIDER = "javax.persistence.provider";
    public static final String ECLIPSELINK_OSGI_PROVIDER = "org.eclipse.persistence.jpa.PersistenceProvider";
    private static BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        registerProviderService();
    }

    /**
     * Our service provider provides the javax.persistence.spi.PersistenceProvider service. In this
     * method, we register as a provider of that service
     * 
     * @throws Exception
     */
    public void registerProviderService() throws Exception {
        // Create and register ourselves as a JPA persistence provider service
        PersistenceProvider providerService = new PersistenceProvider();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(PERSISTENCE_PROVIDER, ECLIPSELINK_OSGI_PROVIDER);
        context
            .registerService("javax.persistence.spi.PersistenceProvider", providerService, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
