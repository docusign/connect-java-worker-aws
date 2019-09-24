# Java: Connect Worker for AWS

Repository: [connect-java-worker-aws](https://github.docusignhq.com/chen-ostrovski/connect-java-worker-aws).

## Introduction

This is an example worker application for
Connect webhook notification messages sent
via the [AWS SQS (Simple Queueing System)](https://aws.amazon.com/sqs/).

This application receives DocuSign Connect
messages from the queue and then processes them:

* If the envelope is complete, the application
  uses a DocuSign JWT Grant token to retrieve
  the envelope's combined set of documents,
  and stores them in the `output` directory.
  
   For this example, the envelope **must** 
   include an Envelope Custom Field
   named `Sales order.` The Sales order field is used
   to name the output file.

## Architecture

![Connect listener architecture](docs/connect_listener_architecture.png)

AWS has [SQS](https://aws.amazon.com/tools/)
SDK libraries for C#, Java, Node.js, Python, Ruby, C++, and Go. 

## Installation

### Introduction
First, install the **Lambda listener** on AWS and set up the SQS queue.

Then set up this code example to receive and process the messages
received via the SQS queue.

### Eclipse installation

See the [Eclipse instructions](https://github.docusignhq.com/chen-ostrovski/connect-java-worker-aws/blob/master/docs/Readme.Eclipse.md)

### Installing the Lambda Listener

Install the example 
   [Connect listener for AWS](https://github.com/docusign/connect-node-listener-aws)
   on AWS.
   At the end of this step, you will have the
   `Queue URL`, `Queue Region` and `Enqueue url` that you need for the next step.

### Installing the worker (this repository)

#### Requirements
Install the latest Long Term Support version of 
Java v1.7, v1.8 or later on your system.

1. Download or clone this repository.

1. Using AWS IAM, create an IAM `User` with 
   access to your SQS queue. 
   Record the IAM user's AWS Access Key and Secret.
   Configure environment variables 
   `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` with the
   IAM user credentials.

1. Configure a DocuSign Integration Key for the application.
   The application uses the OAuth JWT Grant flow.
   If consent has not been granted to the application by
   the user, then the application provides a url
   that can be used to grant individual consent.

1. Download this repository to a directory.

1. Configure `config.properties` or set the 
   environment variables as indicated in that file.


### Short installation instructions
* Download or clone this repository.
* The project includes a Maven pom file.
* Configure the project's resource file:

  `connect-java-worker-aws/src/main/resources/config.properties` 
  See the Configuration section, below,
  for more information.
* The project's main class is
  `com.docusign.example.jwt.AWSWorker`


## Configure the example

You can configure the example either via a properties file or via
environment variables:

*  **config.properties:** In the **src/main/resources/**
   directory, edit the `config.properties` file to update
   it with your settings.
   More information for the configuration settings is below.
*  Or via **environment variables:** export the needed
   environment variables.
   The variable names in the `config.properties` file
   are the same as the needed environment variables.

**Note:** do not store your Integration Key, private key, or other
private information in your code repository.

#### Creating the Integration Key
Your DocuSign Integration Key must be configured for a JWT OAuth authentication flow:
* Using the DocuSign Admin tool,
  create a public/private key pair for the integration key.
  Store the private key
  in a secure location. You can use a file or a key vault.
* The example requires the private key. Store the private key in the
  `config.properties` file or in the environment variable
  `DS_PRIVATE_KEY`.
* Due to Java handling of multi-line strings, add the
  text `\n\` at the end of each line of the private key.
  See the example in the `config.properties` file.
* If you will be using individual consent grants, you must create a
  `Redirect URI` for the key. Any URL can be used. By default, this
  example uses `https://www.docusign.com`

````
# private key string
# NOTE: the Java config file parser requires that you 
# add \n\ at the ending of every line
# DS_PRIVATE_KEY=\n\
-----BEGIN RSA PRIVATE KEY-----\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
MIIEpAIBAAKCAXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n\
...
UC1WqwKBgQCY/6aZxlWX9XYgsQsnUjhj2aTwr7pCiQuYceIzLTQzy+nz8M4PfCE1\n\
rjRsm6YTpoxh7nuW2qnFfMA58UPs9tonN/z1pr9mKfwmamtPXeMSJeEZUVmh7mNx\n\
PEHgznlGh/vUboCuA4tQOcKytxFfKG4F+jM/g4GH9z46KZOow3Hb6g==\n\
-----END RSA PRIVATE KEY-----
````  
## Run the examples

The project's main class is `com.docusign.example.jwt.AWSWorker`

## Testing
Configure a DocuSign Connect subscription to send notifications to
the Cloud Function. Create / complete a DocuSign envelope.
The envelope **must include an Envelope Custom Field named "Sales order".**

* Check the Connect logs for feedback.
* Check the console output of this app for log output.
* Check the `output` directory to see if the envelope's
  combined documents and CoC were downloaded.

  For this code example, the 
  envelope's documents will only be downloaded if
  the envelope is `complete` and includes a 
  `Sales order` custom field.

## Unit Tests
Includes three types of testing:
* [SavingEnvelopeTest.cs](UnitTests/SavingEnvelopeTest.cs) allow you to send an envelope to your amazon sqs from the program. The envelope is saved at `output` directory although its status is `sent`.

* [RunTest.cs](UnitTests/RunTest.cs) divides into two types of tests, both submits tests for 8 hours and updates every hour about the amount of successes or failures that occurred in that hour, the differences between the two are:
    * `few` - Submits 5 tests every hour.
    * `many` - Submits many tests every hour.

In order to run the tests you need to first run the program. then choose the wanted test and run it also. 

## Support, Contributions, License

Submit support questions to [StackOverflow](https://stackoverflow.com). Use tag `docusignapi`.

Contributions via Pull Requests are appreciated.
All contributions must use the MIT License.

This repository uses the MIT license, see the
[LICENSE](https://github.com/docusign/eg-01-java-jwt/blob/master/LICENSE) file.
