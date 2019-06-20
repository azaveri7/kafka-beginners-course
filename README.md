Cli commands
 
1.	Start zookeeper
zookeeper-server-start.bat config\zookeeper.properties
 
2.	Start kafka broker
kafka-server-start.bat config\server.properties
 
3.	Create a topic
kafka-topics  --zookeeper 127.0.0.1:2181 --topic first_topic --create --partitions 3  --replication-factor 1
kafka-topics  --zookeeper 127.0.0.1:2181 --topic second_topic --create --partitions 6  --replication-factor 1
 
4.	To print names of all topics
kafka-topics --zookeeper 127.0.0.1:2181 --list
 
5.	To get info about a topic
kafka-topics --zookeeper 127.0.0.1:2181 --topic first_topic --describe
 
6.	To delete a topic
kafka-topics --zookeeper 127.0.0.1:2181 --topic second_topic --delete
 
7.	To produce messages to a topic using inbuilt producer
kafka-console-producer --broker-list 127.0.0.1:9092 --topic first_topic
 
Here if first_topic do not exist, then it will be created with single partition and replication factor=1
This is because in server.properties, there is a property as
 
num.partitions=1
 
If you change it to 
 
num.partitions=3
 
New topic created by kafka producer will have 3 partitions by default.
 
8.	To use acknowledgement
 
kafka-console-producer --broker-list 127.0.0.1:9092 --topic fist_topic --producer-property acks=all
 
9.	Kafka consumer
kafka-console-consumer --bootstrap-server 127.0.0.1:9092 --topic first_topic --from-beginning
 
10.	Consumer  in a group
kafka-console-consumer --bootstrap-server 127.0.0.1:9092 --topic first_topic --group my-first-application
 
Here if multiple consumers are running in the same group and listening to same topic, if producer posts a message
to the topic, any one consumer will pick one msg and others will pick other msgs.
 
So consumers in a group share the load.
 
Instead of 127.0.0.1:9092, you can use localhost:9092
 
11.	To list consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list
 
12.	To describe a consumer group
kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group my-first-application
 
It shows following table if consumers are active in this group
 
TOPIC             PARTITION     CURRENT-OFFSET        LOG-END-OFFSET      LAG      CONSUMER-ID   HOST    CLIENT-ID
first_topic           0                             9                                     11                          2                  -                    -               -
first_topic           2                             11                                   13                          2                  -                    -               -
first_topic           1                             9                                     10                          1                  -                    -               -
 
Meaning in partition 0, pointer is at 9th position and 2 msgs are yet to be read. 
 
Consumer-id and host will give application name and the host on which consumer is running.
 
13.	How to replay data by resetting offset
kafka-consumer-groups --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --to-earliest --execute --topic first_topic
 
All partitions have their offset reset to 0.
 
14.	To replay data by few offsets
kafka-consumer-groups --bootstrap-server localhost:9092 --group my-first-application --reset-offsets --shift-by -2 --execute --topic first_topic
 
So u can read the last 2 msgs.
 
KAFKA UI tool
www.kafkatool.com
 
15.	For the maven java project, google maven apache kafka dependencies.
 
And select the dependencies listed below kafka-clients and version 2.x
 
16.	To write java code, google apache kafka documentation and search for the topic that you are writing.
For e.g. for writing java kafka producer, go to producer configs. It will give you the list of properties that you need to configure to write a kafka producer.
 
17.	If you create a java program for a consumer with a group_id and give the argument as 'earliest', It will reall all msgs from beginning first time. If you the program again with same group_id, it will not read msgs from beginning.
 
If u use the command 'kafka-consumer-groups --bootstrap-server localhost:9092 --group my-fourth-application --describe' to describe the group_id, you will see the offset value set to last msg.
 
So to run the same consumer program again and read the msgs from beginning in a topic, either use a different group_id each time u run the program or reset the offset of the consumer group using cli command.
 
18.	If you have a consumer Cons1 subscribed to topic first_topic, it will be assigned all 3 partitions of topic to consumer. If you then start Cons2 which is subscribed to same topic and group_id, then partition rebalancing happens and for e.g. Cons1 will be assigned partition1 and Cons2 will be assigned partition0 and partition2.
 
So if a producer now posts msgs to first_topic, then all msgs in partition1 will be consumed by Cons1 and msgs in partition0 and partition2 will be consumed by Cons2.
 
If a new consumer Cons3 comes up, again partition reassignment happens. Similarly if a consumer goes down, agai n partition reassignment happens.
 
19.	Assign and seek consumers are used to read a specific message / messages from a specific partition.
 
20.	As of Kafka 0.10.2, clients and kafka brokers have bi-directional compatibility meaning older client version can talk to new kafka broker version and vice-versa.

Setting up elastic search as a service

https://bonsai.io

GET /_cat/health?v -> gives info about health of our cluster and number of nodes.
PUT /twitter -> will create twitter index
GET /_cat/indices?v -> list of all indexes stored in elastic search
PUT /twitter/tweets/1 and body is 
{
  	“course” : “Kafka beginners”,
	“instructor”: “AnandZaveri”,
	“module”: “ElasticSearch”
}

GET /twitter/tweets/1

You will get the stored index.

Delete /twitter

Deletes the twitter index	
 
Case study:
 
1.	MovieFlix:
 
a.	Make sure user can resume video where they left off
b.	Build a user profile in real time
c.	Recommend next show to the user
d.	Store all data in analytics store
 
Implementation: 
 
Advanced topics:
1.	Kafka message acknowledgements:
 
acks=0, leader in the kafka cluster will not send any acknowledgement for the message received.
acks=1, leader in the kafka will send an acknowledgement even if the msg received is not replicated. So if the    leader in the kafka cluster goes down before data replication, you will lose the msg.
acks=all(replicas acks), leader will send the acknowledgement only after replicas are sent to other kafka brokers and they receive acknowledgement from these brokers. So latency and safety is high in this.
 
acks=all is used in conjunction with min.insync.replicas. If there are not enough brokers available in the cluster for replication, it will throw NOT_ENOUGH_REPLICAS exception to the producer.
 
2.	Few properties:
 
linger.ms -> number of milliseconds a producer is willing to wait before sending a batch out. By introducing some lag(linger.ms=5), we increase the chances of msgs beings sent together in a batch.
batch.size -> if batch is full before the end of linger.ms period, it will be sent to kafka right away. Default value = 16kb.
buffer.memory-> if the producer produces msgs faster  than consumer can consume it, msgs can be stored in buffer.memory. Buffer memory max is 32 MB.
max.block.ms-> if the buffer is full, then the .send() method will start to block. The time the .send() will block until throwing an exception. Exceptions are basically thrown when
i.	Producer has filled buffer
ii.	Broker is not accepting any new data
iii.	60 seconds has elapsed
 
1.	Kafka topic configuration:
 
kafka-configs --zookeeper 127.0.0.1:2181 --entity-type topics --entity-name configured_topic --describe
 
kafka-configs --zookeeper 127.0.0.1:2181 --entity-type topics --entity-name configured_topic --add-config min.insync.replicas=2  --alter
 
kafka-configs --zookeeper 127.0.0.1:2181 --entity-type topics --entity-name configured_topic --delete
-config min.insync.replicas=2  --alter
 
2.	Partitions and Segments:
 
Topics made of partitions and partitions made of segments. Last segment called Active segment.
 
At any time, there is only 1 active segment where data is being written.
 
log.segment.bytes -> max size of single segment in bytes
log.segment.ms-> time kafka will wait before committing segment if not full
 
Segment come with 2 indexes (files):
a.	Offset to position index.
b.	Timestamp to offset index.
 
Therefore, kafka knows where to find data in constant time.
