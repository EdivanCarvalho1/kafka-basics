package br.iff.edu.edivan;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class ConsumerDemoCooperative {

    public static final Logger log = LoggerFactory.getLogger(ConsumerDemoCooperative.class.getName());

    public static void main(String[] args) {
        log.info("Kafka Consumer");

        String groupId = "my-java-app";

        String topic = "mytopic";

        // Producer Properties
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "[::1]:9092");

        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        properties.setProperty("group.id", groupId);

        properties.setProperty("auto.offset.reset", "earliest");
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);

        final Thread currentThread = Thread.currentThread();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Detectado um shutdown, saindo!");
                kafkaConsumer.wakeup();

                try {
                    currentThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        try {

            // Faz subscribe a um ou mais tópicos
            kafkaConsumer.subscribe(List.of(topic));

            while (true) {
                log.info("polling");
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    log.info("{ Key: " + record.key() + " | Value: " + record.value() + " }");
                    log.info("{ Partition: " + record.partition() + " | Offset: " + record.offset() + " }");
                }

            }
        } catch (WakeupException e) {
            log.info("O consumer está desligando...");
        } catch (Exception e) {
            log.error("Erro inesperado!");
        } finally {
            kafkaConsumer.close();
            log.info("O consumor foi desligado com sucesso.");
        }

    }
}
