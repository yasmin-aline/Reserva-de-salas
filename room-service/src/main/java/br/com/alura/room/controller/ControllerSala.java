package br.com.alura.room.controller;

import br.com.alura.room.dto.SalaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.alura.room.service.SalaService;

@RestController
@RequestMapping("/api/v1/salas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Salas", description = "Gerenciamento de salas de reunião")
public class ControllerSala {

    private final SalaService service;

    public ControllerSala(SalaService service) {
        this.service = service;
    }

    @Operation(summary = "Listar salas paginadas")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public Page<SalaDTO> listar(@PageableDefault(size = 10) Pageable paginacao) {
        return service.listar(paginacao);
    }

    @Operation(summary = "Buscar sala por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sala encontrada"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    @GetMapping("/{id}")
    public SalaDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar sala (ADMIN)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sala criada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaDTO criar(@RequestBody SalaDTO dto) {
        return service.criar(dto);
    }

    @Operation(summary = "Atualizar sala (ADMIN)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sala atualizada"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public SalaDTO atualizar(@PathVariable Long id, @RequestBody SalaDTO dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Remover sala (ADMIN)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sala removida"),
        @ApiResponse(responseCode = "404", description = "Sala não encontrada")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
