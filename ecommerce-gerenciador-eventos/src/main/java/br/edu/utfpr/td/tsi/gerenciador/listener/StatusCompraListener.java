package br.edu.utfpr.td.tsi.gerenciador.listener;

import br.edu.utfpr.td.tsi.gerenciador.model.PedidoRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class StatusCompraListener {

	private static final Logger logger = Logger.getLogger(StatusCompraListener.class.getSimpleName());
	private final MongoTemplate mongoTemplate;

	public StatusCompraListener(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@RabbitListener(queues = "fila.pagamento.dlq")
	public void processarFalhaPagamento(PedidoRequest pedido) {
		logger.warning("GERENCIADOR: Falha de pagamento recebida da DLQ para o pedido de " + pedido.getEmailUsuario());

		atualizarStatusNoBanco(pedido.getId(), "FALHA_PAGAMENTO");
	}

	@RabbitListener(queues = "fila.compra.concluida")
	public void processarSucessoCompra(PedidoRequest pedido) {
		logger.info("GERENCIADOR: Sucesso recebido! Compra concluída para " + pedido.getEmailUsuario());

		atualizarStatusNoBanco(pedido.getId(), "CONCLUIDO");
	}

	private void atualizarStatusNoBanco(String pedidoId, String novoStatus) {
		if (pedidoId != null) {
			Query query = new Query(Criteria.where("_id").is(pedidoId));
			Update update = new Update().set("status", novoStatus);
			mongoTemplate.updateFirst(query, update, PedidoRequest.class, "pedidos");
			logger.info("Status do pedido " + pedidoId + " atualizado para: " + novoStatus);
		} else {
			logger.severe("Não foi possível atualizar o banco: ID do pedido está nulo.");
		}
	}
}