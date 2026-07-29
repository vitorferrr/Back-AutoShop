package com.oficina.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.oficina.api.dto.ServicoRequestDTO;
import com.oficina.api.dto.ServicoResponseDTO;
import com.oficina.api.service.ServicoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    @GetMapping
    public Page<ServicoResponseDTO> listar(
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return servicoService.listar(busca, pageable);
    }

    @GetMapping("/{id}")
    public ServicoResponseDTO buscarPorId(@PathVariable Long id) {
        return servicoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoResponseDTO criar(@RequestBody @Valid ServicoRequestDTO request) {
        return servicoService.criar(request);
    }

    @PutMapping("/{id}")
    public ServicoResponseDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ServicoRequestDTO request) {
        return servicoService.atualizar(id, request);
    }

    /**
     * endpoint para ativar/desativar serviço.
     * O frontend pode chamar PATCH /api/v1/servicos/{id}/toggle-ativo
     * sem precisar enviar o corpo completo do serviço.
     * Retorna o serviço atualizado com o novo estado de 'ativo'.
     */
    @PatchMapping("/{id}/toggle-ativo")
    public ServicoResponseDTO toggleAtivo(@PathVariable Long id) {
        return servicoService.toggleAtivo(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        servicoService.excluir(id);
    }
}