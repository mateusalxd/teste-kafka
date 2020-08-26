Este projeto tem como objetivo realizar um teste simples com Kafka.

## Como usar

```bash
# --> para montar sua própria imagem

# considerando que o diretório atual seja a raiz do projeto
cd kafka
docker build -t mateusalxd/kafka:2.13-2.6.0-1.8 .

# --> se preferir utilizar a imagem já pronta

docker pull mateusalxd/kafka:2.13-2.6.0-1.8

# --* 2.13 Scala, 2.6.0 Kafka e 1.8 Java
```

Com a imagem disponível, agora é nececessário executá-la e em seguida acessá-la para criar os tópicos.

```bash
# monta o volume
docker volume create dados-kafka

# --rm pode ser desconsiderado caso queira
docker run --rm -d --name kafka-server -p 9092:9092 -v dados-kafka:/app/data mateusalxd/kafka:2.13-2.6.0-1.8

# considerando que o nome do container foi mantido
docker exec -it $(docker ps -q -f name=kafka-server) /bin/bash

# criação dos tópicos
./kafka/bin/kafka-topics.sh --create --topic NOVA_ORDEM --bootstrap-server localhost:9092

./kafka/bin/kafka-topics.sh --create --topic ESTOQUE_RESERVADO --partitions 3 --bootstrap-server localhost:9092

./kafka/bin/kafka-topics.sh --create --topic ESTOQUE_INSUFICIENTE --bootstrap-server localhost:9092

# para sair do container
exit
```

Neste exemplo foi utilizado um banco de dados MySQL, então agora é necessário subir um container e depois popular alguns dados.

```bash
# já configurei uma variável de ambiente chamada MYSQL_ROOT_PASSWORD

docker run --name mysql-server -e MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD} -e MYSQL_DATABASE=projeto -p 3306:3306 -d mysql

# acessando o MySQL para popular os dados
# considerando que o nome do container foi mantido
docker exec -it $(docker ps -q -f name=mysql-server) /bin/bash

# nessa parte será solicitada a senha
mysql -u root -p

use projeto;

# copie o conteúdo do arquivo mysql/estrutura.sql e cole no terminal

# para sair do container
exit
exit
```

Alguns outros comandos utilitários.

```bash
# acessar os logs do Kafka
docker logs -f $(docker ps -q -f name=kafka-server)

# parar o Kafka
docker stop $(docker ps -q -f name=kafka-server)

# parar o MySQL
docker stop $(docker ps -q -f name=mysql-server)
```
