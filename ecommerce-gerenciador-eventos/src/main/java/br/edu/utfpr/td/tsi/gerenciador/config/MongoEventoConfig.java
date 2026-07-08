package br.edu.utfpr.td.tsi.gerenciador.config;

import br.edu.utfpr.td.tsi.gerenciador.model.PedidoRequest;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import org.bson.Document;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListener;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.data.mongodb.core.query.Criteria;

@Configuration
public class MongoEventoConfig {

    @Bean
    public MessageListenerContainer messageListenerContainer(MongoTemplate mongoTemplate, RabbitTemplate rabbitTemplate) {
        MessageListenerContainer container = new DefaultMessageListenerContainer(mongoTemplate);
        container.start();

        MessageListener<ChangeStreamDocument<Document>, PedidoRequest> listener = event -> {
            PedidoRequest novoPedido = event.getBody();
            
            if (novoPedido != null) {
                System.out.println("GERENCIADOR DE EVENTOS: Novo pedido detectado no banco de dados para " + novoPedido.getEmailUsuario());
                
                rabbitTemplate.convertAndSend("fila.email", "Confirmação de pedido recebida para: " + novoPedido.getEmailUsuario());
                rabbitTemplate.convertAndSend("fila.pagamento", novoPedido);
            }
        };

        ChangeStreamRequest<PedidoRequest> request = ChangeStreamRequest.builder(listener)
                .collection("pedidos")
                .filter(Aggregation.newAggregation(Aggregation.match(Criteria.where("operationType").is("insert"))))
                .build();

        container.register(request, PedidoRequest.class);

        return container;
    }
}