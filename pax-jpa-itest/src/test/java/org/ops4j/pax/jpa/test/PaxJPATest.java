package org.ops4j.pax.jpa.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;

@ExtendWith(BundleContextExtension.class)
public class PaxJPATest {

	@InjectBundleContext
	BundleContext bc;

	public PaxJPATest() {

		System.out.println("BasicTest.BasicTest()");
	}

	@BeforeAll
	static void beforeAll(@InjectBundleContext BundleContext staticBC) {

		System.out.println("The static bc is " + staticBC);
	}

	@Test
	@DisplayName("Test BundleContext injection")
	void myTest() {

		System.out.println("The injected bc is " + bc);
	}
}
