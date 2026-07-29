package com.oficina.api.controller;

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

import com.oficina.api.dto.VeiculoRequestDTO;
import com.oficina.api.dto.VeiculoResponseDTO;
import com.oficina.api.service.VeiculoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/veiculos")
@RequiredArgsConstructor
public class VeiculoController {
    private final VeiculoService veiculoService;

    @GetMapping
    public Page<VeiculoResponseDTO> listar(
        @RequestParam(required = false) String busca,
        @RequestParam(required = false) Long clienteId,
        @PageableDefault(size = 10, sort = "placa", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return veiculoService.listar(busca, clienteId, pageable);
    }

    @GetMapping("/{id}")
    public VeiculoResponseDTO buscarPorId(@PathVariable Long id) {
        return veiculoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VeiculoResponseDTO criar(@RequestBody @Valid VeiculoRequestDTO request) {
        return veiculoService.criar(request);
    }

    @PutMapping("/{id}")
    public VeiculoResponseDTO atualizar(@PathVariable Long id, @RequestBody @Valid VeiculoRequestDTO request) {
        return veiculoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        veiculoService.excluir(id);
    }
    
}
