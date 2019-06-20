package com.github.kafka.kafka_twitter_example;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

// refer https://github.com/twitter/hbc

public class TwitterProducerWithCompression {
	
	Logger logger = LoggerFactory.getLogger(TwitterProducerWithCompression.class.getName());

	private static final String consumerKey = "M3ZdcDDi5UIdDn09jKE7VB8gx";
	private static final String consumerSecret = "AiWAaTOVWf7M5zeVWF3WTgZpLzeMo92xon3DBes6I3jwWKnrRb";
	private static final String token = "971445173078188032-D3za2I0UwG8gZGMgU5SIgq6jZjpfmpc";
	private static final String secret = "GlJh4w9Ijk873AFNALffgk8vTD1N3smD2XoJeRZ7CA1Gi";
	
	private List<String> terms = Lists.newArrayList("kafka" , "politics", "india");
	
	public TwitterProducerWithCompression() {
		
	}
	
	public void run() {
		/** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
		BlockingQueue<String> msgQueue = 
				new LinkedBlockingQueue<String>(10);
		// create twitter account
		Client client = createTwitterClient(msgQueue);
		// create a kafka producer
		KafkaProducer<String, String> producer = createKafkaProducer();
		client.connect();		
		
		// add a shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("exiting the application");
			logger.info("shutting down the client from twitter");
			client.stop();
			logger.info("closing the producer");
			producer.close();
			logger.info("samapt");
		}));
		
		// loop to send tweets to kafka
		// on a different thread, or multiple different threads....
		while (!client.isDone()) {
		  String msg;
			try {
				msg = msgQueue.poll(5, TimeUnit.SECONDS);
				if(null != msg) {
					logger.info("msg: " + msg);
					producer.send(new 
							ProducerRecord<String, String>("twitter_tweets", null, msg), new Callback() {
								
								@Override
								public void onCompletion(RecordMetadata record, Exception e) {
									if(null != e) {
										logger.error("Something bad happended", e);
									}
								}
							});
				}
					
			} catch (InterruptedException e) {
				e.printStackTrace();
				client.stop();
			}			
		}
		logger.info("exiting");
	}
	
	public static void main(String[] args) {
		new TwitterProducerWithCompression().run();
	}
	
	public KafkaProducer<String, String> createKafkaProducer(){
String bootStrapServers  = "127.0.0.1:9092";
		
		// create producer properties
		Properties properties = new Properties();
		properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		
		// create safe producer
		properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
		properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
		properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
		//kafka 2.x > 1.1 version, so 5 else it should be 1
		properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
		
		// high throughput producer (at the expense of a bit of latency and CPU usage)
		properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
		properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
		properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));
		
		// create the producer
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
		return producer;		
	}
	
	public Client createTwitterClient(BlockingQueue<String> msgQueue) {
		
		/** Declare the host you want to connect to, 
		 * the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		//List<Long> followings = Lists.newArrayList(1234L, 566788L);
		
		//hosebirdEndpoint.followings(followings);
		hosebirdEndpoint.trackTerms(terms);

		// These secrets should be read from a config file
		Authentication hosebirdAuth = new 
				OAuth1(consumerKey, consumerSecret, token, secret);
		
		// Creating Twitter client
		ClientBuilder builder = new ClientBuilder()
				  .name("Hosebird-Client-01")                              // optional: mainly for the logs
				  .hosts(hosebirdHosts)
				  .authentication(hosebirdAuth)
				  .endpoint(hosebirdEndpoint)
				  .processor(new StringDelimitedProcessor(msgQueue));
				  //.eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

				Client hosebirdClient = builder.build();
				// Attempts to establish a connection.
				return hosebirdClient;
		
	}

}
