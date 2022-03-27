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

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.xml.transform.sax.SAXSource;

import org.jcp.xmlns.xml.ns.persistence.ObjectFactory;
import org.jcp.xmlns.xml.ns.persistence.Persistence;
import org.jcp.xmlns.xml.ns.persistence.Persistence.PersistenceUnit;
import org.jcp.xmlns.xml.ns.persistence.PersistenceUnitCachingType;
import org.jcp.xmlns.xml.ns.persistence.PersistenceUnitValidationModeType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Parser for persistence descriptors. Only supports the JPA 2.0 scheme.
 * 
 * Based on JAXB.
 * 
 * @author Harald Wellmann
 *
 */
public class PersistenceDescriptorParser {

	private static JAXBContext singletonJaxbContext;

	public Persistence parseDescriptor(URL url) throws JAXBException, IOException, SAXException {

		XMLReader reader = XMLReaderFactory.createXMLReader();
		// Use filter to override the namespace in the document.
		// On JDK 7, JAXB fails to parse the document if the namespace does not match
		// the one indicated by the generated JAXB model classes.
		// For some reason, the JAXB version in JDK 8 is more lenient and does
		// not require this filter.
		NamespaceFilter inFilter = new NamespaceFilter("http://xmlns.jcp.org/xml/ns/persistence");
		inFilter.setParent(reader);
		JAXBContext context = getJaxbContext();
		Unmarshaller unmarshaller = context.createUnmarshaller();
		SAXSource source = new SAXSource(inFilter, new InputSource(url.openStream()));
		return unmarshaller.unmarshal(source, Persistence.class).getValue();
	}

	private synchronized JAXBContext getJaxbContext() throws JAXBException {

		if(singletonJaxbContext == null) {
			singletonJaxbContext = JAXBContext.newInstance(ObjectFactory.class, Persistence.class, PersistenceUnitCachingType.class, PersistenceUnitValidationModeType.class, ObjectFactory.class);
		}
		return singletonJaxbContext;
	}

	public Properties parseProperties(PersistenceUnit persistenceUnit) {

		PersistenceUnit.Properties jaxbProps = persistenceUnit.getProperties();
		Properties props = new Properties();
		if(jaxbProps != null) {
			for(PersistenceUnit.Properties.Property p : jaxbProps.getProperty()) {
				props.setProperty(p.getName(), p.getValue());
			}
		}
		return props;
	}
}
