/*
* (c) Copyright IBM Corporation 2018
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.ibm.mq.samples.jms;


import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import java.text.DecimalFormat;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.json.JSONObject;




/**
 * A minimal and simple application for Point-to-point messaging.
 *
 * Application makes use of fixed literals, any customisations will require
 * re-compilation of this source file. Application assumes that the named queue
 * is empty prior to a run.
 *
 * Notes:
 *
 * API type: JMS API (v2.0, simplified domain)
 *
 * Messaging domain: Point-to-point
 *
 * Provider type: IBM MQ
 *
 * Connection mode: Client connection
 *
 * JNDI in use: No
 *
 */
public class ResponderGetPut{

	// System exit status value (assume unset value to be 1)
	private static int status = 1;

	// Create variables for the connection to MQ
	private static final String IN_QUEUE_NAME = "REQUEST.OUT"; // Queue that the application uses to put and get messages to and from
	private static final String OUT_QUEUE_NAME = "REPLY.IN"; // Queue that the application uses to put and get messages to and from


	/**
	 * Main method
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("");
		System.out.println("");
		System.out.println("");
		System.out.println("Responder app - Get message");


		// Variables
		JMSContext context = null;
		Destination destination = null;
		JMSConsumer consumer = null;
		JMSProducer producer = null;


		try {
			// Create a connection factory
			JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
			JmsConnectionFactory cf = ff.createConnectionFactory();

			// Set the properties
			cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, Constants.HOST);
			cf.setIntProperty(WMQConstants.WMQ_PORT, Constants.PORT);
			cf.setStringProperty(WMQConstants.WMQ_CHANNEL, Constants.CHANNEL);
			cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, Constants.QMGR);
			cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "ResponderGetPut (JMS)");
			cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			cf.setStringProperty(WMQConstants.WMQ_SSL_CIPHER_SUITE, "TLS_RSA_WITH_AES_128_CBC_SHA256");

			// Create JMS objects
			context = cf.createContext();
			destination = context.createQueue("queue:///" + IN_QUEUE_NAME);

			consumer = context.createConsumer(destination); // autoclosable
			Message inMessage = consumer.receive(2000); // in ms or 15 seconds
			String msgID = inMessage.getJMSMessageID();
			String requestBody = inMessage.getBody(String.class);
			
			System.out.println("Request Message received by Responder app: " + requestBody);
			System.out.println("Message ID: " + msgID);
			
			JSONObject responseJSON = new JSONObject(requestBody);
			((JSONObject)responseJSON.get("tracking")).put("lat", new DecimalFormat("#.000000").format(Math.random()*361-180));
			((JSONObject)responseJSON.get("tracking")).put("lon", new DecimalFormat("#.000000").format(Math.random()*361-180));
			//System.out.println("Response json = " + responseJSON.toString());
			
			System.out.println("");
			System.out.println("");
			System.out.println("");
			System.out.println("Responder app - Put message");
			

			destination = context.createQueue("queue:///" + OUT_QUEUE_NAME);
			TextMessage message = context.createTextMessage(responseJSON.toString());
			
			message.setJMSCorrelationID(msgID);
			producer = context.createProducer();
			producer.send(destination, message);
			
			System.out.println("Reply Message sent by Responder app:\n" + message);

            context.close();

			recordSuccess();
		} catch (JMSException jmsex) {
			recordFailure(jmsex);
		}

		System.exit(status);

	} // end main()
	

	/**
	 * Record this run as successful.
	 */
	private static void recordSuccess() {
		System.out.println("SUCCESS");
		status = 0;
		return;
	}

	/**
	 * Record this run as failure.
	 *
	 * @param ex
	 */
	private static void recordFailure(Exception ex) {
		if (ex != null) {
			if (ex instanceof JMSException) {
				processJMSException((JMSException) ex);
			} else {
				System.out.println(ex);
			}
		}
		System.out.println("FAILURE");
		status = -1;
		return;
	}

	/**
	 * Process a JMSException and any associated inner exceptions.
	 *
	 * @param jmsex
	 */
	private static void processJMSException(JMSException jmsex) {
		System.out.println(jmsex);
		Throwable innerException = jmsex.getLinkedException();
		if (innerException != null) {
			System.out.println("Inner exception(s):");
		}
		while (innerException != null) {
			System.out.println(innerException);
			innerException = innerException.getCause();
		}
		return;
	}

}