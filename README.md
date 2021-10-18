# Integrate your on-premises applications with IBM Cloud Pak for Integration

[This code pattern](code pattern overview on IBM Developer) demonstrates how to connect your cloud and on-premises applications and deliver messages reliably with enterprise-grade messaging and integration features of IBM Cloud Pak for Integration(CP4I). 

In this code pattern, you will look at a shipment tracking scenario. The details are as follows.
- There are two client applications, a requestor application and a responder applications. The requester client application requests for shipment tracking data. And the responder application provides shipment tracking data. 
- The requester client application sends a request message, in XML format, for shipment status. 
- The responder client application accepts request messages in JSON format and responds with shipment tracking details in JSON format. 
- The communication mechanism between requester and responder applications happen asynchronously.

These requestor and responder applications use IBM Cloud Pak for Integration to achieve reliable and secure messaging, app integration features that supports the full breadth of integration needs across a modern digital enterprise, in this case it being used for the following:
- To host an application integration flow (developed using [App Connect Enterprise toolkit](https://www.ibm.com/docs/en/app-connect/11.0.0?topic=overview-app-connect-enterprise-toolkit)).
- To transform XML messages to JSON and vice-versa using low-code approach.

When the reader has completed this pattern, they will understand how to:
- Setup messaging (MQ queue manager) security with [TLS](https://www.ibm.com/docs/en/ibm-mq/9.0?topic=mechanisms-tls-security-protocols-in-mq).
- Deploy application integration (App Connect) flows.
- Integrate applications, outside of CP4I cluster, messaging as well as application integration capabilities of CP4I.
- Run simple Java JMS applications to test the complete set up.

<!-- Include this image if required ![](./images/architecture-high-level.png) -->
![](./images/architecture-low-level.png)

## Flow
1. Requester app puts a shipping tracking request, in XML format, to messaging queue (REQUEST.IN).
2. App integration flow picks requester app request from MQ queue (REQUEST.IN) and transforms XML message to JSON message.
3. App integration flow puts the JSON message to an MQ queue (REQUEST.OUT).
4. Responder app picks the JSON message from the MQ queue (REQUEST.OUT) and prepares a response message.
5. Responder app puts the response message, in JSON format, to an MQ Queue (REPLY.IN).
6. App integration flow picks the response message from Queue (REPLY.IN), transforms the message from JSON to XML format.
7. App integration flow puts the XML response message to an MQ queue (REPLY.OUT).
8. Requester app picks the response message from the MQ queue (REPLY.OUT).

## Prerequisites
- [IBM Cloud Pak for Integration (CP4I) v2021.2.1-0](https://cloud.ibm.com/docs/cloud-pak-integration?topic=cloud-pak-integration-getting-started)
- [OpenSSL](https://www.openssl.org/source/)
- [Java JDK](https://www.oracle.com/java/technologies/downloads/)
- [git client](https://git-scm.com/downloads)
- [oc client](https://docs.openshift.com/container-platform/4.8/cli_reference/openshift_cli/getting-started-cli.html)
- [Windows Subsystem for Linux](https://docs.microsoft.com/en-us/windows/wsl/install) (Optional)

It is assumed that you have basic familiarity with above tools/technologies required for following this code pattern.

## Steps
1. [Clone the repository](#1-clone-the-repository)
2. [Create TLS objects](#2-create-tls-objects)
3. [Create an instance of MQ Queue Manager](#3-create-an-instance-of-mq-queue-manager-in-cp4i)
4. [Configure MQ instance for TLS](#4-configure-mq-instance-for-tls)
5. [Connect to your MQ instance from outside the cluster](#5-connect-to-your-mq-instance-from-outside-the-cluster)
6. [Deploy App integration flows](#6-deploy-app-integration-flows)
7. [Test the complete set up](#7-test-the-set-up)

### 1. Clone the repository
Clone the repository using the below command
```
git clone https://github.com/IBM/applications-integration-using-ibm-cloud-pak-for-integration.git
```

### 2. Create TLS objects
TLS is used in MQ to secure channels. The TLS handshake enables the TLS client and server to establish the secret keys with which they communicate. Let us create a self-signed certificate and extract the public key from it for the client to use. A self-signed certificate is signed with its own private key.

#### 2.1 Create self signed certificates for server and client

In a terminal window, change directory to parent folder of cloned repository.

Create self signed certificate for the server using the below command
```
openssl req -newkey rsa:2048 -nodes -keyout server.key -x509 -days 365 -out server.crt
```

You will be prompted to enter some information. Enter whatever you like. An example is provided below.
```
Country Name (2 letter code) []:IN
State or Province Name (full name) []:KA
Locality Name (eg, city) []:BLR
Organization Name (eg, company) []:Example
Organizational Unit Name (eg, section) []:Abc
Common Name (eg, fully qualified host name) []:example.com
Email Address []:abc@example.com
```

Verify the certificate has been created successfully with this command:
```
openssl x509 -text -noout -in server.crt
```

Similarly create self signed certificate for the client using the below command
```
openssl req -newkey rsa:2048 -nodes -keyout client.key -x509 -days 365 -out client.crt
```

### 3. Create an instance of MQ Queue Manager in CP4I
The MQ in Cloud Pak For Integration cluster is accessed by applications that are deployed outside the cluster. This requires TLS to be set up while the MQ queue manager is being created. The self signed certificates we created will be used to create secrets in our OpenShift cluster. These secrets will be used while creating an instance of Queue Manager.

#### 3.1 Set up TLS Certificates for MQ Deployment
For the sake of this code pattern, the CP4I instance is deployed in a namespace called `cp4i`.

**Create Server Secret**
1. Navigate to OpenShift console (where CP4I is deployed).
2. Click on `Workloads` -> `Secrets`. Select project `cp4i`.
3. Click on `Create` -> `Key/value secret`.
![](./images/secrets.png)
4. Fill the form as the following information:
- Secret Name: `mq-server`.
- Key: `tls.key`.
- Value: Click `Browse` and select server.key that was generated in step [Clone the repository](#1-clone-the-repository).
- Click on `Add Key/Value`
- Key: `tls.crt`.
- Value: Click `Browse` and select server.crt that was generated in step [Clone the repository](#1-clone-the-repository).
5. Click `Create`.
![](./images/create-secrets.png)

**Create Client Secret**
1. Navigate to OpenShift console (where CP4I is deployed).
2. Click on `Workloads` -> `Secrets`. Select project `cp4i`.
3. Click on `Create` -> `Key/value secret`.
4. Fill the form as the following information:
- Secret Name: `mq-client`.
- Key: `tls.crt`.
- Value: Click `Browse` and select client.crt that was generated in step [Clone the repository](#1-clone-the-repository).
> tls.key and tls.crt will be created for mq-server, whereas, only tls.crt will be created for mq-client. 

#### 3.2 Create an instance of MQ Queue Manager
1. Navigate to OpenShift console (where CP4I is deployed).
2. Click `operators` -> `Installed Operators`.
3. Click on `IBM MQ` operator. You may search for the operator by typing `IBM MQ` in search field. Click on `IBM MQ` link.
![](./images/installed-operator-mq.png)
4. Click on `Queue Manager` tab.
![](./images/qm-tab.png)
5. Click `Create QueueManager` button available on the right side of the screen.
6. Fill form details as follows:
- Enter name as `qm1`.
- Read and accept the license.
![](./images/mq-form-1.png)

- [Storage considerations](https://www.ibm.com/docs/en/ibm-mq/9.2?topic=containers-storage-considerations-mq-operator) -> For the sake of this content, we will create MQ queue manager using the default setting of ephemeral storage. If you decide to use persistent storage, you will need to make appropriate changes in the settings.

- Click `Advanced Configuration` at the end of the page.
- Click on `PKI` -> `Keys` -> expand `Secret` -> `Advanced configuration` -> `Items` -> `Add Item`. Enter `Value` as `tls.key`. 
- Click `Add Item` link below. Enter `Value` as tls.crt. Select Secret `mq-server` from the dropdown.
![](./images/mq-tls-config.png)
- Click `Advanced Configuration` below and enter `Name` as `default`.
- Next click on `Trust` -> expand `Secret` -> `Add Item. Enter `Value` as `tls.crt`.
- Under `Secret name` dropdown select `mq-client`.
- Click `Advanced Configuration` below and enter `Name` ad `label2`.
- Scroll to the top of the page and select `YAML view` option. It should look as shown below. Make further changes, if required, either in form view or YAML view.

![](./images/qm1-create.gif)

Alternatively, copy paste the below yaml content in yaml view and make necessary changes wherever required.
```
apiVersion: mq.ibm.com/v1beta1
kind: QueueManager
metadata:
  name: qm1
  namespace: cp4i
spec:
  license:
    accept: true
    license: L-RJON-BXUPZ2
    use: NonProduction
  pki:
    keys:
      - name: default
        secret:
          items:
            - tls.key
            - tls.crt
          secretName: mq-server
    trust:
      - name: label2
        secret:
          items:
            - tls.crt
          secretName: mq-client
  web:
    enabled: true
  version: 9.2.2.0-r1
  template:
    pod:
      containers:
        - env:
            - name: MQSNOAUT
              value: 'yes'
          name: qmgr
  queueManager:
    name: qm1
    storage:
      queueManager:
        type: ephemeral
```
7. Click `Create`.

Queue Manager should be created and be in running status.
![](./images/qm-running.png)

> If there is any issue with data provided for creating a queue manager, then the status of queue manager remains pending even after several minutes. In such a case it is recommended to delete the queue manager and create a new one ensuring you provide correct data.

### 4. Configure MQ instance for TLS

Once your MQ instance is up and running, configure it for authentication.

Launch CP4I platform Navigator:
- Navigate to OpenShift web console -> Click 'applications menu' and Click on `Cloud Pak Administration Hub`.
![](images/applications-menu.png)
- Login to IBM Automation using the `IBM provided credentials (admin only)`. This will be the credentials that was provided when deploying Cloud Pak for Integration.
-  On the top right corner of the dashboard, click on the nine dots icon, then click on `IBM Automation (cp4i)` to open Platform Navigator.

![](images/cp4i-dashboard-link.png)

<details>
  <summary>An alternate method to open Platform Navigator</summary>

- Navigate to OpenShift web console -> `Installed Operators` under `cp4i` namespace and click `IBM Cloud Pak for Integration Platform Navigator` entry.
  ![](./images/platform-nav-oper.png)
- Click `Platform Navigator` tab of `IBM Cloud Pak for Integration Platform Navigator` operator. Then click on the platform navigator entry there.
![](./images/plat-nav-tab.png)
- Under `Details` tab, click the link `Platform Navigator UI` to launch platform navigator.
- Login to IBM Automation using the `IBM provided credentials (admin only)`. This will be the credentials that was provided when deploying Cloud Pak for Integration.
</details>

In platform Navigator dashboard, under the tile `Messaging` tile, click the queue manager that you created in previous step [Create an instance of MQ Queue Manager](#32-create-an-instance-of-mq-queue-manager). Queue Manager home page is displayed.

The first time you attempt to access the MQ console a warning will appear regarding the certificate. This can be accepted as it is normally due to a self-signed certificate, or a unknown certificate authority. In a real production environment this would commonly be configured with a certificate that would be known by the browser.

1. Click on `Manage` -> `View configuration`.
![](./images/qm-config-link.png)

2. Click `Edit`.
- Under `Communication` menu on the left hand side, select `CHLAUTH records` and select `Disabled` option. CHLAUTH feature
allows you to set rules to indicate which inbound connections are allowed to use your queue manager and which are banned. We will not use this feature for this code pattern.
- Under `Extended` menu on the left hand side, delete the text under the field `Connection authentication`. It is used for authentication using username and password and since we are not using that we'll delete the details.
![](./images/mq-conn-auth.png)
- Click `Save`.

3. Create [channel](https://www.ibm.com/docs/en/ibm-mq/9.1?topic=types-channels).

A channel is a logical communication link, used by distributed queue managers, between an IBM MQ MQI client and an IBM MQ server, or between two IBM MQ servers. In this code pattern, we will create channels to enable communication between a) queue manager and client applications b) queue manager and app integration flows. 
- In queue manager console, click `Manage` -> `Communication` tab -> `App channels`. Click `Create`.
![](./images/app-channels-link.png)
- Click `Next`. Enter `Channel name` as `DEV.APP.SVRCONN`. Click `Create`.
![](./images/app-channel-create.png)
- Follow the same steps to create another channel `DEV.ACE.SVRCONN`.
> Note that we are creating two channels because Queue manager connects App integration flow as well as Java JMS clients. App integration flows are within the CP4I cluster and Java JMS clients are outside the CP4I clusters. We will setup different configurations to these channels to connect to different types of client. Note that the same channel can be configured for different types of client, but we'll not do that in this code pattern.
> `DEV.APP.SVRCONN` channel will be used by Java JMS clients and will be configured with TLS. `DEV.ACE.SVRCONN` channel will be used by App integration flows and we will leave this channel with default settings.

4. Configure channel
- On the queue manager console, view configuration of the just created channel.
![](./images/channel-view-config.png)
- Under `Select a queue manager`, select the queue manager qm1. Under `Select an application channel` select the channel `DEV.APP.SVRCONN`. Under `Select a cipher specification` select `ANY_TLS12`.
- Click `Edit` and select `SSL` on the left menu items. Under `SSL Cipher spec`, selection the option `ANY_TLS12`. Select `Optional` for `SSL Authentication`.

`SSL Cipher spec` - specifies the encryption strength and function (CipherSpec). The CipherSpec must match at both ends of channel. The client and the queue manager.
`SSL Authentication` - Defines whether MQ requires and validates a certificate from the SSL client. Since we are not using mutual authentication, so ‘SSL authentication’ has been made ‘Optional’. Setting it to ‘Optional’ will disable the client authentication.


The ability of IBM MQ classes for JMS applications to establish connections to a queue manager, depends on the CipherSpec specified at the server end of the MQI channel and the CipherSuite specified at the client end.
![](./images/channel-ssl-config.png)
- Click `Save`.

5. Create local queues.
- In queue manager console, click `Queues` tab. Click `Create`.
![](./images/q-create-link.png)
- Click on `Local`. For `Queue name` enter `REQUEST.IN` and click `Create`.
![](./q-create)
- Similarly create other queues required for this code pattern demonstration.
`REQUEST.OUT`
`REPLY.IN`
`REPLY.OUT`
`REQUEST.IN.FAILURE`
`REQUEST.OUT.FAILURE`
`REPLY.IN.FAILURE`
`REPLY.OUT.FAILURE`

Make sure that you `Refresh Security` of the Queue Manager after making the security related changes. Go to the Queue Manager configuration and refresh all three types of securities one after another.
![](./images/refresh-security.png)

6. Get connection details
- Go to queue manager configuration. Under `Actions`, click `Download connection file`.
- Click next and make a note of hostname and port. These details are required later for client applications to connect to MQ. Then Click on next and click create.


### 5. Connect to your MQ instance from outside the cluster
Client applications that set the [SNI](https://en.wikipedia.org/wiki/Server_Name_Indication) to the MQ channel require a new OpenShift Route to be created for each channel you wish to connect to. You also have to use unique channel names across your Red Hat OpenShift cluster, to allow routing to the correct queue manager.

This is an example of the channel based SNI mapping:
| Channel         | SNI                 | suffix          |
| -----------     | -----------         | -----------     |
| DEV.APP.SVRCONN | dev2e-app2e-svrconn | chl.mq.ibm.com  |

Use this link to determine the SNI Mapping: [How does MQ provide multiple certificates (CERTLABL) capability](https://www.ibm.com/support/pages/ibm-websphere-mq-how-does-mq-provide-multiple-certificates-certlabl-capability)

We additionally need to create a route for each channel we want to connect to. The HostName of the new route will be the name of the channel mapped to it's SNI format (using the SNI mapping rules) with the suffix chl.mq.ibm.com.

Use the following yaml to create the correct route for DEV.APP.SVRCONN.
From a terminal, log on to your OpenShift cluster and run the below command

```
cat << EOF | oc -n cp4i apply -f -

apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: qm1-ibm-mq-qm-traffic-dev
  namespace: cp4i
spec:
  host: dev2e-app2e-svrconn.chl.mq.ibm.com
  to:
    kind: Service
    name: qm1-ibm-mq
  port:
    targetPort: 1414
  tls:
    termination: passthrough
EOF
```

Use `oc get route -n cp4i | grep qm1-ibm-mq-qm-traffic-dev` to verify that the above route is created.

### 6. Deploy App Integration flows
Ensure that App Connect is deployed in the CP4I instance that you are using. To verify, goto Operators > Installed Operators and search for App Connect. You should  see the App Connect operator.

The integration application has two flows, request flow and response flow. The request flow transforms XML message to JSON message. The response flow transforms JSON message to XML. 

The integration flow is provided as a .bar file (`<cloned repo parent directory>/ACE/appint.bar`) as well as a project interchange file (`<cloned repo parent directory>/ACE/pi.zip`). If you want to understand the details of the integration flows, you can import the project interchange file into [App Connect Enterprise toolkit](https://www.ibm.com/docs/en/app-connect/11.0.0?topic=overview-app-connect-enterprise-toolkit). Here you can explore the flows and integrations. You may make edits, if you want. In case you edit, you will need to update the appint.bar, located at `<cloned repo parent directory>/ACE`, file with your changes.

The default appint.bar file is preconfigured with default values used in this code pattern to connect to MQ. The appint.bar file won't work as expected if the names of Queue manager, queues and channel are different from the ones used in this code pattern.

Launch CP4I platform Navigator as explained in section [Configure MQ instance for TLS](#4-configure-mq-instance-for-tls). In platform navigator dashboard, under `Integrations` tile, click on the entry available there, something like `db-01-quickstart`. App Connect Dashboard opens.

- Click on the tile `Create a server` and enter the following specifications:
- **Types** - `Quick start toolkit integration`. Click `Next`.
- **Integrations** - You need to provide a bar file. You can either drag and drop or click on the `Drag and drop a BAR file or click to upload` link. Upload the bar file `appint` placed in `<cloned repo parent directory>/ACE` folder. Click `Next`. 
- **Configurations** - Skip and click next.
- **Server** - You may leave the default settings and click `Create`.
![](./images/is-02.gif)

- The integration server will take a few minutes to start. Wait for 3-4 minutes and **refresh the browser page**. The status of integration server should be `Ready`.

![](./images/is-status.png)

### 7. Test the set up
To send/receive messages to/from MQ queues, sample applications are provided in this code pattern. These sample applications are JMS Java applications. These files are located in the directory `<cloned repo parent directory>/Client/Apps/com/ibm/mq/samples/jms`

![](./images/java-files.png)

- `RequestorPUT.java` puts a shipping tracking request message, in XML format, to the queue REQUEST.IN.
- `ResponderGetPut.java` takes JSON request message from REQUEST.OUT. It builds a response message, in JSON format, and puts the response message in REPLY.IN queue.
- `RequestorGET.java` gets response message, in XML format, from REPLY.OUT queue.
- `Constants.java` holds connection details to queue manager.

**Update connection details in Constants.java file**. You can get connection details as explained in section [Configure MQ instance for TLS](#4-configure-mq-instance-for-tls).

**Create a client keystore**
The Java JMS application should provide valid certificate details for it to communicate with Queue Manager. In earlier step [Create self signed certificates for server and Client](#21-create-self-signed-certificates-for-server-and-client) we created server key and certificate. We will create a client keystore using that server certificate. We will use keytool (a Java security tool), which is included with Java JREs and SDKs.

On a terminal window change directory to where `server.key` and `server.crt` are placed. To create a .jks client keystore and import our server certificate into it, run the following command in the terminal:
```
keytool -keystore ./Client/ClientKey/clientkey.jks -storetype jks -importcert -file server.crt -alias server-certificate
```
You will be prompted to create a password. Be sure to remember the password that you set as you’ll need it later on. Also, you are prompted whether to trust the certificate. Type yes and hit enter. `Trust this certificate? [no]:  yes`. 

Now that the client keystore is available, let us run the application. 

<details>
  <summary>Mac/Linux users</summary>
The script "cloned repo parent directory/Client/Apps/runClientApps.sh" will compile and run these java files. This script takes 2 arguments. First argument is a shipment number, it can be any string for the purpose of this demo. The second argument is the password that you specified while creating client kaystore (.jks file).

On a terminal window, change directory to "cloned repo parent directory/Client/Apps" folder of the cloned git repository and run the script. 
```
cd <cloned repo parent directory>/Client/Apps/
./runClientApps.sh ship001 <password>
```
</details>

<details>
  <summary>Windows users</summary>
You can either use the batch file script or install [WSL](https://docs.microsoft.com/en-us/windows/wsl/install).
If you use WSL, then refer to Mac instructions. Otherwise, the script "cloned repo parent directory/Client/Apps/runClientApps.bat" will compile and run these java files. This script takes 2 arguments. First argument is a shipment number, it can be any string for the purpose of this demo. The second argument is the password that you specified while creating client kaystore (.jks file).

On a terminal window, change directory to "cloned repo parent directory\Client\Apps" folder of the cloned git repository and run the script. 
```
cd <cloned repo parent directory>\Client\Apps
./runClientApps.bat ship001 <password>
```
</details>


Observe the console log messages. You will notice that 
1. Requestor app puts a request message, in XML format, with the shipment number provided, to the queue REQUEST.IN.
2. Responder app picks the request message, in JSON format, from the queue REQUEST.OUT.
3. Responder app sends a response message, in JSON format, to the queue REPLY.IN.
4. Requestor app picks the response message, in XML format, from the queue REPLY.OUT.

>Note: For demonstration purpose these apps exit after they find messages in MQ queue or 15 seconds, whichever is sooner. In real scenarios the applications constantly monitor queues for messages and could process multiple messages. 

A sample output is provided in file `<cloned repo parent directory>/Client/Apps/sample-output.txt`.

**Using the correlation ID**
A correlation ID is used to correlate response messages with request messages when an application invokes a request-response operation. With MQ and JMS, you can correlate using either a correlation ID or a message ID. In most cases, the caller lets the queue manager select a message ID and expects the application to copy this message ID into the correlation ID of the response message. But there are other possibilities. A caller could specify a specific value in the correlation ID and expect this value to be copied into the response correlation ID. The caller might also require that the message ID of the request message be copied to the message ID of the response message.

When you configure the MQ or MQ JMS bindings, you are given selections that reflect the basic choice of copying the correlation ID to the response correlation ID or copying the message ID to the response correlation ID.


## Summary
This code pattern demonstrated how you can setup Messaging and Integration components in CP4I so that applications external to CP4I cluster. It also demonstrated how messaging and integration components can be integrated. End-to-end integration of applications using CP4I was done. You may follow the steps to build your own custom applications integration with similar requirements.

## License
This code pattern is licensed under the Apache License, Version 2. Separate third-party code objects invoked within this code pattern are licensed by their respective providers pursuant to their own separate licenses. Contributions are subject to the [Developer Certificate of Origin, Version 1.1](https://developercertificate.org/) and the [Apache License, Version 2](https://www.apache.org/licenses/LICENSE-2.0.txt).

[Apache License FAQ](https://www.apache.org/foundation/license-faq.html#WhatDoesItMEAN)
