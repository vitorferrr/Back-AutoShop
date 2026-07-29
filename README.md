# AutoShop Pro — Backend API

## Integrantes da equipe

| Integrante | Entidade principal | Responsabilidades |
|---|---|---|
|  Rafael Assis  | Usuário / Auth | UsuarioModel, AuthController, JWT, SecurityConfig, PasswordReset |
| Vitor Ferreira | Cliente | ClienteModel, ClienteService, ClienteController, ClienteDTO |
|     Sergio     | Veículo | VeiculoModel, VeiculoService, VeiculoController, VeiculoDTO |
| Vitor Ferreira | Serviço | ServicoModel, ServicoService, ServicoController, toggle-ativo |
| Vitor Ferreira | Peça / OrdemServico | PecaModel, OrdemServicoModel, controle de estoque, Strategy de preço |

---

## Sobre o projeto

API REST em Spring Boot para o sistema de gerenciamento de oficina mecânica **AutoShop Pro**.  
Desenvolvida como projeto integrador da disciplina de POO + Frontend.

---

## Tecnologias

- Java 17 + Spring Boot 3
- Spring Security (stateless, JWT manual HMAC-SHA256)
- Spring Data JPA + PostgreSQL (Supabase em produção)
- H2 (banco em memória para testes)
- Lombok, SpringDoc (Swagger)
- Design pattern: **Strategy** (cálculo de preço das ordens de serviço)

---

## Configuração local

Crie um arquivo `.env` na raiz do projeto com base no `.env.example`:

```properties
DB_URL=jdbc:postgresql://db.<SEU-PROJECT-REF>.supabase.co:5432/postgres
DB_USER=postgres
DB_PASSWORD=sua-senha-supabase
JWT_SECRET=segredo-longo-e-aleatorio-aqui
JWT_EXPIRATION_HOURS=8
MAIL_USERNAME=seu@gmail.com
MAIL_PASSWORD=senha-de-app-gmail
FRONTEND_URL=http://localhost:3000
```

### Rodando com perfil local (H2, sem .env)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Rodando com banco real (PostgreSQL)

```bash
./mvnw spring-boot:run
```

---

## Documentação interativa (Swagger)

Com o servidor rodando, acesse:  
`http://localhost:8080/swagger-ui.html`

---

## Endpoints principais

### Autenticação (público)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastrar mecânico/usuário |
| POST | `/auth/login` | Fazer login — retorna JWT |
| GET | `/auth/me` | Dados do usuário logado |
| POST | `/auth/forgot-password` | Solicitar link de recuperação |
| POST | `/auth/reset-password` | Redefinir senha com token |

### Recursos protegidos (exigem `Authorization: Bearer {token}`)

| Método | Rota | Descrição |
|---|---|---|
| GET/POST | `/api/v1/clientes` | Listar e criar clientes |
| GET/PUT/DELETE | `/api/v1/clientes/{id}` | Buscar, editar e excluir |
| GET | `/api/v1/clientes/{id}/veiculos` | Veículos de um cliente |
| GET/POST | `/api/v1/veiculos` | Listar e cadastrar veículos |
| GET/PUT/DELETE | `/api/v1/veiculos/{id}` | Buscar, editar e excluir |
| GET/POST | `/api/v1/servicos` | Catálogo de serviços |
| PATCH | `/api/v1/servicos/{id}/toggle-ativo` | Ativar/desativar serviço |
| GET/POST | `/api/v1/pecas` | Estoque de peças |
| GET/POST | `/api/v1/ordens-servico` | Ordens de serviço (CRUD completo) |
| GET | `/api/v1/dashboard/resumo` | Resumo com contagens reais |

### Regras de negócio implementadas

- Ao criar uma Ordem de Serviço, o estoque de cada peça é decrementado em 1
- Ao excluir uma OS, o estoque é restaurado
- Serviços com `ativo = false` não podem ser adicionados a uma OS
- Veículo deve pertencer ao cliente informado na OS
- CPF/CNPJ do cliente é único no sistema
- Placa do veículo é única no sistema
- Strategy Pattern para cálculo de preço: `PADRAO`, `DESCONTO_10` (10% off) e `URGENCIA_20` (+20%)
