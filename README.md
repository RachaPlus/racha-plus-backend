# ⚽ RachaPlus API

[![CI - Back-end](https://github.com/RachaPlus/racha-plus-backend/actions/workflows/ci-backend.yml/badge.svg)](https://github.com/RachaPlus/racha-plus-backend/actions/workflows/ci-backend.yml)
[![Swagger UI](https://img.shields.io/badge/Swagger-OpenAPI%203-brightgreen)](https://api.rachaplus.site/swagger-ui/index.html)

API RESTful da plataforma **RachaPlus**, desenvolvida para gerenciar e organizar partidas esportivas amadoras ("rachas" ou "peladas"), permitindo controle de usuários, formação de grupos de partidas, gestão de membros e balanceamento de equipes.

---

## 📌 Sumário
- [🎯 Objetivos do Projeto](#-objetivos-do-projeto)
- [🛠️ Tecnologias Utilizadas](#️-tecnologias-utilizadas)
- [📋 Pré-requisitos](#-pré-requisitos)
- [⚙️ Variáveis de Ambiente](#️-variáveis-de-ambiente)
- [🚀 Como Executar o Projeto](#-como-executar-o-projeto)
  - [Opção 1: Via Docker Compose (Recomendado)](#opção-1-via-docker-compose-recomendado)
  - [Opção 2: Localmente via Maven](#opção-2-localmente-via-maven)
- [🧪 Como Rodar os Testes](#-como-rodar-os-testes)
- [📖 Documentação Interativa da API (Swagger / OpenAPI)](#-documentação-interativa-da-api-swagger--openapi)
- [🌐 Principais Endpoints](#-principais-endpoints)
- [🏗️ Estrutura do Projeto](#️-estrutura-do-projeto)
- [🔄 Pipeline de CI/CD](#-pipeline-de-cicd)

---

## 🎯 Objetivos do Projeto

O **RachaPlus** resolve a dificuldade enfrentada por organizadores e jogadores de partidas esportivas amadoras:
- **Gestão Simplificada de Grupos**: Criação de rachas com controle de organizador (`ADMIN`) e jogadores (`PLAYER`).
- **Autenticação Segura**: Controle de acesso stateless via tokens JWT para proteção das informações dos participantes.
- **Participação e Integração**: Inclusão de membros em grupos com cálculo de avaliação esportiva (*rating*).
- **Escalabilidade e Qualidade**: Pipeline contínua de testes e documentação padronizada via OpenAPI 3.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** [Java 21 (LTS)](https://www.oracle.com/java/technologies/downloads/#java21)
- **Framework Principal:** [Spring Boot 3.5](https://spring.io/projects/spring-boot)
  - **Spring Web**: Criação dos controladores REST.
  - **Spring Data JPA & Hibernate**: Persistência de dados e mapeamento objeto-relacional.
  - **Spring Security**: Autenticação, autorização e políticas stateless.
  - **Spring Validation / Bean Validation**: Validação das entradas dos DTOs.
- **Banco de Dados:** [PostgreSQL 16](https://www.postgresql.org/)
- **Autenticação:** [Java JWT (jjwt 0.12.5)](https://github.com/jwtk/jjwt)
- **Documentação de API:** [Springdoc OpenAPI UI 2.8.5](https://springdoc.org/) (Swagger 3)
- **Testes Automatizados:** [JUnit 5](https://junit.org/junit5/), [Mockito](https://site.mockito.org/) e Spring MockMvc
- **Containerização:** [Docker](https://www.docker.com/) e [Docker Compose](https://docs.docker.com/compose/)
- **CI/CD:** [GitHub Actions](https://github.com/features/actions)

---

## 📋 Pré-requisitos

Para rodar este projeto em ambiente local, você precisará de:
- **Git** instalado.
- **JDK 21** instalado e configurado (`JAVA_HOME`).
- **Docker** e **Docker Compose** (caso prefira rodar o banco de dados em container).

---

## ⚙️ Variáveis de Ambiente

Crie um arquivo `.env` na raiz do repositório baseado no modelo [.env.example](file:///.env.example):

```bash
cp .env.example .env
```

Exemplo de configuração para ambiente local:

```env
# Configurações do Banco de Dados PostgreSQL
DB_HOST=localhost
DB_NAME=rachaplus
DB_USER=postgres
DB_PASSWORD=suasenha_aqui

# JWT Secret - Chave de criptografia de no mínimo 32 caracteres
JWT_SECRET=chave-secreta-de-desenvolvimento-com-minimo-32-caracteres

# CORS - Origens permitidas separadas por vírgula
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://rachaplus.site,https://www.rachaplus.site
```

---

## 🚀 Como Executar o Projeto

### Opção 1: Via Docker Compose (Recomendado para subir o Banco)

Suba o container do PostgreSQL em background:

```bash
docker compose up -d
```

Em seguida, execute a aplicação com o Maven Wrapper:

```bash
cd rachaplus-api
./mvnw spring-boot:run
```

### Opção 2: Localmente via Maven

Se você já possuir uma instância do PostgreSQL rodando localmente com a base criada:

```bash
cd rachaplus-api
./mvnw clean package -DskipTests
java -jar target/rachaplus-api-0.0.1-SNAPSHOT.jar
```

A API estará disponível em: **`http://localhost:8080`**

---

## 🧪 Como Rodar os Testes

O projeto conta com suítes de testes automatizados e testes de integração com MockMvc:

```bash
cd rachaplus-api

# Executar todos os testes
./mvnw test

# Executar apenas uma classe de teste específica
./mvnw test -Dtest=UsuarioControllerTest
./mvnw test -Dtest=AutenticacaoControllerTest
./mvnw test -Dtest=RachaControllerTest
```

Os relatórios detalhados da execução dos testes são gerados na pasta:
`rachaplus-api/target/surefire-reports/`

---

## 📖 Documentação Interativa da API (Swagger / OpenAPI)

Com a aplicação rodando, acesse a documentação interativa para testar as requisições diretamente pelo navegador:

- **Local:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Produção:** [https://api.rachaplus.site/swagger-ui/index.html](https://api.rachaplus.site/swagger-ui/index.html)
- **Especificação OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

> 💡 **Dica de autenticação no Swagger**: Ao efetuar login no endpoint `/api/v1/auth/login`, copie o token gerado, clique no botão **Authorize 🔓** no topo da página e cole o token para testar os endpoints protegidos.

---

## 🌐 Principais Endpoints

| Método | Endpoint | Descrição | Autenticação |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/users` | Cadastro de novos usuários | Pública |
| `POST` | `/api/v1/auth/login` | Login com email/senha e emissão do JWT | Pública |
| `POST` | `/api/v1/rachas` | Criação de um novo Racha (vincula organizador) | `Bearer JWT` |
| `GET` | `/api/v1/rachas` | Lista os rachas dos quais o usuário autenticado faz parte | `Bearer JWT` |
| `GET` | `/api/v1/rachas/{id}/members` | Lista todos os membros de um racha específico | `Bearer JWT` |

---

## 🏗️ Estrutura do Projeto

O projeto adota uma arquitetura em camadas no pacote `br.com.rachaplus.api`:

```text
rachaplus-api/src/
├── main/
│   ├── java/br/com/rachaplus/api/
│   │   ├── application/
│   │   │   ├── dto/             # Records de entrada/saída (Requests e Responses)
│   │   │   └── service/         # Regras de negócio da aplicação
│   │   ├── domain/
│   │   │   ├── exception/       # Exceções de domínio (ex: ConflitoRegistroException)
│   │   │   ├── repository/      # Interfaces Spring Data JPA
│   │   │   └── *.java           # Entidades JPA (Usuario, Racha, RachaMember)
│   │   └── infrastructure/
│   │       ├── config/          # Configuração do Swagger/OpenAPI (OpenApiConfig)
│   │       ├── controller/      # Endpoints REST e GlobalExceptionHandler
│   │       └── security/        # Filtro JWT, PasswordEncoder e SecurityConfig
│   └── resources/
│       └── application.properties # Parâmetros do Spring Boot
└── test/
    └── java/br/com/rachaplus/api/
        └── infrastructure/controller/ # Testes unitários/MockMvc dos controllers
```

---

## 🔄 Pipeline de CI/CD

O repositório possui uma pipeline contínua configurada via **GitHub Actions** (`.github/workflows/ci-backend.yml`):
- É disparada a cada **push** ou **pull request** direcionado às branches `main` e `dev`.
- Sobe automaticamente um serviço de banco **PostgreSQL 16** temporário para testes.
- Configura o ambiente **Java 21** com cache das dependências do Maven.
- Executa compilação e todos os testes automatizados (`mvn clean verify`).
- Publica e armazena os relatórios de testes (`surefire-reports`) como artefatos do workflow.
