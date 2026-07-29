package com.oficina.api.repository;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import com.oficina.api.model.VeiculoModel;
 
@Repository
public interface VeiculoRepository extends JpaRepository<VeiculoModel, Long> {
 
    Optional<VeiculoModel> findByPlaca(String placa);
 
    boolean existsByPlaca(String placa);
 
    @Query("SELECT COUNT(v) > 0 FROM VeiculoModel v WHERE v.placa = :placa AND v.id <> :id")
    boolean existsPlacaEmOutroVeiculo(@Param("placa") String placa, @Param("id") Long id);

    List<VeiculoModel> findByClienteId(Long clienteId);
 
    @Query("""
        SELECT v FROM VeiculoModel v
        WHERE (:clienteId IS NULL OR v.cliente.id = :clienteId)
          AND (:termo IS NULL OR :termo = ''
               OR LOWER(v.placa) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(v.modelo) LIKE LOWER(CONCAT('%', :termo, '%'))
               OR LOWER(v.marca) LIKE LOWER(CONCAT('%', :termo, '%')))
    """)
    Page<VeiculoModel> buscar(@Param("termo") String termo,
                              @Param("clienteId") Long clienteId,
                              Pageable pageable);
}