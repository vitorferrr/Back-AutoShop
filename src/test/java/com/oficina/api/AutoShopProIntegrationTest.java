package com.oficina.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Suite de testes de integração — Sprint 4
 *
 * Usa H2 em memória (configurado em src/test/resources/application.properties).
 * Cada teste roda em uma transação que é revertida ao final (@Transactional),
 * garantindo isolamento entre os casos.
 *
 * Os testes estão ordenados para que o token de autenticação seja obtido
 * no primeiro teste e reutilizado pelos demais.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class AutoShopProIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    // Token JWT compartilhado entre os testes
    private static String tokenJwt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // =========================================================================
    // Testes de Auth (Integrante 1)
    // =========================================================================

    /**
     * Caso 1: Registrar usuário com sucesso.
     * Esperado: 201 Created + body com token e dados do usuário.
     */
    @Test
    @Order(1)
    void caso1_deveRegistrarUsuarioComSucesso() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Mecânico Teste",
                          "email": "mecanico@autoshoppro.com",
                          "password": "senha1234"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value("Mecânico Teste"))
                .andExpect(jsonPath("$.user.email").value("mecanico@autoshoppro.com"))
                .andReturn();

        // Salva o token para os próximos testes
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        tokenJwt = body.get("token").asText();
    }

    /**
     * Caso 2: Email duplicado deve ser rejeitado.
     * Esperado: 409 Conflict.
     */
    @Test
    @Order(2)
    void caso2_naoDeveRegistrarEmailDuplicado() throws Exception {
        // Primeiro cadastro
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Dup","email":"dup@autoshoppro.com","password":"senha1234"}
                        """))
                .andExpect(status().isCreated());

        // Segundo cadastro com mesmo email
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Dup2","email":"dup@autoshoppro.com","password":"senha1234"}
                        """))
                .andExpect(status().isConflict());
    }

    /**
     * Caso 3: Login com credenciais corretas.
     * Esperado: 200 OK + token JWT.
     */
    @Test
    @Order(3)
    void caso3_deveLogarComCredenciaisCorretas() throws Exception {
        // Cria o usuário primeiro
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Login Test","email":"login@autoshoppro.com","password":"senha1234"}
                        """))
                .andExpect(status().isCreated());

        // Faz login
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"login@autoshoppro.com","password":"senha1234"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // =========================================================================
    // Testes de Cliente (Integrante 2)
    // =========================================================================

    /**
     * Caso 4: Criar cliente com CPF válido.
     * Esperado: 201 Created + dados do cliente.
     */
    @Test
    @Order(4)
    void caso4_deveCriarClienteComCpfValido() throws Exception {
        garantirToken();

        mockMvc.perform(post("/api/v1/clientes")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nomeCompleto": "João da Silva",
                          "cpfCnpj": "123.456.789-09",
                          "telefone": "(81) 98888-0001",
                          "email": "joao@email.com"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nomeCompleto").value("João da Silva"));
    }

    /**
     * Caso 5: CPF duplicado deve ser rejeitado.
     * Esperado: 422 Unprocessable Entity.
     */
    @Test
    @Order(5)
    void caso5_naoDeveCriarClienteComCpfDuplicado() throws Exception {
        garantirToken();

        String body = """
                {"nomeCompleto":"Maria","cpfCnpj":"111.222.333-44","email":"maria@e.com"}
                """;

        mockMvc.perform(post("/api/v1/clientes")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/clientes")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("CPF")));
    }

    // =========================================================================
    // Testes de Serviço (Integrante 4)
    // =========================================================================

    /**
     * Caso 6: Criar serviço e depois desativar via toggle.
     * Esperado: serviço criado com ativo=true; após toggle, ativo=false.
     */
    @Test
    @Order(6)
    void caso6_deveCriarServicoEToggleAtivo() throws Exception {
        garantirToken();

        // Cria o serviço
        MvcResult result = mockMvc.perform(post("/api/v1/servicos")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Troca de óleo","precoBase":80.00,"ativo":true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn();

        Long servicoId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asLong();

        // Toggle → desativa
        mockMvc.perform(patch("/api/v1/servicos/" + servicoId + "/toggle-ativo")
                .header("Authorization", "Bearer " + tokenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // Toggle novamente → reativa
        mockMvc.perform(patch("/api/v1/servicos/" + servicoId + "/toggle-ativo")
                .header("Authorization", "Bearer " + tokenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(true));
    }

    // =========================================================================
    // Testes de Peça e Estoque (Integrante 5)
    // =========================================================================

    /**
     * Caso 7: Criar peça com estoque 5, criar OS com essa peça, verificar estoque=4.
     * Esse é o teste central do Sprint 1 — garante o decremento de estoque.
     */
    @Test
    @Order(7)
    void caso7_deveDecrementarEstoqueAoCriarOrdemDeServico() throws Exception {
        garantirToken();

        // Cria cliente
        Long clienteId = criarCliente("Ana Mecânica", "999.888.777-66");

        // Cria veículo para o cliente
        Long veiculoId = criarVeiculo(clienteId, "TST-1111");

        // Cria serviço ativo
        Long servicoId = criarServico("Alinhamento", 150.00, true);

        // Cria peça com estoque = 5
        Long pecaId = criarPeca("Filtro de ar", 5);

        // Verifica estoque inicial
        mockMvc.perform(get("/api/v1/pecas/" + pecaId)
                .header("Authorization", "Bearer " + tokenJwt))
                .andExpect(jsonPath("$.quantidade").value(5));

        // Cria OS com a peça
        mockMvc.perform(post("/api/v1/ordens-servico")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                        {
                          "clienteId": %d,
                          "veiculoId": %d,
                          "servicoIds": [%d],
                          "pecaIds": [%d],
                          "descricaoProblema": "Motor barulhento",
                          "tipoCalculoPreco": "PADRAO"
                        }
                        """, clienteId, veiculoId, servicoId, pecaId)))
                .andExpect(status().isCreated());

        // Verifica que o estoque foi decrementado para 4
        mockMvc.perform(get("/api/v1/pecas/" + pecaId)
                .header("Authorization", "Bearer " + tokenJwt))
                .andExpect(jsonPath("$.quantidade").value(4));
    }

    /**
     * Caso 8: Tentar adicionar serviço inativo a uma OS deve ser rejeitado.
     * Esperado: 422 Unprocessable Entity.
     */
    @Test
    @Order(8)
    void caso8_naoDeveAceitarServicoInativoNaOrdem() throws Exception {
        garantirToken();

        Long clienteId = criarCliente("Pedro Teste", "777.666.555-44");
        Long veiculoId = criarVeiculo(clienteId, "TST-2222");
        Long servicoId = criarServico("Balanceamento", 90.00, false); // inativo!

        mockMvc.perform(post("/api/v1/ordens-servico")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                        {
                          "clienteId": %d,
                          "veiculoId": %d,
                          "servicoIds": [%d],
                          "pecaIds": [],
                          "descricaoProblema": "Vibração no volante",
                          "tipoCalculoPreco": "PADRAO"
                        }
                        """, clienteId, veiculoId, servicoId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("inativos")));
    }

    /**
     * Caso 9: OS criada com DESCONTO_10 deve ter valor = 90% do preço base do serviço.
     * Serviço com precoBase = 100.00 → valorTotal esperado = 90.00.
     */
    @Test
    @Order(9)
    void caso9_devCalcularPrecoComDesconto10() throws Exception {
        garantirToken();

        Long clienteId = criarCliente("Carlos Strategy", "444.333.222-11");
        Long veiculoId = criarVeiculo(clienteId, "TST-3333");
        Long servicoId = criarServico("Revisão completa", 100.00, true);

        mockMvc.perform(post("/api/v1/ordens-servico")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("""
                        {
                          "clienteId": %d,
                          "veiculoId": %d,
                          "servicoIds": [%d],
                          "pecaIds": [],
                          "descricaoProblema": "Revisão anual",
                          "tipoCalculoPreco": "DESCONTO_10"
                        }
                        """, clienteId, veiculoId, servicoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(90.00));
    }

    /**
     * Caso 10: Dashboard deve retornar contagens reais do banco.
     * Sem auth → 403. Com auth → 200 com os campos de resumo.
     */
    @Test
    @Order(10)
    void caso10_dashboardSemAuthRetorna403EComAuthRetorna200() throws Exception {
        // Sem token → deve bloquear
        mockMvc.perform(get("/api/v1/dashboard/resumo"))
                .andExpect(status().isForbidden());

        garantirToken();

        // Com token → retorna o resumo
        mockMvc.perform(get("/api/v1/dashboard/resumo")
                .header("Authorization", "Bearer " + tokenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes").isNumber())
                .andExpect(jsonPath("$.veiculos").isNumber())
                .andExpect(jsonPath("$.ordensAbertas").isNumber())
                .andExpect(jsonPath("$.pecasEstoqueBaixo").isNumber());
    }

    // =========================================================================
    // Helpers — reutilizados pelos testes
    // =========================================================================

    /**
     * Garante que tokenJwt está preenchido.
     * Cada teste pode chamar esse método para não depender da ordem de execução.
     */
    private void garantirToken() throws Exception {
        if (tokenJwt == null || tokenJwt.isBlank()) {
            MvcResult result = mockMvc.perform(post("/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Setup","email":"setup@autoshoppro.com","password":"senha1234"}
                            """))
                    .andReturn();
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
            tokenJwt = body.has("token") ? body.get("token").asText() : null;
        }
    }

    private Long criarCliente(String nome, String cpf) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/clientes")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"nomeCompleto\":\"%s\",\"cpfCnpj\":\"%s\"}", nome, cpf)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarVeiculo(Long clienteId, String placa) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/veiculos")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"clienteId\":%d,\"placa\":\"%s\",\"modelo\":\"Uno\",\"marca\":\"Fiat\"}",
                        clienteId, placa)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarServico(String nome, double preco, boolean ativo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/servicos")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"nome\":\"%s\",\"precoBase\":%.2f,\"ativo\":%b}",
                        nome, preco, ativo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long criarPeca(String nome, int quantidade) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pecas")
                .header("Authorization", "Bearer " + tokenJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"nome\":\"%s\",\"quantidade\":%d,\"estoqueMinimo\":1,\"precoUnitario\":25.00}",
                        nome, quantidade)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}