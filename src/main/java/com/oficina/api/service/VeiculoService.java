package com.oficina.api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oficina.api.dto.VeiculoRequestDTO;
import com.oficina.api.dto.VeiculoResponseDTO;
import com.oficina.api.exception.RecursoNaoEncontradoException;
import com.oficina.api.exception.RegraDeNegocioException;
import com.oficina.api.model.ClienteModel;
import com.oficina.api.model.VeiculoModel;
import com.oficina.api.repository.VeiculoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteService clienteService;

    @Transactional
    public VeiculoResponseDTO criar(VeiculoRequestDTO request) {
        validarPlacaNova(request.placa());
        VeiculoModel veiculo = new VeiculoModel();
        preencher(veiculo, request);
        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional(readOnly = true)
    public Page<VeiculoResponseDTO> listar(String busca, Long clienteId, Pageable pageable) {
        return veiculoRepository.buscar(normalizarBusca(busca), clienteId, pageable)
                .map(VeiculoResponseDTO::from);
    }

    /**
     * Retorna todos os veículos de um cliente sem paginação e
     * valida que o cliente existe antes de buscar os veículos.
     */
    @Transactional(readOnly = true)
    public List<VeiculoResponseDTO> listarPorCliente(Long clienteId) {
        clienteService.buscarModelPorId(clienteId);
        return veiculoRepository.findByClienteId(clienteId)
                .stream()
                .map(VeiculoResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponseDTO buscarPorId(Long id) {
        return VeiculoResponseDTO.from(buscarModelPorId(id));
    }

    @Transactional
    public VeiculoResponseDTO atualizar(Long id, VeiculoRequestDTO request) {
        VeiculoModel veiculo = buscarModelPorId(id);
        String placaNova = normalizarPlaca(request.placa());
        if (!placaNova.equals(veiculo.getPlaca())) {
            validarPlacaEdicao(placaNova, id);
        }
        preencher(veiculo, request);
        return VeiculoResponseDTO.from(veiculoRepository.save(veiculo));
    }

    @Transactional
    public void excluir(Long id) {
        if (!veiculoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Veículo não encontrado com id: " + id);
        }
        veiculoRepository.deleteById(id);
    }

    public VeiculoModel buscarModelPorId(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Veículo não encontrado com id: " + id));
    }

    private void preencher(VeiculoModel veiculo, VeiculoRequestDTO request) {
        ClienteModel cliente = clienteService.buscarModelPorId(request.clienteId());
        veiculo.setCliente(cliente);
        veiculo.setPlaca(normalizarPlaca(request.placa()));
        veiculo.setMarca(limpar(request.marca()));
        veiculo.setModelo(request.modelo().trim());
        veiculo.setAno(request.ano());
        veiculo.setHistoricoServicos(limpar(request.historicoServicos()));
    }

    private void validarPlacaNova(String placa) {
        String normalizada = normalizarPlaca(placa);
        if (veiculoRepository.existsByPlaca(normalizada)) {
            throw new RegraDeNegocioException("Já existe veículo cadastrado com esta placa.");
        }
    }

    private void validarPlacaEdicao(String placa, Long idAtual) {
        if (veiculoRepository.existsPlacaEmOutroVeiculo(placa, idAtual)) {
            throw new RegraDeNegocioException("Já existe outro veículo com esta placa.");
        }
    }

    private String normalizarPlaca(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String normalizarBusca(String busca) {
        return busca == null ? null : busca.trim();
    }
}