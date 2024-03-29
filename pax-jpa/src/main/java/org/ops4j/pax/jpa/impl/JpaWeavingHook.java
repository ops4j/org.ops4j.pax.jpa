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

import java.lang.instrument.IllegalClassFormatException;
import java.util.Collection;
import java.util.Set;

import javax.persistence.spi.ClassTransformer;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Weaving hook for enhancing entity classes.
 * 
 * The hook applies the transformer obtained from the persistence provider and
 * adds dynamic imports for any additional packages required by the enhanced
 * code.
 * 
 * @author Harald Wellmann
 *
 */
public class JpaWeavingHook implements WeavingHook {

	private static final Logger LOG = LoggerFactory.getLogger(JpaWeavingHook.class);

	private final Set<String> managedClasses;

	private final Bundle persistenceBundle;

	private final Collection<ClassTransformer> transformers;

	private final String unitName;

	public JpaWeavingHook(Bundle persistenceBundle, String unitName, Set<String> managedClasses, Collection<ClassTransformer> transformers) {

		this.persistenceBundle = persistenceBundle;
		this.unitName = unitName;
		this.transformers = transformers;
		this.managedClasses = managedClasses;
	}

	@Override
	public void weave(WovenClass wovenClass) {

		if(wovenClass.getBundleWiring().getBundle() == persistenceBundle
				&& managedClasses.contains(wovenClass.getClassName())) {
			if (transformers.isEmpty()) {
				return;
			}
			try {
				synchronized (this) {
					LOG.debug("weaving class {} of persistence unit {}", wovenClass.getClassName(), unitName);
					ClassLoader tempClassLoader = wovenClass.getBundleWiring().getClassLoader();
					boolean woven = false;
					for (ClassTransformer transformer : transformers) {
						byte[] transformed = transformer.transform(tempClassLoader, wovenClass.getClassName(),
								wovenClass.getDefinedClass(), wovenClass.getProtectionDomain(), wovenClass.getBytes());
						if (transformed != null) {
							wovenClass.setBytes(transformed);
							woven = true;
						}
					}
					if (woven) {
						/*
						 * 
						 * TODO Hard-coded list of packages for OpenJPA,
						 * Eclipselink and Hibernate. We should only add the
						 * ones required for the given provider.
						 */
						wovenClass.getDynamicImports().add("org.apache.openjpa.enhance");
						wovenClass.getDynamicImports().add("org.apache.openjpa.util");

						wovenClass.getDynamicImports().add("org.eclipse.persistence.*");

						wovenClass.getDynamicImports().add("org.hibernate.*");
						wovenClass.getDynamicImports().add("javassist.util.proxy");
					}
				}
			} catch (IllegalClassFormatException exc) {
				throw new WeavingException("cannot transform " + wovenClass.getClassName(), exc);
			}
		}
	}

}
