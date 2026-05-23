package br.com.alura.user.model;

import br.com.alura.user.security.TotpSecretConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String senha;

    @Column(nullable = false)
    private String role = "USER";

    /** "LOCAL", "GOOGLE", etc. */
    @Column(name = "provider_type", nullable = false)
    private String providerType = "LOCAL";

    /** ID único no provedor externo (ex.: sub do Google). */
    @Column(name = "provider_id")
    private String providerId;

    @Convert(converter = TotpSecretConverter.class)
    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_ativo", nullable = false)
    private boolean totpAtivo = false;
}
