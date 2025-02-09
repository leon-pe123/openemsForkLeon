package io.openems.edge.controller.ess.chargedischargelimiter;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;

public class MyControllerTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerChargeDischargeLimiterImpl()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.build())
				.next(new TestCase());
	}

}
