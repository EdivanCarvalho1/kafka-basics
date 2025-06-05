# Projeto de Simulação Kafka com CooperativeStickyAssignor em Java

## 1. Introdução

Este projeto em Java demonstra a implementação de Producers e Consumidores em Kafka, com foco na simulação de
comportamentos como o CooperativeStickyAssignor.

## 2. Configuração do Ambiente

> Website: https://learn.conduktor.io/kafka/how-to-install-apache-kafka-on-windows/

# Pré-requisitos

- Java 11
- WSL2
- Ubuntu 20.04 ou superior
- Docker
- Desabilitar IPV6

## Desabilitar IPV6

É necessário pois o Kafka não funciona bem com o IPV6 habilitado. Para desabilitar, siga os passos:

```sh
sudo sysctl -w net.ipv6.conf.all.disable_ipv6=1
sudo sysctl -w net.ipv6.conf.default.disable_ipv6=1
```

## Instalando o Java 11

O Java é necessário estritamente na versão 11, pois o Kafka é mais estável nessa versão.

```sh
wget -O- https://apt.corretto.aws/corretto.key | sudo apt-key add - 
sudo add-apt-repository 'deb https://apt.corretto.aws stable main'
sudo apt-get update; sudo apt-get install -y java-11-amazon-corretto-jdk
```

## Instalando o Ubuntu pela Microsoft Store

1. Abra a Microsoft Store.
2. Pesquise por "Ubuntu".
3. Instale a versão 20.04 ou superior.
4. Após a instalação, abra o Ubuntu e configure o usuário e senha.

## Instalando o Kafka

Versão utilizada no Projeto: 3.9.1, mas é importante que seja uma versão maior que 3.x.x.

```sh
wget https://dlcdn.apache.org/kafka/3.9.1/kafka_2.12-3.9.1.tgz && \
tar xzf kafka_2.12-3.9.1.tgz && \
rm kafka_2.12-3.9.1.tgz && \
mv kafka_2.12-3.9.1 ~
```

## Adicionando o Kafka ao PATH

```sh
echo 'export PATH=$PATH:~/kafka_2.12-3.9.1/bin' >> ~/.bashrc
source ~/.bashrc
```

> Obs: reiniciar o ubuntu para aplicar as mudanças no PATH.

## Iniciando o Zookeeper e o Kafka

Inicialize o Zookeeper antes do servidor Kafka, pois o Kafka depende do Zookeeper para gerenciar o cluster.

Em versões mais recentes do Kafka, o Zookeeper é opcional, o Kafka Raft é mais utilizado.

```sh
~/kafka_2.12-3.9.1/bin/zookeeper-server-start.sh ~/kafka_2.12-3.9.1/config/zookeeper.properties
```

```sh
~/kafka_2.12-3.9.1/bin/kafka-server-start.sh ~/kafka_2.12-3.9.1/config/server.properties
```

## Modificando server.properties

Para permitir conexões externas, edite o arquivo `server.properties`:

```sh
vim ~/kafka_2.12-3.9.1/config/server.properties
```

editar listeners=PLAINTEXT://localhost:9092

## 3. Componentes

### 3.1. Producers (`ProducerDemo.java`)

* **Responsabilidades:**
    * Enviar 30 mensagens (chave/valor `String`) para o tópico `mytopic`.
* **Configurações chave:**
    * `bootstrap.servers="[::1]:9092"`
    * Serializadores: `StringSerializer` para chave e valor.
    * Uso de `Callback` para logar o resultado do envio (partição e chave).
    * Chamada de `flush()` e `close()` para garantir o envio e liberar recursos.

### 3.2. Consumers (`ConsumerDemoCooperative.java`)

* **Responsabilidades:**
    * Ler mensagens `String` do tópico `mytopic`.
    * Pertence ao grupo de consumidores `my-java-app`.
    * Logar dados da mensagem recebida (chave, valor, partição, offset).
* **Configuração do `partition.assignment.strategy`:**
    * `CooperativeStickyAssignor.class.getName()`: Para rebalanceamento cooperativo e incremental, minimizando
      interrupções no processamento do grupo.
* **Outras Configurações Chave:**
    * `bootstrap.servers="[::1]:9092"`
    * Deserializadores: `StringDeserializer` para chave e valor.
    * `group.id="my-java-app"`
    * `auto.offset.reset="earliest"`
    * Implementação de `ShutdownHook` para desligamento seguro do consumidor.

## 4. Simulação do `CooperativeStickyAssignor`

*(Pré-requisito: Tópico `mytopic` criado com múltiplas partições, ex: 3)*

### 4.1. Cenário 1: Adição de Novo Consumidor

* **Setup inicial:**
    1. Uma instância do `ConsumerDemoCooperative.java` em execução, consumindo de `mytopic`.
* **Passos:**
    1. Iniciar uma segunda instância do `ConsumerDemoCooperative.java` (mesmo `group.id`).
* **Observações do comportamento de reatribuição:**
    * Um rebalanceamento do grupo é iniciado.
    * As partições são redistribuídas incrementalmente entre os dois consumidores.
    * O primeiro consumidor continua processando mensagens das partições que não foram revogadas *durante* o processo de
      atribuição ao segundo. Logs indicarão as mudanças.

### 4.2. Cenário 2: Remoção de Consumidor

* **Setup inicial:**
    1. Duas instâncias do `ConsumerDemoCooperative.java` em execução, consumindo de `mytopic`.
* **Passos:**
    1. Parar uma das instâncias do consumidor (ex: Ctrl+C, acionando o `ShutdownHook`).
* **Observações do comportamento de reatribuição:**
    * Um rebalanceamento do grupo é iniciado.
    * As partições que pertenciam ao consumidor parado são reatribuídas cooperativamente ao consumidor restante.
    * O consumidor restante assume as novas partições com mínima interrupção.

## 5. Resultados e Análise

* **Benefícios do `CooperativeStickyAssignor`:**
    * Rebalanceamentos mais suaves e rápidos, minimizando o "stop-the-world" para o grupo de consumidores.
    * Consumidores podem continuar processando em partições não afetadas durante a redistribuição.
    * Redução significativa do tempo de inatividade, crucial para aplicações que exigem alta disponibilidade.
* **Comparação:**
    * Superior a assignors não cooperativos (ex: `RangeAssignor`, `RoundRobinAssignor` em suas implementações mais
      antigas) que geralmente causam uma parada total no processamento do grupo.
    * O `CooperativeStickyAssignor` combina "aderência" (manter atribuições existentes o máximo possível) com o
      protocolo de rebalanceamento cooperativo, resultando em mínima disrupção.

## 6. Aprendizados e Próximos Passos

* **Aprendizados:**
    * Funcionamento prático dos componentes fundamentais do Kafka: Producers, Consumers, Tópicos, Partições, Offsets e
      Grupos de Consumidores.
    * Conceito de rebalanceamento de consumidores e o impacto das diferentes estratégias de atribuição.
    * Benefícios claros do `CooperativeStickyAssignor` na redução do tempo de inatividade e na melhoria da estabilidade
      do consumo durante mudanças na topologia do grupo.
    * Importância da implementação de um desligamento seguro (`shutdown hook`) para consumidores Kafka.