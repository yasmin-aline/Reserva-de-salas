package br.com.alura.user.controller;

import br.com.alura.user.dto.UsuarioDTO;
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
import br.com.alura.user.service.UsuarioService;

@RestController
@RequestMapping("/api/v1/usuarios")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuários", description = "Gerenciamento de usuários (requer ADMIN)")
public class ControllerUsuario {

    private final UsuarioService service;

    public ControllerUsuario(UsuarioService service) {
        this.service = service;
    }

    @Operation(summary = "Listar usuários paginados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<UsuarioDTO> listar(@PageableDefault(size = 10) Pageable paginacao) {
        return service.listar(paginacao);
    }

    @Operation(summary = "Buscar usuário por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UsuarioDTO buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @Operation(summary = "Criar usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO criar(@RequestBody UsuarioDTO dto) {
        return service.criar(dto);
    }

    @Operation(summary = "Atualizar usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public UsuarioDTO atualizar(@PathVariable Long id, @RequestBody UsuarioDTO dto) {
        return service.atualizar(id, dto);
    }

    @Operation(summary = "Remover usuário")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Usuário removido"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
