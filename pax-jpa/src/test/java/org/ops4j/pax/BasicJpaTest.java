package org.ops4j.pax;

import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ops4j.pax.jpa.impl.PaxJPA;

@ExtendWith(OsgiContextExtension.class)
public class BasicJpaTest {

	private final OsgiContext context = new OsgiContext();
	private final PaxJPA paxJPA = new PaxJPA();

	@BeforeEach
	public void start() throws Exception {
		paxJPA.start(context.bundleContext());
	}

	@AfterEach
	public void stop() throws Exception {

		paxJPA.stop(context.bundleContext());
	}

}
