package com.github.kafka.kafka_beginners_course;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerDemoAssignAndSeek {

	public static void main(String[] args) {

		String bootStrapServers = "127.0.0.1:9092";
		String topic = "first_topic";
		String group_id = "my_sixth_application";
		Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);
		
		// create consumer properties
		Properties properties = new Properties();
		properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		//properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group_id);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		// create the consumer
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);

		// assign and seek are mostly used to replay data or fetch a specific message
		
		// assign
		TopicPartition partitionToReadFrom = new TopicPartition(topic, 0);
		long offsetToReadFrom = 5L;
		consumer.assign(Arrays.asList(partitionToReadFrom));
		
		// seek
		consumer.seek(partitionToReadFrom, offsetToReadFrom);
		
		int noOfMsgsToRead = 5;
		boolean keepOnReading = true;
		int noOfMsgsReadSoFar = 0;
		

		while (keepOnReading) {
			ConsumerRecords<String, String> records = 
					consumer.poll(Duration.ofMillis(100));

			for (ConsumerRecord<String, String> record : records) {
				noOfMsgsReadSoFar++;
				logger.info("Key: " + record.key());
				logger.info("Value: " + record.value());
				logger.info("Partition: " + record.partition());
				logger.info("Offset: " + record.offset());
				if(noOfMsgsReadSoFar > noOfMsgsToRead) {
					keepOnReading = false; // to exit while loop
					break;
				}
			}
		}
		
		logger.info("Exiting....");
	}

}
