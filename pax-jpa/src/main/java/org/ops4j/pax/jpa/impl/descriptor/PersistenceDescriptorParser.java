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
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.ops4j.pax.jpa.jaxb.Persistence;
import org.ops4j.pax.jpa.jaxb.Persistence.PersistenceUnit;

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

    public Persistence parseDescriptor(URL url) throws JAXBException {
        JAXBContext jaxbContext = getJaxbContext();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Persistence persistenceXml = (Persistence) unmarshaller.unmarshal(url);
        return persistenceXml;
    }

    private synchronized JAXBContext getJaxbContext() throws JAXBException {
        if (singletonJaxbContext == null) {
            singletonJaxbContext = JAXBContext.newInstance(Persistence.class.getPackage().getName(), getClass()
                .getClassLoader());
        }
        return singletonJaxbContext;
    }

    public Properties parseProperties(PersistenceUnit persistenceUnit) {
        PersistenceUnit.Properties jaxbProps = persistenceUnit.getProperties();
        Properties props = new Properties();
        if (jaxbProps != null) {
            for (PersistenceUnit.Properties.Property p : jaxbProps.getProperty()) {
                props.setProperty(p.getName(), p.getValue());
            }
        }
        return props;
    }
}
