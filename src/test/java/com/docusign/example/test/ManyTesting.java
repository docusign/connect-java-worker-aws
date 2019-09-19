package com.docusign.example.test;

import org.junit.Test;

public class ManyTesting {

	@Test
	public void test() throws InterruptedException {
		// Sending many tests every hour for 8 hours
		RunTest test = new RunTest();
		test.startTest("many");
	}
}
