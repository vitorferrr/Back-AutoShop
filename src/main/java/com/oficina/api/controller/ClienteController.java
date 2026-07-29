package com.oficina.api.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.oficina.api.dto.ClienteRequestDTO;
import com.oficina.api.dto.ClienteResponseDTO;
import com.oficina.api.dto.VeiculoResponseDTO;
import com.oficina.api.service.ClienteService;
import com.oficina.api.service.VeiculoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final VeiculoService veiculoService;

    @GetMapping
    public Page<ClienteResponseDTO> listar(
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "nomeCompleto", direction = Sort.Direction.ASC) Pageable pageable) {
        return clienteService.listar(busca, pageable);
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    /**
     * Retorna todos os veículos de um cliente específico.
     */
    @GetMapping("/{id}/veiculos")
    public List<VeiculoResponseDTO> veiculosDoCliente(@PathVariable Long id) {
        return veiculoService.listarPorCliente(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponseDTO criar(@RequestBody @Valid ClienteRequestDTO request) {
        return clienteService.criar(request);
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ClienteRequestDTO request) {
        return clienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        clienteService.excluir(id);
    }
}