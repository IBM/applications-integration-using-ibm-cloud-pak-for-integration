if [ $# -eq 0 ]; then
    echo "Provide ship tracking number"
    exit 1
fi

javac -cp ./com.ibm.mq.allclient-9.1.4.0.jar:./javax.jms-api-2.0.1.jar:./json-20210307.jar -d . com/ibm/mq/samples/jms/*.java

java -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.trustStore="../ClientKey/clientkey.jks" -Djavax.net.ssl.trustStorePassword=$2 -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -cp ./com.ibm.mq.allclient-9.1.4.0.jar:./javax.jms-api-2.0.1.jar:. com.ibm.mq.samples.jms.App1Put $1

java -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.trustStore="../ClientKey/clientkey.jks" -Djavax.net.ssl.trustStorePassword=$2 -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -cp ./com.ibm.mq.allclient-9.1.4.0.jar:./javax.jms-api-2.0.1.jar:./json-20210307.jar:. com.ibm.mq.samples.jms.App2GetPut

java -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.trustStore="../ClientKey/clientkey.jks" -Djavax.net.ssl.trustStorePassword=$2 -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -cp ./com.ibm.mq.allclient-9.1.4.0.jar:./javax.jms-api-2.0.1.jar:. com.ibm.mq.samples.jms.App1Get
