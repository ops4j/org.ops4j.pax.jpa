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
import org.ops4j.pax.jpa.JpaConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Adapter for Eclipselink to register its PersistenceProvider service with the 
 * required service properties.
 * 
 * @author Harald Wellmann
 *
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        PersistenceProvider providerService = new PersistenceProvider();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(JpaConstants.JPA_PROVIDER, PersistenceProvider.class.getName());
        context.registerService(javax.persistence.spi.PersistenceProvider.class, providerService, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }
}
