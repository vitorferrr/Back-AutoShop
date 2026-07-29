package com.oficina.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oficina.api.dto.OrdemServicoRequestDTO;
import com.oficina.api.dto.OrdemServicoResponseDTO;
import com.oficina.api.exception.RecursoNaoEncontradoException;
import com.oficina.api.exception.RegraDeNegocioException;
import com.oficina.api.model.ClienteModel;
import com.oficina.api.model.OrdemServicoModel;
import com.oficina.api.model.PecaModel;
import com.oficina.api.model.ServicoModel;
import com.oficina.api.model.StatusOrdemServico;
import com.oficina.api.model.TipoCalculoPreco;
import com.oficina.api.model.VeiculoModel;
import com.oficina.api.repository.OrdemServicoRepository;
import com.oficina.api.repository.PecaRepository;
import com.oficina.api.repository.ServicoRepository;
import com.oficina.api.service.strategy.PrecoStrategyFactory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteService clienteService;
    private final VeiculoService veiculoService;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final PrecoStrategyFactory precoStrategyFactory;


    @Transactional
    public OrdemServicoResponseDTO criar(OrdemServicoRequestDTO request) {
        OrdemServicoModel ordem = new OrdemServicoModel();
        preencher(ordem, request);

        // Decrementa o estoque de cada peça associada
        decrementarEstoque(ordem.getPecas());

        return OrdemServicoResponseDTO.from(ordemServicoRepository.save(ordem));
    }

    @Transactional(readOnly = true)
    public Page<OrdemServicoResponseDTO> listar(String busca, StatusOrdemServico status, Pageable pageable) {
        return ordemServicoRepository
                .buscar(busca == null ? null : busca.trim(), status, pageable)
                .map(OrdemServicoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponseDTO buscarPorId(Long id) {
        return OrdemServicoResponseDTO.from(buscarModelPorId(id));
    }

    @Transactional
    public OrdemServicoResponseDTO atualizar(Long id, OrdemServicoRequestDTO request) {
        OrdemServicoModel ordem = buscarModelPorId(id);

        restaurarEstoque(ordem.getPecas());

        preencher(ordem, request);

        decrementarEstoque(ordem.getPecas());

        return OrdemServicoResponseDTO.from(ordemServicoRepository.save(ordem));
    }

    @Transactional
    public void excluir(Long id) {
        OrdemServicoModel ordem = buscarModelPorId(id);

        restaurarEstoque(ordem.getPecas());

        ordemServicoRepository.deleteById(id);
    }

    public OrdemServicoModel buscarModelPorId(Long id) {
        return ordemServicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Ordem de serviço não encontrada com id: " + id));
    }

    
    private void preencher(OrdemServicoModel ordem, OrdemServicoRequestDTO request) {
        ClienteModel cliente = clienteService.buscarModelPorId(request.clienteId());
        VeiculoModel veiculo = veiculoService.buscarModelPorId(request.veiculoId());

        // Garante que o veículo pertence ao cliente informado
        if (!veiculo.getCliente().getId().equals(cliente.getId())) {
            throw new RegraDeNegocioException(
                    "O veículo selecionado não pertence ao cliente informado.");
        }

        List<ServicoModel> servicos = buscarServicos(request.servicoIds());
        List<PecaModel> pecas = buscarPecas(request.pecaIds());
        TipoCalculoPreco tipo = request.tipoCalculoPreco() == null
                ? TipoCalculoPreco.PADRAO
                : request.tipoCalculoPreco();

        ordem.setCliente(cliente);
        ordem.setVeiculo(veiculo);
        ordem.setStatus(request.status() == null ? StatusOrdemServico.ABERTA : request.status());
        ordem.setDescricaoProblema(limpar(request.descricaoProblema()));
        ordem.setObservacoes(limpar(request.observacoes()));
        ordem.setTipoCalculoPreco(tipo);
        ordem.setServicos(servicos);
        ordem.setPecas(pecas);
        ordem.setValorTotal(precoStrategyFactory.calcular(tipo, servicos, pecas));
    }

    private List<ServicoModel> buscarServicos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        List<ServicoModel> encontrados = servicoRepository.findAllById(ids);

        if (encontrados.size() != ids.size()) {
            throw new RecursoNaoEncontradoException(
                    "Um ou mais serviços informados não foram encontrados.");
        }

        // serviço inativo não pode entrar em uma ordem de serviço
        List<String> inativos = encontrados.stream()
                .filter(s -> !Boolean.TRUE.equals(s.getAtivo()))
                .map(ServicoModel::getNome)
                .toList();

        if (!inativos.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Os seguintes serviços estão inativos e não podem ser adicionados: "
                    + String.join(", ", inativos));
        }

        return encontrados;
    }

    private List<PecaModel> buscarPecas(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        List<PecaModel> encontradas = pecaRepository.findAllById(ids);

        if (encontradas.size() != ids.size()) {
            throw new RecursoNaoEncontradoException(
                    "Uma ou mais peças informadas não foram encontradas.");
        }

        // valida se tem estoque suficiente para cada peça
        List<String> semEstoque = encontradas.stream()
                .filter(p -> p.getQuantidade() <= 0)
                .map(PecaModel::getNome)
                .toList();

        if (!semEstoque.isEmpty()) {
            throw new RegraDeNegocioException(
                    "As seguintes peças estão sem estoque disponível: "
                    + String.join(", ", semEstoque));
        }

        return encontradas;
    }

    
    private void decrementarEstoque(List<PecaModel> pecas) {
        for (PecaModel peca : pecas) {
            peca.setQuantidade(peca.getQuantidade() - 1);
        }
        pecaRepository.saveAll(pecas);
    }

    
    private void restaurarEstoque(List<PecaModel> pecas) {
        for (PecaModel peca : pecas) {
            peca.setQuantidade(peca.getQuantidade() + 1);
        }
        pecaRepository.saveAll(pecas);
    }


    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}