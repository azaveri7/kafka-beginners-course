package com.github.kafka.kafka_beginners_course;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoWithThreads {

	private ConsumerDemoWithThreads() {		
	}
	
	private void run() {
		String bootStrapServers = "127.0.0.1:9092";

		Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);
		String topic = "first_topic";
		String groupId = "my_fifth_application";
		
		// latch for dealing with multithreading
		CountDownLatch latch = new CountDownLatch(1);
		
		logger.info("Creating the consumer thread...");
		
		Runnable myConsumerRunnable = 
				new ConsumerRunnable(latch, bootStrapServers, 
						groupId, topic);
		
		// start the thread
		Thread myConsumerThread = new Thread(myConsumerRunnable);
		myConsumerThread.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
				logger.info("caught shutdown hook");
				((ConsumerRunnable)myConsumerRunnable)
					.shutdown();
				try {
					latch.await();
				} catch (InterruptedException e) {
					logger.info("application is exited");
				}
			}));
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			logger.info("application is interrupted", e);
			e.printStackTrace();
		} finally {
			logger.info("application is closing");
		}		
	}
	
	public static void main(String[] args) {
		new ConsumerDemoWithThreads().run();
	}

	public class ConsumerRunnable implements Runnable{

		private CountDownLatch latch;
		private KafkaConsumer<String, String> consumer;
		private Logger logger = 
				LoggerFactory.getLogger(ConsumerRunnable.class);
		
		public ConsumerRunnable(CountDownLatch latch, 
				String bootStrapServers, 
				String groupId, 
				String topic) {
			this.latch = latch;
			
			// create consumer properties
			Properties properties = new Properties();
			properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
			properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
			properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
			properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			// create the consumer
			consumer = new KafkaConsumer<String, String>(properties);

			// subscribe consumer to the desired consumer
			consumer.subscribe(Arrays.asList(topic));
		}
		
		public void run() {
			try {
				while (true) {
					ConsumerRecords<String, String> records = 
							consumer.poll(Duration.ofMillis(100));
					for (ConsumerRecord<String, String> record : records) {
						logger.info("Key: " + record.key());
						logger.info("Value: " + record.value());
						logger.info("Partition: " + record.partition());
						logger.info("Offset: " + record.offset());
					}
				}
			} catch(WakeupException e) {
				logger.error("Received shutdown signal" , e);
			} finally {
				consumer.close();
				// tell our main program we are 
				// done with the consumer
				latch.countDown();
			}		
		}
		
		public void shutdown() {
			// the wakeup() method is a special method 
			// to interrupt consumer.poll()
			// it will throw WakeupException.
			consumer.wakeup();
		}
	}
}
