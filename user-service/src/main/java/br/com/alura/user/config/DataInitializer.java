package br.com.alura.user.config;

import br.com.alura.user.model.Usuario;
import br.com.alura.user.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        upsert("Admin", "admin@reserva.com", "admin123", "ADMIN");
        upsert("Usuario Teste", "user@reserva.com", "user123", "USER");
        System.out.println(">>> DataInitializer OK: admin@reserva.com/admin123 | user@reserva.com/user123");
    }

    private void upsert(String nome, String email, String senha, String role) {
        Usuario u = repository.findByEmail(email).orElse(new Usuario());
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(passwordEncoder.encode(senha));
        u.setRole(role);
        u.setTotpAtivo(false);
        u.setTotpSecret(null);
        repository.save(u);
    }
}
