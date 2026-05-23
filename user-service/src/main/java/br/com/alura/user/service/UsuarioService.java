package br.com.alura.user.service;

import br.com.alura.user.dto.UsuarioDTO;
import br.com.alura.user.exception.RegraDeNegocioException;
import br.com.alura.user.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.alura.user.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<UsuarioDTO> listar(Pageable paginacao) {
        return repository.findAll(paginacao).map(UsuarioDTO::new);
    }

    public UsuarioDTO buscarPorId(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário com ID " + id + " não encontrado."));
        return new UsuarioDTO(usuario);
    }

    @Transactional
    public UsuarioDTO criar(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        usuario.setRole(dto.getRole() != null ? dto.getRole() : "USER");

        Usuario salvo = repository.save(usuario);
        return new UsuarioDTO(salvo);
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário com ID " + id + " não encontrado."));

        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        if (dto.getRole() != null) {
            usuario.setRole(dto.getRole());
        }

        return new UsuarioDTO(usuario);
    }

    @Transactional
    public void remover(Long id) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário com ID " + id + " não encontrado."));
        repository.delete(usuario);
    }
}
