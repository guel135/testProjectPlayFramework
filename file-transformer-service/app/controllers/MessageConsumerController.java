package controllers;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.google.inject.Inject;

import models.User;
import play.Configuration;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;

public class MessageConsumerController implements Runnable, ExceptionListener {
	private static final String TOPIC_NAME = "miguelTopic";
	private static Thread consumerService;

	public static synchronized void initService() {
		Logger.info("Message Consumer initialized");
		MessageConsumerController MessageConsumer = new MessageConsumerController();
		if (consumerService != null) {
			Logger.info("STOPPING MessageConsumer thread.");
			consumerService.interrupt();
		}
		Logger.info("Starting MessageConsumer thread.");
		consumerService = new Thread(MessageConsumer);
		consumerService.setDaemon(true);
		consumerService.setName("MessageConsumer Service");
		consumerService.start();
		Logger.info("MessageConsumer thread started.");
	}

	@Inject
	Configuration config;

	@Override
	@Transactional
	public void run() {
		try {

			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("admin", "admin",
					"tcp://localhost:61616");

			Logger.info("Creating ActiveMQ connection");
			Connection connection = connectionFactory.createConnection();
			connection.start();
			connection.setExceptionListener(this);
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			Logger.info("Connecting to topic " + TOPIC_NAME);
			Destination destination = session.createTopic(TOPIC_NAME);

			Logger.info("Creating consumer");
			MessageConsumer consumer = session.createConsumer(destination);

			while (!Thread.currentThread().isInterrupted()) {
				Logger.info("Wait for messages...");
				Message message = consumer.receive();

				if (message instanceof TextMessage) {
					TextMessage textMessage = (TextMessage) message;
					String text = textMessage.getText();
					Logger.info("Received: " + text);
					User user = new User();
					user.setId(555);
					user.setName("miguel");
					user.setTime_of_start("time");
					Logger.info("insert user");
					
					JPA.em().getTransaction().begin();
					JPA.em().persist(user); // em.merge(u); for updates
					JPA.em().getTransaction().commit();
					
					
//					ReadFileController readfile = new ReadFileController();
//					readfile.readFileSecondOption();

				} else {
					Logger.info("Received: " + message.getClass().getSimpleName());
				}

			}
			Logger.info("Message consumer interrupted.");
			consumer.close();
			session.close();
			connection.close();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Logger.info("Message Consumer thread interrupted.");
			} else {
				Logger.error(e.getLocalizedMessage(), e);
			}
		}
	}

	public synchronized void onException(JMSException ex) {
		Logger.error("JMS Exception occured.  Shutting down client.");
		Logger.error("ErrorCode=" + ex.getErrorCode() + " , " + ex.getMessage(), ex);
	}

}