package io.github.mateusalxd.testekafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class ServicoNota {

    public static void main(String[] args) {
        try (var consumidor = new KafkaConsumer<String, String>(propriedades())) {
            consumidor.subscribe(Collections.singletonList("ESTOQUE_RESERVADO"));

            while (true) {
                ConsumerRecords<String, String> registros = consumidor.poll(Duration.ofSeconds(3));
                if (!registros.isEmpty()) {
                    for (var registro : registros) {
                        System.out.println(gerarNota(registro));
                    }
                }
            }
        }
    }

    private static String gerarNota(ConsumerRecord<String, String> registro) {
        var dadosNota = new StringBuilder();
        var totalGeral = 0D;

        dadosNota.append("-".repeat(52)).append("\n");
        dadosNota.append(" Nota: ")
                .append(UUID.randomUUID().toString())
                .append("\n");
        dadosNota.append(" Caixa: ")
                .append(registro.partition() + 1)
                .append("\n");
        dadosNota.append(" Posição: ")
                .append(registro.offset() + 1)
                .append("\n");
        dadosNota.append("-".repeat(52)).append("\n");
        dadosNota.append("                    Produtos\n");
        dadosNota.append("-".repeat(52)).append("\n");
        dadosNota.append(String.format("%-5s%-20s%9s%9s%9s\n", "Cod.", "Produto", "Valor", "Qtde", "Total"));
        dadosNota.append("-".repeat(52)).append("\n");

        for (String linha : registro.value().split("\\|")) {
            var colunas = linha.split(";");
            var totalLinha = Double.parseDouble(colunas[2]) * Double.parseDouble(colunas[3]);
            totalGeral += totalLinha;
            dadosNota.append(
                    String.format(
                            "%-5s%-20s%9.2f%9d%9s\n",
                            colunas[0],
                            colunas[1],
                            Double.parseDouble(colunas[2]),
                            Integer.parseInt(colunas[3]),
                            String.format("%.2f", totalLinha)));
        }
        dadosNota.append("-".repeat(52)).append("\n");
        dadosNota.append(" Total: ")
                .append(String.format("%44.2f", totalGeral))
                .append("\n");
        dadosNota.append("-".repeat(52)).append("\n");
        return dadosNota.toString();
    }

    private static Properties propriedades() {
        var propriedades = new Properties();
        propriedades.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propriedades.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propriedades.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propriedades.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "NOTA");
        return propriedades;
    }

}
