package com.ibm.mq.samples.jms;

public class Constants {
	// Create variables for the connection to MQ
	public static final String HOST = ""; // Host name or IP address
	public static final int PORT = 443; // Listener port for your queue manager
	public static final String CHANNEL = "DEV.APP.SVRCONN"; // Channel name
	public static final String QMGR = "qm1"; // Queue manager name
	public static final String QUEUE_NAME = "REQUEST.IN"; // Queue that the application uses to put and get messages to and from
}
