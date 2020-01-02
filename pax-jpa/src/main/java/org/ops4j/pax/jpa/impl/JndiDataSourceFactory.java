/*******************************************************************************
 * Copyright (c) 2020 Lablicate GmbH.
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
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.ops4j.pax.jpa.impl;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.osgi.service.jdbc.DataSourceFactory;

/**
 * A simple bridge between JNDI and DataSourceFactory
 * @author Christoph Läubrich
 *
 */
public class JndiDataSourceFactory implements DataSourceFactory{

	private String jndiName;

	public JndiDataSourceFactory(String jndiName) {
		this.jndiName = jndiName;
		
	}

	@Override
	public DataSource createDataSource(Properties props) throws SQLException {

		return lookUp(DataSource.class);
	}

	@Override
	public ConnectionPoolDataSource createConnectionPoolDataSource(Properties props) throws SQLException {

		return lookUp(ConnectionPoolDataSource.class);
	}

	@Override
	public XADataSource createXADataSource(Properties props) throws SQLException {

		return lookUp(XADataSource.class);
	}

	@Override
	public Driver createDriver(Properties props) throws SQLException {

		return lookUp(Driver.class);
	}
	
	private <T> T lookUp(Class<T> clz) throws SQLException {
		try {
			InitialContext context = new InitialContext();
			Object lookup = context.lookup(jndiName);
			if (clz.isInstance(lookup)) {
				return clz.cast(lookup);
			} else {
				throw new SQLException(jndiName+" was found but object is not of required type "+clz);
			}
		} catch(NamingException exc) {
			throw new SQLException(exc);
		}
	}
	
	public String getJndiName() {
		return jndiName;
	}
}
