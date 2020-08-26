package io.github.mateusalxd.testekafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.sql.*;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ServicoReserva {

    private static Connection conexao;
    private static KafkaProducer<String, String> produtor;
    private static final Callback retorno = (resultado, erro) -> {
        if (erro != null)
            erro.printStackTrace();

        System.out.printf("%s::%s::%s::%s%n",
                resultado.topic(),
                resultado.partition(),
                resultado.offset(),
                resultado.timestamp());
    };

    public static void main(String[] args) throws SQLException, ExecutionException, InterruptedException {
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/projeto",
                "root", System.getenv("MYSQL_ROOT_PASSWORD"));

        produtor = new KafkaProducer<>(propriedadesProdutor());

        try (var consumidor = new KafkaConsumer<String, String>(propriedadesConsumidor())) {
            consumidor.subscribe(Collections.singletonList("NOVA_ORDEM"));
            while (true) {
                ConsumerRecords<String, String> registros = consumidor.poll(Duration.ofSeconds(3));
                if (!registros.isEmpty()) {
                    for (var registro : registros) {
                        realizarReserva(registro.key(), registro.value());
                    }
                }
            }
        }
    }

    private static void realizarReserva(String ordem, String dados) throws SQLException, ExecutionException, InterruptedException {
        var linhasOrdemPura = dados.split("\\|");
        var produtosOrdem = new HashMap<String, String>();
        for (var linha : linhasOrdemPura) {
            var colunas = linha.split(";");
            produtosOrdem.put(colunas[0], colunas[0]);
        }

        var ids = produtosOrdem.keySet().stream().reduce("", (anterior, atual) -> {
            if (anterior.isEmpty())
                return atual;

            return String.format("%s, %s", anterior, atual);
        });

        var consulta = "SELECT id_produto, SUM(qt_transacao) qtde FROM transacao WHERE id_produto in (" +
                ids + ") AND in_revertida = 0 GROUP BY id_produto";
        var comandoConsulta = conexao.createStatement();
        ResultSet resultado = comandoConsulta.executeQuery(consulta);
        var contador = 0;
        var valoresInsercao = new StringBuilder();

        while (resultado.next()) {
            var idProduto = resultado.getInt("id_produto");
            var quantidadeDisponivel = resultado.getInt("qtde");
            var quantidadeOrdem = Integer.parseInt(produtosOrdem.get(String.valueOf(idProduto)));
            var diferenca = quantidadeDisponivel - quantidadeOrdem;

            if (diferenca >= 0) {
                valoresInsercao.append(
                        String.format(contador == 0 ?
                                        "(%d, '%s', %d, NOW())" :
                                        ", (%d, '%s', %d, NOW())",
                                idProduto, ordem, -1 * quantidadeOrdem));
                contador++;
            } else {
                break;
            }
        }

        if (contador == linhasOrdemPura.length) {
            Statement comandoInsercao = conexao.createStatement();
            comandoInsercao.execute("INSERT INTO transacao (id_produto, id_ordem, qt_transacao, dt_criacao) " +
                    "VALUES " + valoresInsercao.toString());
            emitirEstoqueReservado(ordem, dados);
        } else {
            emitirEstoqueInsuficiente(ordem, dados);
        }
    }

    private static void emitirEstoqueReservado(String ordem, String dados) throws ExecutionException, InterruptedException {
        produtor.send(new ProducerRecord<>("ESTOQUE_RESERVADO", ordem, dados), retorno).get();
    }

    private static void emitirEstoqueInsuficiente(String ordem, String dados) throws ExecutionException, InterruptedException {
        produtor.send(new ProducerRecord<>("ESTOQUE_INSUFICIENTE", ordem, dados), retorno).get();
    }

    private static Properties propriedadesConsumidor() {
        var propriedades = new Properties();
        propriedades.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propriedades.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propriedades.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propriedades.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "RESERVA");
        return propriedades;
    }

    private static Properties propriedadesProdutor() {
        var propriedades = new Properties();
        propriedades.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propriedades.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        propriedades.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return propriedades;
    }

}
