package com.oficina.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oficina.api.dto.ClienteRequestDTO;
import com.oficina.api.dto.ClienteResponseDTO;
import com.oficina.api.exception.RecursoNaoEncontradoException;
import com.oficina.api.exception.RegraDeNegocioException;
import com.oficina.api.model.ClienteModel;
import com.oficina.api.repository.ClienteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponseDTO criar(ClienteRequestDTO request) {
        validarCpfCnpjNovo(request.cpfCnpj());
        ClienteModel novoCliente = new ClienteModel();
        preencher(novoCliente, request);
        return ClienteResponseDTO.from(clienteRepository.save(novoCliente));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> listar(String busca, Pageable pageable) {
        return clienteRepository.buscar(normalizarBusca(busca), pageable).map(ClienteResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return ClienteResponseDTO.from(buscarModelPorId(id));
    }

    @Transactional
    public ClienteResponseDTO atualizar(Long id, ClienteRequestDTO request) {
        ClienteModel cliente = buscarModelPorId(id);
        if (!normalizarCpfCnpj(request.cpfCnpj()).equals(normalizarCpfCnpj(cliente.getCpfCnpj()))) {
            validarCpfCnpjEdicao(request.cpfCnpj(), id);
        }
        preencher(cliente, request);
        return ClienteResponseDTO.from(clienteRepository.save(cliente));
    }

    @Transactional
    public void excluir(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado com id: " + id);
        }
        clienteRepository.deleteById(id);
    }

    public ClienteModel buscarModelPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado com id: " + id));
    }

    private void preencher(ClienteModel cliente, ClienteRequestDTO request) {
        cliente.setNomeCompleto(request.nomeCompleto().trim());
        cliente.setCpfCnpj(normalizarCpfCnpj(request.cpfCnpj()));
        cliente.setTelefone(limpar(request.telefone()));
        cliente.setEmail(limpar(request.email()));
        cliente.setEndereco(limpar(request.endereco()));
    }

    private void validarCpfCnpjNovo(String cpfCnpj) {
        String normalizado = normalizarCpfCnpj(cpfCnpj);
        validarTamanhoCpfCnpj(normalizado);
        if (clienteRepository.existsByCpfCnpj(normalizado)) {
            throw new RegraDeNegocioException("Já existe cliente cadastrado com este CPF/CNPJ.");
        }
    }

    private void validarCpfCnpjEdicao(String cpfCnpj, Long idAtual) {
        String normalizado = normalizarCpfCnpj(cpfCnpj);
        validarTamanhoCpfCnpj(normalizado);
        if (clienteRepository.existsCpfCnpjEmOutroCliente(normalizado, idAtual)) {
            throw new RegraDeNegocioException("Já existe outro cliente com este CPF/CNPJ.");
        }
    }

    private void validarTamanhoCpfCnpj(String valor) {
        String digitos = valor.replaceAll("[^0-9]", "");
        if (digitos.length() != 11 && digitos.length() != 14) {
            throw new RegraDeNegocioException("CPF deve ter 11 dígitos ou CNPJ deve ter 14 dígitos.");
        }
    }

    private String normalizarCpfCnpj(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private String normalizarBusca(String busca) {
        return busca == null ? null : busca.trim();
    }
}
