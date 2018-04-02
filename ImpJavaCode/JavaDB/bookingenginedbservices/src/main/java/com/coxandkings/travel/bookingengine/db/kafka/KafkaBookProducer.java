package com.coxandkings.travel.bookingengine.db.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Qualifier("Kafka")
public class KafkaBookProducer {

	private  static String TOPIC;
	private  static String BOOTSTRAP_SERVERS;
	//TODO: check if we need to move clientID to mongo doc
	private final  static String CLIENTID="KafkaBookProducer";
	
	private Producer<Long, String> createProducer() {
		
		TOPIC=KafkaConfig.getOPS_PRODUCER_TOPIC();
		BOOTSTRAP_SERVERS=KafkaConfig.getOPS_PRODUCER_BOOTSTRAP_SERVERS();
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
		props.put(ProducerConfig.CLIENT_ID_CONFIG, CLIENTID);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		return new KafkaProducer<>(props);
	}

	public void runProducer(final int sendMessageCount, JSONObject message) throws Exception {
		long time = System.currentTimeMillis();

		try (Producer<Long, String> producer = createProducer()) {
			for (long index = time; index < time + sendMessageCount; index++) {
				final ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC, index, message.toString());

				RecordMetadata metadata = producer.send(record).get();
				long elapsedTime = System.currentTimeMillis() - time;
				System.out.printf("sent record(key=%s value=%s) " + "meta(partition=%d, offset=%d) time=%d\n",
						record.key(), record.value(), metadata.partition(), metadata.offset(), elapsedTime);
			}
		}
	}

}
