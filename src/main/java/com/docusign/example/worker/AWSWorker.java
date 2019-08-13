package com.docusign.example.worker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;

public class AWSWorker {

	private static final ApiClient apiClient = new ApiClient();
	private static BasicAWSCredentials creds = new BasicAWSCredentials(DSConfig.AWS_ACCOUNT, DSConfig.AWS_SECRET);
	private static AmazonSQS queue = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion(DSConfig.QUEUE_REGION).build();
	private static Queue<String> checkLogQ = new LinkedList<String>();
	private static String queueUrl = DSConfig.QUEUE_URL; 
	private static boolean restart = true; //restart the program if any error has occurred

	public static void main(String args[]) throws Exception {

		listenForever();
		
	}

	/**
	 * The function will listen forever, dispatching incoming notifications
	 * to the processNotification library. 
	 * See https://docs.aws.amazon.com/sdk-for-javascript/v2/developer-guide/sqs-examples-send-receive-messages.html#sqs-examples-send-receive-messages-receiving
	 * @throws Exception while trying to sleep for 5 seconds
	 */
	private static void listenForever() throws Exception {
		// Check that we can get a DocuSign token
		testToken(); 

		while(true) {
			if(restart) {
				Date date = new Date();
				SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				System.out.println(formatter.format(date) + " Starting queue worker");
				restart = false;
				// Start the queue worker
				startQueue();
			}
			TimeUnit.SECONDS.sleep(5);
		}
	}

	/**
	 * Check that we can get a DocuSign token and handle common error
	 * cases: ds_configuration not configured, need consent.
	 * @throws Exception while trying to get access to token
	 */
	private static void testToken() throws Exception {
		try {
			if(DSConfig.CLIENT_ID.equals("{CLIENT_ID}")) {
				System.err.println("Problem: you need to configure this example, either via environment variables (recommended) \n" + 
						"or via the ds_configuration.js file. \n" + 
						"See the README file for more information\n\n");
				return;
			}
			JWTAuth dsJWTAuth = new JWTAuth(apiClient);
			dsJWTAuth.checkToken();
		}

		// An API problem
		catch (ApiException e) {
			// Special handling for consent_required
			String message = e.getMessage();
			if(message != null && message.contains("consent_required")){
				String consent_url = String.format("%s/oauth/auth?response_type=code&scope=%s" +
						"&client_id=%s" +
						"&redirect_uri=%s",
						DSConfig.DS_AUTH_SERVER,
						DSConfig.PERMISSION_SCOPES,
						DSConfig.CLIENT_ID,
						DSConfig.OAUTH_REDIRECT_URI);
				System.err.println("\nC O N S E N T   R E Q U I R E D" +
						"\nAsk the user who will be impersonated to run the following url: " +
						"\n"+ consent_url+
						"\n\nIt will ask the user to login and to approve access by your application." +
						"\nAlternatively, an Administrator can use Organization Administration to" +
						"\npre-approve one or more users.");
				System.exit(0);
			} 
			else {
				// Some other DocuSign API problem 
				System.err.println(String.format("    Reason: %d", e.getCode()));
				System.err.println(String.format("    Error Reponse: %s", e.getResponseBody()));
				System.exit(0);
			}
		}
		// Not an API problem
		catch(Exception e) {
			Date date = new Date();
			SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			System.err.println(formatter.format(date) +  " " + e.getMessage());
		}
	}

	/**
	 * Receive and wait for messages from queue 
	 * @throws Exception catches all types of errors that may occur during the program
	 */
	private static void startQueue() throws Exception {

		Date date;
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

		try {
			// Receive messages from queue, maximum waits for 20 seconds for message
			ReceiveMessageRequest receive_request = new ReceiveMessageRequest()
					.withQueueUrl(queueUrl)
					.withWaitTimeSeconds(20);

			while(true) {
				date = new Date();
				addCheckLogQ(formatter.format(date) + " Awaiting a message...");
				// The List will contain all the queue messages
				List<Message> messages = queue.receiveMessage(receive_request).getMessages();
				// The amount of messages received
				int msgCount = messages.size();
				date = new Date();
				addCheckLogQ(formatter.format(date) + " found " + msgCount + " message(s)");
				// If at least one message has been received
				if(msgCount!=0) {
					printCheckLogQ();
					for(Message m: messages) {
						messageHandler(m, queue);
					}
				}
			}
		}
		// Catches all types of errors that may occur during the program
		catch(Exception e) {
			printCheckLogQ();
			date = new Date();
			System.err.println("\n"+formatter.format(date) + " Queue receive error:");
			System.err.println(e.getMessage());
			TimeUnit.SECONDS.sleep(5);
			// Restart the program
			restart = true;
		}
	}


	/**
	 * Maintain the array checkLogQ as a FIFO buffer with length 4.
	 * When a new entry is added, remove oldest entry and shuffle.
	 * @param message the message received from queue
	 */
	private static void addCheckLogQ(String message) {
		int length = 4;
		// If checkLogQ size is smaller than 4 add the message
		if(checkLogQ.size() < length) {
			checkLogQ.add(message);
		}
		// If checkLogQ size is bigger than 4
		else {
			// Remove the oldest message
			checkLogQ.remove();
			// Create temporary queue in order to change checkLogQ
			Queue<String> temp = new LinkedList<String>();
			for(String msg : checkLogQ) {
				temp.add(msg);
			}
			// Add the new message
			temp.add(message);
			checkLogQ.clear();
			checkLogQ.addAll(temp);
			temp.clear(); // Reset
		}
	}

	/**
	 * Prints all checkLogQ messages to the console
	 */
	private static void printCheckLogQ() {
		// Prints all the elements in the checkLogQ
		for(String message : checkLogQ) {
			System.out.println(message);
		}
		checkLogQ.clear(); // Reset
	}

	/**
	 * Process a message
	 * See https://github.com/Azure/azure-sdk-for-js/tree/master/sdk/servicebus/service-bus#register-message-handler
	 * @param message the received message from queue
	 * @param queue contains all the info of the AmazonSQS queue 
	 * @throws Exception while creating JSON object or while trying to convert null object to string
	 */
	private static void messageHandler(Message message,  AmazonSQS queue) throws Exception {

		String test = null;
		String xml = null;
		// If there is an error the program will catch it and the JSONCreated will change to false
		boolean JSONCreated = true;
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

		if(DSConfig.DEBUG.equals("true")) {
			String str = " Processing message id " + message.getMessageId();
			System.out.println(formatter.format(date) + str);
		}
		try {
			// Parse the information from message body. the information contains contains fields like test and xml
			String body = message.getBody();
			JSONObject jsonObject = new JSONObject(body);
			xml = (String) jsonObject.get("xml");
			// Used in the test mode
			test = (String) jsonObject.get("test");

		}
		// Catch exceptions while trying to create a JSON object
		catch(JSONException e) {
			System.err.println(formatter.format(date) + " " + e.getMessage());
			JSONCreated = false;
		}
		// Catch java.lang exceptions (trying to convert null to String) - make sure your message contains both those fields
		catch(Exception e) {
			date = new Date();
			System.err.println(formatter.format(date) + " " + e.getMessage());
			JSONCreated = false;
		}
		// If JSON object created successfully - continue
		if(JSONCreated) {
			ProcessNotification.process(test, xml);
		}
		// If JSON object wasn't created - ignore message
		else {
			String errorMessage = " Null or bad body in message id " + message.getMessageId() + ". Ignoring.";
			date = new Date();
			System.out.println(formatter.format(date) + errorMessage);
		}
		// Delete the message after all its information has been parsed
		queue.deleteMessage(queueUrl, message.getReceiptHandle());
	}
}
