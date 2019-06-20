package com.github.kafka.kafka_beginners_course;

import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProducerDemoWithCallback {

	public static void main(String[] args) {
		
		final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);
		String bootStrapServers  = "127.0.0.1:9092";
		
		// create producer properties
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		// create the producer
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
		
		for(int i = 0; i < 10; i++) {
			// create producer record
			ProducerRecord<String, String> record =
					new ProducerRecord<String, String>("first_topic", "hello world " + i);
			
			// send data asynchronously
			producer.send(record, new Callback() {
				
				public void onCompletion(RecordMetadata recordMetadata, Exception e) {
					// executes every time a record is successfully sent or an exception is thrown
					if(null == e) {
						logger.info("Received metadata. \n" + 
					           "Topic: " + recordMetadata.topic() + "\n" + 
					           "Partition: " + recordMetadata.partition() + "\n" + 
					           "Offset: " + recordMetadata.offset() + "\n" + 
					           "Timestamp: " + recordMetadata.timestamp() + "\n");
					} else {
						logger.error("Error while producing ", e);
					}
					
				}
			});
		}
		
		
		
		// flush data and close prouducer
		producer.flush();
		producer.close();
	}

}
