package br.iff.edu.edivan;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


public class ProducerDemo {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());

    public static void main(String[] args) {
        // Logger
        log.info("hello world");

        // Producer Properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // Kafka Producer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        // Producer Record


        for (int i = 0; i < 30; i++) {

            String topic = "mytopic";
            String key = "_id" + i;
            String value = "Hello World_" + i;

            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);

            kafkaProducer.send(producerRecord, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    if (e == null) {
                        log.info("{ Key: " + key + " | Partition: " + metadata.partition() + " }"
                        );
                    } else {
                        log.error("Erro ao receber metadados: " + e);
                    }

                }
            });
        }

        kafkaProducer.flush();

        kafkaProducer.close();
    }
}
