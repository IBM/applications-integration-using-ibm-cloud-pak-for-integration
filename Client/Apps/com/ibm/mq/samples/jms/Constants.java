package com.ibm.mq.samples.jms;

public class Constants {
	// Create variables for the connection to MQ
	public static final String HOST = "qm1-ibm-mq-qm-cp4i.cp4i-errortest-dal10-c3-f2c6cdc6801be85fd188b09d006f13e3-0000.us-south.containers.appdomain.cloud"; // Host name or IP address
	public static final int PORT = 443; // Listener port for your queue manager
	public static final String CHANNEL = "DEV.APP.SVRCONN"; // Channel name
	public static final String QMGR = "qm1"; // Queue manager name
	public static final String QUEUE_NAME = "REQUEST.IN"; // Queue that the application uses to put and get messages to and from
}
