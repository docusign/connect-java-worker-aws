# Java JWT authentication code example

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


## Installation

1. need to be change !!!! = Install the example 
   [Connect listener for AWS](https://github.com/docusign/connect-node-listener-aws)
   on AWS.
   At the end of this step, you will have the
   `Queue URL`, and `Queue Region`.

1. Using AWS IAM, create an IAM `User` with 
   access to your SQS queue. 
   Record the IAM user's AWS Access Key and Secret.
   Configure environment variables 
   `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` with the
   IAM user credentials.

1. Install the latest Long Term Support version of 
   Java v1.7, v1.8 or later on your system.

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

### Eclipse installation

See the [Eclipse instructions](https://github.com/docusign/eg-01-java-jwt/blob/master/docs/Readme.IntelliJ.md).
need to be changed!!!

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

#### The impersonated user's guid
The JWT will impersonate a user within your account. The user can be
an individual or a user representing a group such as "HR".

The example needs the guid assigned to the user.
The guid value for each user in your account is available from
the DocuSign Admin tool in the **Users** section.

To see a user's guid, **Edit** the user's information.
On the **Edit User** screen, the guid for the user is shown as
the `API Username`.

## Run the examples

The project's main class is `com.docusign.example.jwt.AWSWorker`

### Consent
With JWT authentication, your application will **impersonate** a
DocuSign user in your account. To do so, your integration key
must gain the **consent** of the person who will be impersonated.
There are several methods availables:

1. Recommended: use the Organization Administration feature to
   preemptively grant consent. This technique requires that the
   account have Organization administration enabled and the
   email domain of the affected users must be claimed. SSO is
   not required.
1. Grant consent individually. Consent can be obtained for
   each user by having the user follow the first leg in the
   OAuth authorization code flow. The code example prints
   the url that should be used if consent is required.

## Support, Contributions, License

Submit support questions to [StackOverflow](https://stackoverflow.com). Use tag `docusignapi`.

Contributions via Pull Requests are appreciated.
All contributions must use the MIT License.

This repository uses the MIT license, see the
[LICENSE](https://github.com/docusign/eg-01-java-jwt/blob/master/LICENSE) file.
