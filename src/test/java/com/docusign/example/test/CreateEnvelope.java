package com.docusign.example.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.sun.jersey.core.util.Base64;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.model.*;
import com.docusign.example.worker.DSConfig;
import com.docusign.example.worker.JWTAuth;


class CreateEnvelope extends JWTAuth {

    private static final String DOC_2_DOCX = "World_Wide_Corp_Battle_Plan_Trafalgar.docx";
    
    private static final String DOC_3_PDF = "World_Wide_Corp_lorem.pdf";
    private static final String ENVELOPE_1_DOCUMENT_1 = "<!DOCTYPE html>" +
            "<html>" +
            "    <head>" +
            "      <meta charset=\"UTF-8\">" +
            "    </head>" +
            "    <body style=\"font-family:sans-serif;margin-left:2em;\">" +
            "    <h1 style=\"font-family: 'Trebuchet MS', Helvetica, sans-serif;" +
            "         color: darkblue;margin-bottom: 0;\">World Wide Corp</h1>" +
            "    <h2 style=\"font-family: 'Trebuchet MS', Helvetica, sans-serif;" +
            "         margin-top: 0px;margin-bottom: 3.5em;font-size: 1em;" +
            "         color: darkblue;\">Order Processing Division</h2>" +
            "  <h4>Ordered by " + DSConfig.SIGNER_NAME + "</h4>" +
            "    <p style=\"margin-top:0em; margin-bottom:0em;\">Email: " + DSConfig.SIGNER_EMAIL + "</p>" +
            "    <p style=\"margin-top:0em; margin-bottom:0em;\">Copy to: " + DSConfig.CC_NAME + ", " + DSConfig.SIGNER_EMAIL + "</p>" +
            "    <p style=\"margin-top:3em;\">" +
            "  Candy bonbon pastry jujubes lollipop wafer biscuit biscuit. Topping brownie sesame snaps" +
            " sweet roll pie. Croissant danish biscuit soufflé caramels jujubes jelly. Dragée danish caramels lemon" +
            " drops dragée. Gummi bears cupcake biscuit tiramisu sugar plum pastry." +
            " Dragée gummies applicake pudding liquorice. Donut jujubes oat cake jelly-o. Dessert bear claw chocolate" +
            " cake gummies lollipop sugar plum ice cream gummies cheesecake." +
            "    </p>" +
            "    <!-- Note the anchor tag for the signature field is in white. -->" +
            "    <h3 style=\"margin-top:3em;\">Agreed: <span style=\"color:white;\">**signature_1**/</span></h3>" +
            "    </body>" +
            "</html>";

            
    public CreateEnvelope(ApiClient apiClient) throws IOException {
        super(apiClient);
    }

    /**
     * method show the usage of
     * @return
     * @throws ApiException
     * @throws IOException
     */
    public EnvelopeSummary sendEnvelope() throws ApiException, IOException {

        this.checkToken();

        EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
        envelopeDefinition.setEmailSubject("Document sent from the Test Mode");

        Document doc1 = new Document();
        doc1.setDocumentBase64(new String(Base64.encode(ENVELOPE_1_DOCUMENT_1.getBytes())));
        doc1.setName("Order acknowledgement");
        doc1.setFileExtension("html");
        doc1.setDocumentId("1");

        Document doc2 = new Document();
        doc2.setDocumentBase64(new String(Base64.encode(DSHelper.readContent(DOC_2_DOCX))));
        doc2.setName("Battle Plan");
        doc2.setFileExtension("docx");
        doc2.setDocumentId("2");

        Document doc3 = new Document();
        doc3.setDocumentBase64(new String(Base64.encode(DSHelper.readContent(DOC_3_PDF))));
        doc3.setName("Lorem Ipsum");
        doc3.setFileExtension("pdf");
        doc3.setDocumentId("3");

        // The order in the docs array determines the order in the envelope
        envelopeDefinition.setDocuments(Arrays.asList(doc1, doc2, doc3));
        // create a signer recipient to sign the document, identified by name and email
        // We're setting the parameters via the object creation
        Signer signer1 = new Signer();
        signer1.setEmail(DSConfig.SIGNER_EMAIL);
        signer1.setName(DSConfig.SIGNER_NAME);
        signer1.setRecipientId("1");
        signer1.setRoutingOrder("1");
        // routingOrder (lower means earlier) determines the order of deliveries
        // to the recipients. Parallel routing order is supported by using the
        // same integer as the order for two or more recipients.

        
        // Create signHere fields (also known as tabs) on the documents,
        // We're using anchor (autoPlace) positioning
        //
        // The DocuSign platform seaches throughout your envelope's
        // documents for matching anchor strings. So the
        // sign_here_2 tab will be used in both document 2 and 3 since they
        // use the same anchor string for their "signer 1" tabs.
        SignHere signHere1 = new SignHere();
        signHere1.setAnchorString("**signature_1**");
        signHere1.setAnchorUnits("pixels");
        signHere1.setAnchorXOffset("20");
        signHere1.anchorYOffset("10");

        SignHere signHere2 = new SignHere();
        signHere2.setAnchorString("/sn1/");
        signHere2.setAnchorUnits("pixels");
        signHere2.setAnchorXOffset("20");
        signHere2.anchorYOffset("10");
        // Tabs are set per recipient / signer
        Tabs tabs = new Tabs();
        tabs.setSignHereTabs(Arrays.asList(signHere1, signHere2));
        signer1.setTabs(tabs);
        // Add the recipients to the envelope object
        Recipients recipients = new Recipients();
        recipients.setSigners(Arrays.asList(signer1));
        envelopeDefinition.setRecipients(recipients);
   
        // Request that the envelope be sent by setting |status| to "sent".
        // To request that the envelope be created as a draft, set to "created"
        envelopeDefinition.setStatus("sent");
        
        // Add the customFields to the envelope
        TextCustomField textCustomField = new TextCustomField();
        textCustomField.setValue("Test_Mode");
        textCustomField.setShow("true");
        textCustomField.setName("Sales order");
        CustomFields customFields = new CustomFields();
        List<TextCustomField> textCustomFields = new ArrayList<TextCustomField>();
        textCustomFields.add(textCustomField);
		customFields.setTextCustomFields(textCustomFields );
    	envelopeDefinition.setCustomFields(customFields);
    	
        
        EnvelopesApi envelopeApi = new EnvelopesApi(this.apiClient);
        EnvelopeSummary results = envelopeApi.createEnvelope(this.getAccountId(), envelopeDefinition);

        return results;
    }

}
