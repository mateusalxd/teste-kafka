package io.github.mateusalxd.testekafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Properties;

public class GeradorOrdem {

    public static void main(String[] args) {
        var produtor = new KafkaProducer<String, String>(propriedades());
        try {
            var produtos = new String[][]{
                    {"Arroz", "18.5"},
                    {"Feijão", "6.49"},
                    {"Óleo", "4.90"},
                    {"Molho de Tomate", "2.65"},
                    {"Açúcar", "2.59"},
                    {"Sorvete", "28.90"},
                    {"Sal refinado", "2.65"},
            };

            for (int i = 1; i <= 10; i++) {
                var resumoOrdem = new HashMap<Integer, Integer>();
                var numeroProdutos = (int) (Math.random() * 20 + 1);

                for (int x = 0; x < numeroProdutos; x++) {
                    var posicaoProduto = (int) (Math.random() * 7);
                    if (resumoOrdem.containsKey(posicaoProduto)) {
                        var quantidade = resumoOrdem.get(posicaoProduto).intValue();
                        resumoOrdem.replace(posicaoProduto, quantidade + 1);
                    } else {
                        resumoOrdem.put(posicaoProduto, 1);
                    }
                }

                var ordemCompleta = resumoOrdem.entrySet().stream()
                        .map(item -> {
                            var produto = produtos[item.getKey()];
                            return String.format("%s;%s;%s", produto[0], produto[1], item.getValue());
                        }).reduce("", (anterior, atual) -> {
                            if (anterior.isEmpty())
                                return atual;
                            return String.format("%s|%s", anterior, atual);
                        });

                var registro = new ProducerRecord<String, String>("NOVA_ORDEM", String.valueOf(i), ordemCompleta);
                Callback retorno = (resultado, erro) -> {
                    if (erro != null)
                        erro.printStackTrace();

                    System.out.println(
                            String.format("%s::%s::%s::%s",
                                    resultado.topic(),
                                    resultado.partition(),
                                    resultado.offset(),
                                    resultado.timestamp()));
                };
                produtor.send(registro, retorno).get();
                Thread.sleep((int) (Math.random() * 5 + 1) * 1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            produtor.close();
        }
    }

    private static Properties propriedades() {
        var propriedades = new Properties();
        propriedades.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propriedades.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        propriedades.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return propriedades;
    }

}
