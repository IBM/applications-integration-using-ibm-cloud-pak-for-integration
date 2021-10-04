if "%1" == "" goto no_args
if "%3" == "" goto args_count_ok

:no_args
echo Provide ship tracking number
exit /b 1

javac -classpath ".\javax.jms-api-2.0.1.jar;.\com.ibm.mq.allclient-9.1.4.0.jar;.\json-20210307.jar" -d . .\com\ibm\mq\samples\jms\*.java

java -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.trustStore=".Client//ClientKey/clientkey.jks" -Djavax.net.ssl.trustStorePassword=%2 -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -classpath ".\javax.jms-api-2.0.1.jar;.\com.ibm.mq.allclient-9.1.4.0.jar;.\json-20210307.jar;." com.ibm.mq.samples.jms.RequestorPUT %1

java -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.trustStore=".Client//ClientKey/clientkey.jks" -Djavax.net.ssl.trustStorePassword=%2 -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -classpath ".\javax.jms-api-2.0.1.jar;.\com.ibm.mq.allclient-9.1.4.0.jar;.\json-20210307.jar;." com.ibm.mq.samples.jms.ResponderGetPut

java -Djavax.net.ssl.trustStoreType=jks -Djavax.net.ssl.trustStore=".Client//ClientKey/clientkey.jks" -Djavax.net.ssl.trustStorePassword=%2 -Dcom.ibm.mq.cfg.useIBMCipherMappings=false -classpath ".\javax.jms-api-2.0.1.jar;.\com.ibm.mq.allclient-9.1.4.0.jar;.\json-20210307.jar;." com.ibm.mq.samples.jms.RequestorGET
