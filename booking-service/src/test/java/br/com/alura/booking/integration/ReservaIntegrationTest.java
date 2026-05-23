package br.com.alura.booking.integration;

import br.com.alura.booking.event.ReservaCriadaEvent;
import br.com.alura.booking.messaging.AnaliticoConsumer;
import br.com.alura.booking.messaging.NotificacaoConsumer;
import br.com.alura.booking.model.StatusReserva;
import br.com.alura.booking.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Import(ClientsMockConfig.class)
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class ReservaIntegrationTest {

    // ─── Contêineres efêmeros — static para serem reutilizados entre os testes ───

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("test_booking")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3-management"));

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",          mysql::getJdbcUrl);
        registry.add("spring.datasource.username",     mysql::getUsername);
        registry.add("spring.datasource.password",     mysql::getPassword);
        registry.add("spring.rabbitmq.host",           rabbit::getHost);
        registry.add("spring.rabbitmq.port",           rabbit::getAmqpPort);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    // ─── Injeções ───

    @Autowired TestRestTemplate restTemplate;
    @Autowired ReservaRepository reservaRepository;
    @Autowired NotificacaoConsumer notificacaoConsumer;
    @Autowired AnaliticoConsumer analiticoConsumer;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${reserva.rabbitmq.exchange}") String exchange;
    @Value("${reserva.rabbitmq.routingkey}") String routingKey;
    @Value("${reserva.kafka.topic}") String kafkaTopic;

    private static final String USER_TOKEN  = JwtTestHelper.tokenFor(1L, "USER");
    private static final String ADMIN_TOKEN = JwtTestHelper.tokenFor(2L, "ADMIN");

    @BeforeEach
    void setup() {
        reservaRepository.deleteAll();
        notificacaoConsumer.resetCounters();
        analiticoConsumer.resetCounters();
    }

    // ─── Fluxo de reserva (HTTP ponta-a-ponta) ───

    @Test
    @DisplayName("POST /reservas com JWT USER → 201 e persiste no banco")
    void deveCriarReservaComSucesso() {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/v1/reservas",
                comJwt(USER_TOKEN, dto(1L, 1L,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2))),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).containsEntry("status", "ATIVA");
        assertThat(reservaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /reservas sem token → 401")
    void deveRetornar401SemToken() {
        // Enviamos sem corpo para evitar streaming mode do HttpURLConnection;
        // o filtro de segurança retorna 401 antes de processar qualquer body.
        ResponseEntity<String> resp = restTemplate.postForEntity(
                "/api/v1/reservas",
                null,
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /reservas com conflito de horário → 422")
    void deveRetornar422EmConflito() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(5)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusHours(2);

        restTemplate.postForEntity("/api/v1/reservas",
                comJwt(USER_TOKEN, dto(1L, 1L, inicio, fim)), Map.class);

        ResponseEntity<String> conflito = restTemplate.postForEntity("/api/v1/reservas",
                comJwt(USER_TOKEN, dto(1L, 2L, inicio.plusMinutes(30), fim.plusHours(1))),
                String.class);

        assertThat(conflito.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(conflito.getBody()).containsIgnoringCase("conflito");
    }

    @Test
    @DisplayName("DELETE /reservas/{id} → 204 e status CANCELADA no banco")
    void deveCancelarReserva() {
        ResponseEntity<Map> criada = restTemplate.postForEntity("/api/v1/reservas",
                comJwt(USER_TOKEN, dto(1L, 1L,
                        LocalDateTime.now().plusDays(3),
                        LocalDateTime.now().plusDays(3).plusHours(1))),
                Map.class);
        Long id = ((Number) criada.getBody().get("id")).longValue();

        restTemplate.exchange("/api/v1/reservas/" + id, HttpMethod.DELETE,
                new HttpEntity<>(headers(USER_TOKEN)), Void.class);

        assertThat(reservaRepository.findById(id))
                .isPresent()
                .hasValueSatisfying(r -> assertThat(r.getStatus()).isEqualTo(StatusReserva.CANCELADA));
    }

    @Test
    @DisplayName("GET /reservas sem token → 200 (endpoint público)")
    void deveListarSemAutenticacao() {
        assertThat(restTemplate.getForEntity("/api/v1/reservas", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    // ─── Consumidores de mensagens ───

    @Test
    @DisplayName("Criar reserva → NotificacaoConsumer processa evento via RabbitMQ")
    void deveTriggerNotificacaoConsumer() {
        restTemplate.postForEntity("/api/v1/reservas",
                comJwt(USER_TOKEN, dto(1L, 1L,
                        LocalDateTime.now().plusDays(7),
                        LocalDateTime.now().plusDays(7).plusHours(1))),
                Map.class);

        await().atMost(ofSeconds(15))
               .untilAsserted(() ->
                       assertThat(notificacaoConsumer.getProcessedCount()).isGreaterThan(0));
    }

    @Test
    @DisplayName("Criar reserva → AnaliticoConsumer processa evento via Kafka")
    void deveTriggerAnaliticoConsumer() {
        restTemplate.postForEntity("/api/v1/reservas",
                comJwt(USER_TOKEN, dto(1L, 1L,
                        LocalDateTime.now().plusDays(14),
                        LocalDateTime.now().plusDays(14).plusHours(1))),
                Map.class);

        await().atMost(ofSeconds(20))
               .untilAsserted(() ->
                       assertThat(analiticoConsumer.getProcessedCount()).isGreaterThan(0));
    }

    @Test
    @DisplayName("Mesmo evento publicado duas vezes no RabbitMQ → processado apenas uma vez (idempotência)")
    void deveIgnorarEventoDuplicadoRabbitMQ() throws InterruptedException {
        ReservaCriadaEvent event = new ReservaCriadaEvent(
                99L, 1L, 1L,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1));

        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);

        // Aguarda o primeiro processamento
        await().atMost(ofSeconds(10))
               .until(() -> notificacaoConsumer.getProcessedCount() >= 1);

        // Tempo extra para garantir que a segunda mensagem também foi entregue e ignorada
        Thread.sleep(1500);

        assertThat(notificacaoConsumer.getProcessedCount())
                .as("Evento duplicado não deve ser processado novamente")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Publicar evento diretamente no Kafka → AnaliticoConsumer processa")
    void deveConsumirEventoKafkaDireto() {
        ReservaCriadaEvent event = new ReservaCriadaEvent(
                100L, 2L, 1L,
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusHours(1));

        kafkaTemplate.send(kafkaTopic, event.getEventId(), event);

        await().atMost(ofSeconds(20))
               .untilAsserted(() ->
                       assertThat(analiticoConsumer.getProcessedCount()).isGreaterThan(0));
    }

    // ─── Helpers ───

    private Map<String, Object> dto(Long salaId, Long usuarioId,
                                    LocalDateTime inicio, LocalDateTime fim) {
        return Map.of(
                "salaId", salaId,
                "usuarioId", usuarioId,
                "inicio", inicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "fim", fim.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private <T> HttpEntity<T> comJwt(String token, T body) {
        return new HttpEntity<>(body, headers(token));
    }
}
