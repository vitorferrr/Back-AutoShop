package com.oficina.api.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oficina.api.dto.ServicoRequestDTO;
import com.oficina.api.dto.ServicoResponseDTO;
import com.oficina.api.exception.RecursoNaoEncontradoException;
import com.oficina.api.model.ServicoModel;
import com.oficina.api.repository.ServicoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;

    @Transactional
    public ServicoResponseDTO criar(ServicoRequestDTO request) {
        ServicoModel servico = new ServicoModel();
        preencher(servico, request);
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    @Transactional(readOnly = true)
    public Page<ServicoResponseDTO> listar(String busca, Pageable pageable) {
        return servicoRepository.buscar(busca == null ? null : busca.trim(), pageable)
                .map(ServicoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public ServicoResponseDTO buscarPorId(Long id) {
        return ServicoResponseDTO.from(buscarModelPorId(id));
    }

    @Transactional
    public ServicoResponseDTO atualizar(Long id, ServicoRequestDTO request) {
        ServicoModel servico = buscarModelPorId(id);
        preencher(servico, request);
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    /**
     * Alterna o estado ativo/inativo do serviço.
     * Se estava ativo, passa para inativo. Se estava inativo, passa para ativo.
     */
    @Transactional
    public ServicoResponseDTO toggleAtivo(Long id) {
        ServicoModel servico = buscarModelPorId(id);
        servico.setAtivo(!Boolean.TRUE.equals(servico.getAtivo()));
        return ServicoResponseDTO.from(servicoRepository.save(servico));
    }

    @Transactional
    public void excluir(Long id) {
        if (!servicoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Serviço não encontrado com id: " + id);
        }
        servicoRepository.deleteById(id);
    }

    public ServicoModel buscarModelPorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Serviço não encontrado com id: " + id));
    }

    private void preencher(ServicoModel servico, ServicoRequestDTO request) {
        servico.setNome(request.nome().trim());
        servico.setDescricao(limpar(request.descricao()));
        servico.setTempoEstimadoMinutos(request.tempoEstimadoMinutos());
        servico.setPrecoBase(request.precoBase() == null ? BigDecimal.ZERO : request.precoBase());
        servico.setAtivo(request.ativo() == null ? true : request.ativo());
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}