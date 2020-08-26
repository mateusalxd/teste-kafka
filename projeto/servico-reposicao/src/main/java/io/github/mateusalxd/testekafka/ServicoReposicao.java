package io.github.mateusalxd.testekafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

public class ServicoReposicao {

    private static Connection conexao;
    private static HashMap<Integer, Integer> contador = new HashMap<>();

    public static void main(String[] args) throws SQLException {
        conexao = DriverManager.getConnection("jdbc:mysql://localhost:3306/projeto",
                "root", System.getenv("MYSQL_ROOT_PASSWORD"));

        try (var consumidor = new KafkaConsumer<String, String>(propriedades())) {
            consumidor.subscribe(Collections.singletonList("ESTOQUE_INSUFICIENTE"));

            while (true) {
                ConsumerRecords<String, String> registros = consumidor.poll(Duration.ofSeconds(60));
                if (!registros.isEmpty()) {
                    for (var registro : registros) {
                        contarProdutos(registro);
                    }
                    validarEstoque();
                }
            }
        }
    }

    private static void validarEstoque() throws SQLException {
        var ids = contador.keySet().stream()
                .map(id -> id.toString())
                .reduce("", (anterior, atual) ->
                        anterior.isEmpty() ?
                                atual :
                                anterior + ", " + atual);

        var consulta = "SELECT t.id_produto, p.descricao, SUM(t.qt_transacao) qtde FROM transacao t " +
                "INNER JOIN produto p ON (p.id = t.id_produto) WHERE id_produto in (" +
                ids + ") AND t.in_revertida = 0 GROUP BY t.id_produto, p.descricao";
        var comandoConsulta = conexao.createStatement();
        ResultSet resultado = comandoConsulta.executeQuery(consulta);

        while (resultado.next()) {
            var idProduto = resultado.getInt("id_produto");
            var quantidadeDisponivel = resultado.getInt("qtde");
            var descricao = resultado.getString("descricao");
            var quantidadeRequisitada = contador.get(idProduto);
            var diferenca = quantidadeDisponivel - quantidadeRequisitada;

            if (diferenca < 0) {
                System.out.println(
                        String.format("O produto %d - %s teve uma quantidade requisitada de %d, " +
                                        "porém não teve disponível em estoque para finalizar pedidor.",
                                idProduto, descricao, quantidadeRequisitada));
            }
        }

    }

    private static void contarProdutos(ConsumerRecord<String, String> registro) {
        var linhas = registro.value().split("\\|");
        for (var linha : linhas) {
            var colunas = linha.split(";");
            var idProduto = Integer.valueOf(colunas[0]);
            var quantidadeOrdem = Integer.valueOf(colunas[3]);
            if (contador.containsKey(idProduto)) {
                var quantidadeAcumulada = contador.get(idProduto);
                contador.replace(idProduto, quantidadeAcumulada + quantidadeOrdem);
            } else {
                contador.put(idProduto, quantidadeOrdem);
            }
        }
    }

    private static Properties propriedades() {
        var propriedades = new Properties();
        propriedades.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propriedades.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propriedades.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propriedades.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "REPOSICAO_ESTOQUE");
        return propriedades;
    }

}
