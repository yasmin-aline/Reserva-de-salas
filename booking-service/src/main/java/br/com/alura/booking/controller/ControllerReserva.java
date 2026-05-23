package br.com.alura.booking.controller;

import br.com.alura.booking.dto.ReservaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.alura.booking.service.ReservaService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/reservas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reservas", description = "Gerenciamento de reservas de salas")
public class ControllerReserva {

    private final ReservaService service;

    public ControllerReserva(ReservaService service) {
        this.service = service;
    }

    @Operation(summary = "Listar reservas paginadas (público)")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    public Page<ReservaDTO> listar(@PageableDefault(size = 10) Pageable paginacao) {
        return service.listar(paginacao);
    }

    @Operation(summary = "Listar reservas de uma sala (público)")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/sala/{salaId}")
    public Page<ReservaDTO> listarPorSala(@PathVariable Long salaId,
            @PageableDefault(size = 10) Pageable paginacao) {
        return service.listarPorSala(salaId, paginacao);
    }

    @Operation(summary = "Buscar reservas por intervalo de datas (público)")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping("/intervalo")
    public Page<ReservaDTO> buscarPorIntervalo(
            @RequestParam Long salaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @PageableDefault(size = 10) Pageable paginacao) {
        return service.buscarPorIntervalo(salaId, inicio, fim, paginacao);
    }

    @Operation(summary = "Buscar reserva por ID (público)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
        @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    @GetMapping("/{id}")
    public ReservaDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar reserva")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserva criada"),
        @ApiResponse(responseCode = "401", description = "Não autenticado"),
        @ApiResponse(responseCode = "422", description = "Conflito de horário")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDTO criar(@RequestBody ReservaDTO dto) {
        return service.criar(dto);
    }

    @Operation(summary = "Atualizar reserva")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reserva atualizada"),
        @ApiResponse(responseCode = "404", description = "Reserva não encontrada"),
        @ApiResponse(responseCode = "422", description = "Conflito de horário")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{id}")
    public ReservaDTO atualizar(@PathVariable Long id, @RequestBody ReservaDTO dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Cancelar reserva")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Reserva cancelada"),
        @ApiResponse(responseCode = "404", description = "Reserva não encontrada")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id) {
        service.cancelarReserva(id);
    }
}
