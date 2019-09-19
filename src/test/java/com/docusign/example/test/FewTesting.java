package com.docusign.example.test;
import static org.junit.Assert.*;
import org.junit.Test;

public class FewTesting {

	@Test
	public void test() throws InterruptedException {
		// Sending 5 tests every hour for 8 hours
		RunTest test = new RunTest();
		test.startTest("few");
	}

}
