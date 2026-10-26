# BiblioTech

Sistema de Gerenciamento de Biblioteca Comunitária — API REST em Java com Spring Boot e Spring Data JPA.

**Etapa 1:** documentação de concepção e requisitos, diagrama de classes UML e camada de persistência (JPA).

## Integrantes

- Iago Henrique Pinto Bogler
- Guilherme Franciel Meiring
- Johnatan Vargas da Fonseca

## Tecnologias

- Java 17+
- Spring Boot 3.3 (Web, Data JPA, Validation)
- Hibernate / JPA
- H2 (desenvolvimento e testes) e PostgreSQL (produção)
- Lombok
- JUnit 5 + AssertJ
- Maven

## Estrutura

```
bibliotech
├── docs/                      documentação da Etapa 1 (adicionar o PDF/Markdown exportado)
├── pom.xml
└── src
    ├── main
    │   ├── java/br/edu/bibliotech
    │   │   ├── BibliotechApplication.java
    │   │   ├── model/         entidades JPA e enums
    │   │   └── repository/    repositórios Spring Data JPA
    │   └── resources/application.properties
    └── test/java/br/edu/bibliotech/repository/PersistenciaTest.java
```

## Entidades (6)

| Entidade     | Relacionamentos                                              |
|--------------|--------------------------------------------------------------|
| `Categoria`  | 1:N com `Livro`                                              |
| `Autor`      | N:M com `Livro` (tabela `livro_autor`)                       |
| `Livro`      | N:1 com `Categoria`, N:M com `Autor`, 1:N com `Exemplar`     |
| `Exemplar`   | N:1 com `Livro`, 1:N com `Emprestimo`                        |
| `Usuario`    | 1:N com `Emprestimo`                                         |
| `Emprestimo` | N:1 com `Exemplar`, N:1 com `Usuario`                        |

Enums: `Perfil` (ADMIN, BIBLIOTECARIO, LEITOR) e `StatusExemplar` (DISPONIVEL, EMPRESTADO, INDISPONIVEL).

## Como executar

```bash
# rodar os testes de persistência
mvn test

# subir a aplicação
mvn spring-boot:run
```

Com a aplicação no ar, o console do H2 fica em <http://localhost:8080/h2-console>
(JDBC URL: `jdbc:h2:mem:bibliotech`, usuário `sa`, senha em branco).

Para usar PostgreSQL, ajuste as propriedades indicadas em `src/main/resources/application.properties`.

## Fluxo de trabalho em equipe

- Uma branch por tarefa (`feature/nome-da-tarefa`), integrada à `main` por pull request.
- Mensagens de commit curtas e no imperativo (ex.: `Adiciona entidade Emprestimo`).
