package br.edu.utfpr.td.tsi.loja.repository;

import br.edu.utfpr.td.tsi.loja.model.PedidoRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends MongoRepository<PedidoRequest, String> {
}