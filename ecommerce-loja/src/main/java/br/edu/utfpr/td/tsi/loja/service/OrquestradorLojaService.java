package br.edu.utfpr.td.tsi.loja.service;

import br.edu.utfpr.td.tsi.loja.model.PagamentoRequest;
import br.edu.utfpr.td.tsi.loja.model.PedidoRequest;
import br.edu.utfpr.td.tsi.loja.repository.PedidoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.logging.Logger;

@Service
public class OrquestradorLojaService {

	private static final Logger logger = Logger.getLogger(OrquestradorLojaService.class.getSimpleName());

	private RestTemplate restTemplate = new RestTemplate();

	@Autowired
	private PedidoRepository repository;

	private static final String URL_LOGISTICA = "http://localhost:8082";

	public String processarCompra(PedidoRequest pedido) {
		logger.info("--- INICIANDO PROCESSAMENTO DE COMPRA LEGADA ---");

		BigDecimal valorTotal = pedido.getValorBase() != null ? pedido.getValorBase() : BigDecimal.ZERO;

		if (pedido.getPagamento() == null) {
			pedido.setPagamento(new PagamentoRequest());
		}
		pedido.getPagamento().valorTotal(valorTotal);

		try {
			String enderecoJson = restTemplate.getForObject(URL_LOGISTICA + "/api/cep/" + pedido.getCep(),
					String.class);
			logger.info("Endereço consultado via REST: " + enderecoJson);
		} catch (Exception e) {
			logger.warning("Aviso ao consultar o CEP: " + e.getMessage());
		}

		pedido.setStatus("CRIADO");
		repository.save(pedido);

		logger.info("--- Pedido salvo no banco de dados pelo sistema legado ---");

		return "Pedido registrado no banco! O Gerenciador de Eventos assumirá a partir daqui.";
	}
}