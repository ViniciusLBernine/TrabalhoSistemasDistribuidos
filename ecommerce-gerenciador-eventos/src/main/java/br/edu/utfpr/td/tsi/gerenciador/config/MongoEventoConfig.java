package br.edu.utfpr.td.tsi.gerenciador.config;

import br.edu.utfpr.td.tsi.gerenciador.model.PedidoRequest;
import br.edu.utfpr.td.tsi.gerenciador.model.PagamentoRequest;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

@Configuration
public class MongoEventoConfig {

	private static final Logger logger = Logger.getLogger(MongoEventoConfig.class.getSimpleName());

	@Bean
	public MessageListenerContainer messageListenerContainer(MongoTemplate mongoTemplate,
			RabbitTemplate rabbitTemplate) {
		MessageListenerContainer container = new DefaultMessageListenerContainer(mongoTemplate);

		MessageListener<ChangeStreamDocument<Document>, Document> listener = event -> {
			Document doc = event.getBody();

			if (doc != null) {
				logger.info("GERENCIADOR DE EVENTOS: Novo pedido detectado no banco para: "
						+ doc.getString("emailUsuario"));

				PedidoRequest novoPedido = new PedidoRequest();
				if (doc.get("_id") != null)
					novoPedido.setId(doc.get("_id").toString());
				novoPedido.setEmailUsuario(doc.getString("emailUsuario"));
				novoPedido.setCep(doc.getString("cep"));
				List<String> produtos = doc.getList("produtosIds", String.class);
				if (produtos != null) {
				    novoPedido.setProdutosIds(produtos);
				}

				if (doc.get("valorBase") != null) {
					novoPedido.setValorBase(new BigDecimal(doc.get("valorBase").toString()));
				}

				Document pagDoc = (Document) doc.get("pagamento");
				if (pagDoc != null) {
					PagamentoRequest pag = new PagamentoRequest();
					if (pagDoc.get("valorTotal") != null) {
						pag.setValorTotal(new BigDecimal(pagDoc.get("valorTotal").toString()));
					}
					novoPedido.setPagamento(pag);
				}

				rabbitTemplate.convertAndSend("fila.email",
						"Confirmação de pedido recebida para: " + novoPedido.getEmailUsuario());
				rabbitTemplate.convertAndSend("fila.pagamento", novoPedido);
			}
		};

		ChangeStreamRequest<Document> request = ChangeStreamRequest.builder(listener).collection("pedidos")
				.filter(Aggregation.newAggregation(Aggregation.match(Criteria.where("operationType").is("insert"))))
				.build();

		container.register(request, Document.class);
		container.start();

		return container;
	}
}