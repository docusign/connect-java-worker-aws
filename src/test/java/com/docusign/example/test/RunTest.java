package com.docusign.example.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import com.docusign.example.worker.DSConfig;
import com.docusign.example.worker.ProcessNotification;
import com.docusign.example.worker.DatePretty;

public class RunTest {

	private static DateTime timeStart;
	private static DateTime[] timeChecks = new DateTime[8];
	private static int timeCheckNumber=0; // 0..6 
	private static int successes=0;
	private static int enqueueErrors=0;
	private static int dequeueErrors=0;
	public static String mode; // many or few
	ArrayList<String> testsSent = new ArrayList<String>(); // test values sent that should also be receieved
	private static boolean foundAll = false;

	@Test
	public void startTest(String modeName) throws InterruptedException {
		mode = modeName;
		timeStart = new DateTime();
		for(int i=0; i<=7 ; i++) {
			timeChecks[i] = timeStart.plusHours(i+1);
		}
		System.out.println(DatePretty.date() + "Starting");
		doTests();
		System.out.println(DatePretty.date() + "Done");
		
	}

	private void doTests() throws InterruptedException{
		while(timeCheckNumber<=7) {
			while (timeChecks[timeCheckNumber].isAfterNow()) {
				doTest();
				if(mode=="few") {
					DateTime now = new DateTime();
					int sleep = timeChecks[timeCheckNumber].getSecondOfDay()-now.getSecondOfDay()+2;
					TimeUnit.SECONDS.sleep(sleep);
				}
			}
			showStatus();
			timeCheckNumber++;
		}
		showStatus();
	}


	private void showStatus() {
		double rate = (100.0 * successes) / (enqueueErrors + dequeueErrors + successes);
		System.out.println(DatePretty.date() + "#### Test statistics: " + successes + " (" + String.format("%.2f", rate) + "%) successes, " +
				enqueueErrors + " enqueue errors, " + dequeueErrors + " dequeue errors.");
	}

	private void doTest() throws InterruptedException{
		send(); // sets testsSent
		DateTime endTime = DateTime.now().plusMinutes(3);
		foundAll = false;
		int tests = testsSent.size();
		int successesStart = successes;
		while(!foundAll && endTime.isAfterNow()) {
			TimeUnit.SECONDS.sleep(1);
			checkResults(); // sets foundAll and updates testsSent
		}
		if(!foundAll) {
			dequeueErrors += testsSent.size();
		}
		
		System.out.println("Test: " + tests + " sent. " + (successes - successesStart) + " successes, " +
				testsSent.size() + " failures.") ;
	}


	/**
	 * Look for the reception of the testsSent values
	 */
	private void checkResults() {

		ArrayList<String> testsReceived = new ArrayList <String>();
		String fileData = "";
		for(int i=0 ; i<=20 ; i++) {
			fileData = "";
			try {
				// The path of the files created of Test mode
				File testDir = new File(Paths.get(ProcessNotification.mainPath, DSConfig.TEST_OUTPUT_DIR_NAME).toString());
				File file = new File(testDir, "test" + i + ".txt");
				if(file.exists() && !file.equals(null)) {
					BufferedReader br = new BufferedReader(new FileReader(file)); 
					fileData = br.readLine();
					br.close();
				}
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
			if(!fileData.equals(null) && !fileData.isEmpty()) {
				testsReceived.add(fileData);
			}
		}

		// Create a private copy of testsSent (testsSentOrig) and reset testsSent
		// Then, for each element in testsSentOrig not found, add back to testsSent.
		ArrayList<String> testsSentOrig = new ArrayList<String>(testsSent);
		testsSent.clear();
		for(String testValue : testsSentOrig) {
			if(testsReceived.contains(testValue)) {
				successes++;
			}
			else {
				testsSent.add(testValue);
			}
		}
		
		// Update foundAll
		foundAll = testsSent.size() == 0;
	}

	/**
	 * Send 5 messages
	 * @throws InterruptedException 
	 */
	private void send() throws InterruptedException {
		testsSent.clear();
		for(int i=0 ; i<5 ; i++) {
			try {
				long now = Instant.now().toEpochMilli();
				String testValue = "" + now;
				send1(testValue);
				testsSent.add(testValue);
			}
			catch(Exception e) {
				enqueueErrors ++;
				System.out.println(("send: Enqueue error: " + e));
				TimeUnit.SECONDS.sleep(30);
			}
		}
	}

	/**
	 * Send one enqueue request. Errors will be caught by caller
	 * @param test the test value 
	 */
	private void send1(String test) {

		try {
			URL url = new URL(DSConfig.ENQUEUE_URL + "?test=" + test);
			HttpsURLConnection options = (HttpsURLConnection) url.openConnection();
			options.setReadTimeout(1000 * 20);
			options.setRequestMethod("GET");
			options.setRequestProperty(DSConfig.AWS_ACCOUNT, DSConfig.AWS_SECRET);
			String auth = authObject();
			if(!auth.equals(null)) { 
				 String basicAuth = "Basic " + new String(Base64.getEncoder().encode(auth.getBytes()));
				 options.setRequestProperty("Authorization", basicAuth);
			}
			
			int responseCode = options.getResponseCode();
			if(responseCode!=HttpsURLConnection.HTTP_OK) { 
				Assert.fail("send1: GET not worked");
			}
		}
		catch(SocketTimeoutException e) {
			Assert.fail(DatePretty.date() + "send1: SocketTimeoutException - failed to read: " + e);
		}
		catch(IOException e) {
			Assert.fail(DatePretty.date() + "send1: IOException - failed to open url connection: " + e);
		}
	}

	/**
	 * Returns a string for the HttpsURLConnection request 
	 * @return Authorization that contains DSConfig.BASIC_AUTH_NAME and DSConfig.BASIC_AUTH_PW if exist
	 */
	private String authObject() {
		if (!DSConfig.BASIC_AUTH_NAME.equals(null) && DSConfig.BASIC_AUTH_NAME != "{BASIC_AUTH_NAME}" 
				&& !DSConfig.BASIC_AUTH_PW.equals(null) && DSConfig.BASIC_AUTH_PW != "{BASIC_AUTH_PS}" ) {
			return DSConfig.BASIC_AUTH_NAME + ":" + DSConfig.BASIC_AUTH_PW;
		} 
		else {
			return null;
		}
	}
}
