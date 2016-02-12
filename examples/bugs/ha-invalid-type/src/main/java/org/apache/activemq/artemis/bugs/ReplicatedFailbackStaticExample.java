package org.apache.activemq.artemis.bugs;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.util.ServerUtil;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.HashMap;

/**
 * Created by abprabhakar on 2/11/16.
 */
public class ReplicatedFailbackStaticExample {


    private static Process server0;

    private static Process server1;

    public static void main(final String[] args) throws Exception {
        final int numMessages = 30;

        Connection connection = null;


        try {
            server0 = ServerUtil.startServer(args[0], ReplicatedFailbackStaticExample.class.getSimpleName() + "0", 0, 30000);
            server1 = ServerUtil.startServer(args[1], ReplicatedFailbackStaticExample.class.getSimpleName() + "1", 1, 30000);


            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put("host", "localhost");
            map.put("port", "61616");
            TransportConfiguration server1 = new TransportConfiguration(NettyConnectorFactory.class.getName(), map);
            HashMap<String, Object> map2 = new HashMap<String, Object>();
            map2.put("host", "localhost");
            map2.put("port", "61617");
            TransportConfiguration server2 = new TransportConfiguration(NettyConnectorFactory.class.getName(), map2);

            ActiveMQConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithHA(JMSFactoryType.CF, server1, server2);
            connectionFactory.setRetryInterval(1000);
            connectionFactory.setReconnectAttempts(-1);
            connectionFactory.setRetryIntervalMultiplier(1.0);

            // Step 1. Directly instantiate the JMS Queue object.
            Queue queue = ActiveMQJMSClient.createQueue("exampleQueue");


            // Step 3. Create a JMS Connection
            connection = connectionFactory.createConnection();

            // Step 4. Create a *non-transacted* JMS Session with client acknowledgement
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            // Step 5. Start the connection to ensure delivery occurs
            connection.start();

            // Step 6. Create a JMS MessageProducer and a MessageConsumer
            MessageProducer producer = session.createProducer(queue);
            MessageConsumer consumer = session.createConsumer(queue);

            // Step 7. Send some messages to server #1, the live server
            for (int i = 0; i < numMessages; i++) {
                TextMessage message = session.createTextMessage("This is text message " + i);
                producer.send(message);
                System.out.println("Sent message: " + message.getText());
            }

            // Step 8. Receive and acknowledge a third of the sent messages
            TextMessage message0 = null;
            for (int i = 0; i < numMessages / 3; i++) {
                message0 = (TextMessage) consumer.receive(5000);
                System.out.println("Got message: " + message0.getText());
            }
            message0.acknowledge();

            // Step 9. Receive the rest third of the sent messages but *do not* acknowledge them yet
            for (int i = numMessages / 3; i < numMessages; i++) {
                message0 = (TextMessage) consumer.receive(5000);
                System.out.println("Got message: " + message0.getText());
            }

            // Step 10. Crash server #0, the live server, and wait a little while to make sure
            // it has really crashed
            ServerUtil.killServer(server0);

            // Step 11. Acknowledging the 2nd half of the sent messages will fail as failover to the
            // backup server has occurred
            try {
                message0.acknowledge();
            } catch (JMSException e) {
                System.out.println("Got (the expected) exception while acknowledging message: " + e.getMessage());
            }

            // Step 12. Consume again the 2nd third of the messages again. Note that they are not considered as redelivered.
            for (int i = numMessages / 3; i < (numMessages / 3) * 2; i++) {
                message0 = (TextMessage) consumer.receive(5000);
                System.out.printf("Got message: %s (redelivered?: %s)\n", message0.getText(), message0.getJMSRedelivered());
            }
            message0.acknowledge();

            server0 = ServerUtil.startServer(args[0], ReplicatedFailbackStaticExample.class.getSimpleName() + "0", 0, 10000);

            // Step 11. Acknowledging the 2nd half of the sent messages will fail as failover to the
            // backup server has occurred
            try {
                message0.acknowledge();
            } catch (JMSException e) {
                System.err.println("Got exception while acknowledging message: " + e.getMessage());
            }

            // Step 12. Consume again the 2nd third of the messages again. Note that they are not considered as redelivered.
            for (int i = (numMessages / 3) * 2; i < numMessages; i++) {
                message0 = (TextMessage) consumer.receive(5000);
                System.out.printf("Got message: %s (redelivered?: %s)\n", message0.getText(), message0.getJMSRedelivered());
            }
            message0.acknowledge();
        } finally {
            // Step 13. Be sure to close our resources!

            if (connection != null) {
                connection.close();
            }

            ServerUtil.killServer(server0);
            ServerUtil.killServer(server1);
        }
    }
}
