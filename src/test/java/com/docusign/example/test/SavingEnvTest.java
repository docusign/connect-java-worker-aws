package com.docusign.example.test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.example.worker.DSConfig;
import com.docusign.example.worker.ProcessNotification;
import com.docusign.example.test.CreateEnvelope;
import com.docusign.example.worker.DatePretty;

public class SavingEnvTest {

	private static final ApiClient apiClient = new ApiClient();

	@Test
	public void test() {
		System.out.println(DatePretty.date() + "Starting");
		sendEnvelope();
		created();
		System.out.println(DatePretty.date() + "Done");
	}

	public void sendEnvelope() {
		try {
			System.setProperty("https.protocols","TLSv1.2");
			System.out.println("Sending an envelope. The envelope includes HTML, Word, and PDF documents.\n"
					+ "It takes about 15 seconds for DocuSign to process the envelope request... ");
			EnvelopeSummary result = new CreateEnvelope(apiClient).sendEnvelope();
			System.out.println(
					String.format("Envelope status: %s. Envelope ID: %s",
							result.getStatus(),
							result.getEnvelopeId()));

		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		catch (ApiException e) {
			System.err.println("\nDocuSign Exception!");
		}
	}

	/**
	 * Checks if the program created file from the envelope
	 */
	private void created() {
		try {
			TimeUnit.SECONDS.sleep(30);
			File file = new File(ProcessNotification.mainPath + "\\output\\order_Test_Mode.pdf");
			if(!file.exists()) {
				Assert.fail("Failed to find the file");
			}	
		} 
		catch (InterruptedException e) {
			Assert.fail("Failed to sleep: " + e.getMessage());
		}
	}
}
