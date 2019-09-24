package com.docusign.example.worker;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.sun.jersey.api.client.ClientHandlerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ProcessNotification {

	private static String envelopeId;
	private static String subject;
	private static String senderName;
	private static String senderEmail;
	private static String status;
	private static String created;
	private static boolean completed;
	private static String completedMsg;
	private static String orderNumber;
	// Access to the current working directory - in order to save the folders in the right path
	public static String mainPath = Paths.get(".").toAbsolutePath().normalize().toString();

	/**
	 * Process the notification message
	 * @param test checks whether this is a regular message or test mode message
	 * @param xml contains all the information of the envelope
	 * @throws Exception failed to create file or failed to save the document - caught at startQueue method
	 */
	public static void process(String test, String xml) throws Exception{
		// Send the message to the test mode
		if(!test.isEmpty()) {
			processTest(test);
		}
		// In test mode there is no xml sting, should be checked before trying to parse it
		if(!xml.isEmpty()) {
			//Step 1. parse the xml
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xml));
			Document doc = db.parse(is);

			// To get the <node_name> nodes direct children of <EnvelopeStatus> use //EnvelopeStatus/node_name
			XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression xpathExpression = xpath.compile("//EnvelopeStatus/EnvelopeID");
			NodeList envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			Node file = envelopeStatusNodes.item(0);
			envelopeId = file.getTextContent();

			xpathExpression = xpath.compile("//EnvelopeStatus/CustomFields/CustomField/Name");
			envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			file = envelopeStatusNodes.item(0);
			if(file.getTextContent().equals(DSConfig.ENVELOPE_CUSTOM_FIELD)) {
				xpathExpression = xpath.compile("//EnvelopeStatus/CustomFields/CustomField/Value");
				envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
				file = envelopeStatusNodes.item(0);
				orderNumber = file.getTextContent();
			}
			else {
				orderNumber=null;
			}

			xpathExpression = xpath.compile("//EnvelopeStatus/Status");
			envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			file = envelopeStatusNodes.item(0);
			status = file.getTextContent();

			if(status.equals("Completed")){
				completed = true;
				completedMsg = "Completed " + completed;
			}
			else {
				completed = false;
				completedMsg = "";
			}

			xpathExpression = xpath.compile("//EnvelopeStatus/Subject");
			envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			file = envelopeStatusNodes.item(0);
			subject = file.getTextContent();

			xpathExpression = xpath.compile("//EnvelopeStatus/UserName");
			envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			file = envelopeStatusNodes.item(0);
			senderName = file.getTextContent();

			xpathExpression = xpath.compile("//EnvelopeStatus/Email");
			envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			file = envelopeStatusNodes.item(0);
			senderEmail = file.getTextContent();

			xpathExpression = xpath.compile("//EnvelopeStatus/Created");
			envelopeStatusNodes = (NodeList)xpathExpression.evaluate(doc, XPathConstants.NODESET);
			file = envelopeStatusNodes.item(0);
			created = file.getTextContent();


			// For debugging, you can print the entire notification
			System.out.println("EnvelopeId: " + envelopeId);
			System.out.println("Status: " + status);
			System.out.println("Order Number: " + orderNumber);
			System.out.println("Subject: " + subject);
			System.out.println("Sender: " + senderName + ", " + senderEmail);
			System.out.println("Sent: " + created + ", " + completedMsg);

			// Step 2. Filter the notifications
			boolean ignore = false;

			// Check if the envelope was sent from the test mode 
			// If sent from test mode - ok to continue even if the status != Completed
			if(!orderNumber.equals("Test_Mode")) {
				if(!status.equals("Completed")) {
					ignore = true;
					if(DSConfig.DEBUG.equals("true")) {
						System.out.println(DatePretty.date() + "IGNORED: envelope status is " + status);
					}
				}
			}
			
			if(orderNumber == null) {
				ignore = true;
				if(DSConfig.DEBUG.equals("true")) {
					System.out.println(DatePretty.date() + "IGNORED: envelope does not have a " +
							DSConfig.ENVELOPE_CUSTOM_FIELD + " envelope custom field.");
				}
			}

			// Step 3. (Future) Check that this is not a duplicate notification
			// The queuing system delivers on an "at least once" basis. So there is a 
			// chance that we have already processes this notification.
			//
			// For this example, we'll just repeat the document fetch if it is duplicate notification

			// Step 4 save the document - it can raise an exception which will be caught at startQueue 
			if(!ignore) {
				saveDoc(envelopeId, orderNumber);
			}
		}
	}

	/**
	 * Creates a new file that contains the envelopeId and orderNumber
	 * @param envelopeId parse from the message 
	 * @param orderNumber parse from the message 
	 * @throws Exception failed to create file or failed to save the document
	 */
	private static void saveDoc(String envelopeId, String orderNumber) throws Exception {

		try {
			ApiClient dsApiClient = new ApiClient();
			JWTAuth dsJWTAuth = new JWTAuth(dsApiClient);
			// Checks for the token before calling the function getToken
			dsJWTAuth.checkToken(); 
			dsApiClient.setBasePath(dsJWTAuth.getBasePath());
			dsApiClient.setAccessToken(dsJWTAuth.getToken(), (long) 600);
			//dsApiClient.addDefaultHeader("Authorization", "Bearer " + dsJWTAuth.getToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(dsApiClient);


			// Create the output directory if needed 
			File file = new File( Paths.get(mainPath, "output").toString());
			if (!file.exists()) {
				if (!file.mkdir()) {
					throw new Exception(DatePretty.date() + "Failed to create directory");
				}
			} 

			byte[] results = envelopesApi.getDocument(dsJWTAuth.getAccountId(), envelopeId, "combined");
			Path path = Paths.get(mainPath,"output", DSConfig.OUTPUT_FILE_PREFIX + orderNumber + ".pdf");

			try {
				// Create the output file
				Files.write(path, results);
				// In order to see the file click F5 or right click on output folder at the Eclipse Project Explorer and refresh.
				// If the file won't open inside the Eclipse Try Window > Preferences > General > Editors > File Associations.
				// Add *.pdf if it is not there. Highlight it and then add an associated editor.
				// Select the External programs radio and then Adobe Acrobat Document or another reader program.
				// You can also open it from your computer file explorer.
			}
			// Catch exception if failed to create file
			catch(Exception e) {
				throw new Exception(DatePretty.date() + "Failed to create file");
			}
			// Catch exception if BREAK_TEST equals to true or if orderNumber contains "/break"
			if(DSConfig.ENABLE_BREAK_TEST.equals("true") && ("" + orderNumber).contains("/break")) {
				throw new Exception (DatePretty.date() + "Break test");
			}
		}
		catch (ClientHandlerException e) {
			System.err.println("ClientHandlerException: " + e.getMessage());
		}

		// Catch exception while fetching and saving docs for envelope
		catch(Exception e) {
			System.err.println(DatePretty.date() + 
					"Error while fetching and saving docs for envelope " + envelopeId + ", order " + orderNumber);
			System.err.println(e.getMessage());
			throw new Exception (DatePretty.date() + "saveDoc error");
		}
	}

	/**
	 * Process test details into files
	 * @param test contains the test number
	 * @throws Exception if failed to create directory or failed to rename a file name 
	 */
	private static void processTest(String test) throws Exception {

		// Exit the program if BREAK_TEST equals to true or if orderNumber contains "/break"
		if(DSConfig.ENABLE_BREAK_TEST.equals("true") && ("" + test).contains("/break")){
			System.err.println(DatePretty.date() + "BREAKING worker test!");
			System.exit(2);
		}

		System.out.println("Processing test value " + test.toString());

		// Create the test_messages directory if needed 
		String testDirName = DSConfig.TEST_OUTPUT_DIR_NAME;
		File testDir = new File(Paths.get(mainPath, testDirName).toString());
		if (!testDir.exists()) {
			if (!testDir.mkdir()) {
				throw new Exception(DatePretty.date() + "Failed to create directory");
			}
		}

		// First shuffle test1 to test2 (if it exists) and so on
		for(int i=9 ; i>0 ; i--) {
			File oldFile = new File(testDir, "test" + i + ".txt");
			File newFile = new File(testDir, "test" + (i+1) + ".txt");
			if(oldFile.exists() && !oldFile.equals(null)) {
				// Rename the file name - only works if newFile name does not exist
				if(newFile.exists()) {
					newFile.delete();
				}
				// Throw exception if could not rename the file name
				// Try to rename 3 times before giving up
				boolean renameWorked = false;
				for(int j=0; j<3 && !renameWorked ; j++) {
					if(!oldFile.renameTo(newFile)) {
						TimeUnit.MILLISECONDS.sleep(500);
						if(j==2) {
							throw new Exception(DatePretty.date() + "Could not rename "+ oldFile.getName() + " to " + newFile.getName());
						}
					}
					else{
						renameWorked = true;
					}
				}
			}
		}

		// The new test message will be placed in test1 - creating new file
		File newFile = new File(testDir, "test1.txt");
		FileWriter writer = new FileWriter(newFile);
		writer.write(test);
		System.out.println(DatePretty.date() + "New file created");
		writer.close();
	}
}
