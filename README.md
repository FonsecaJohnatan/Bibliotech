# BiblioTech

Sistema de Gerenciamento de Biblioteca Comunitária — MVC com Thymeleaf e API REST em Java com Spring Boot e Spring Data JPA.

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
    │   │   ├── api/           endpoints REST e tratamento de erros JSON
    │   │   ├── controller/    controllers das páginas MVC
    │   │   ├── dto/           dados de entrada e saída
    │   │   ├── model/         entidades JPA e enums
    │   │   ├── repository/    repositórios Spring Data JPA
    │   │   └── service/       regras de negócio compartilhadas
    │   └── resources/
    │       ├── templates/    base.html, home.html, erro.html e pastas de cada entidade
    │       ├── static/css/   estilos das páginas
    │       ├── messages.properties
    │       └── application.properties
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

Para criar o primeiro administrador, defina as variáveis de ambiente
`BIBLIOTECH_ADMIN_EMAIL` e `BIBLIOTECH_ADMIN_SENHA` antes de iniciar a aplicação.
A conta inicial só é criada quando ainda não há um administrador. A senha é armazenada
com BCrypt. Use o e-mail e a senha configurados em `/login`.

O catálogo (`/home`, `/livro` e `/livro/{id}`) é público. Usuários autenticados
podem consultar `/meus-emprestimos`. As telas de cadastro e empréstimos exigem
perfil `ADMIN` ou `BIBLIOTECARIO`. Na API, leitura pública se limita aos
recursos não sensíveis; alterações, usuários e empréstimos exigem esses perfis.
Clientes da API podem autenticar com HTTP Basic. Os formulários MVC usam
proteção CSRF.

Para usar PostgreSQL, ajuste as propriedades indicadas em `src/main/resources/application.properties`.

## Páginas e API

Abra <http://localhost:8080/home> para acessar as telas.

- MVC: `GET /autor` e `GET /categoria` listam os registros.
- Cadastro: `GET /autor/formulario` e `GET /categoria/formulario`.
- Os formulários enviam `POST /autor/novo` ou `POST /categoria/novo`.
- Edição: `GET /autor/{id}/editar` e `GET /categoria/{id}/editar`.
- A edição envia `POST /autor/{id}` ou `POST /categoria/{id}`.
- REST: `/service/autores` e `/service/categorias` recebem e retornam JSON.
  Use GET/POST na coleção e GET/PUT/DELETE em `/{id}`.

A separação de pacotes segue o CatRH: `api` para REST, `controller`
para MVC e `templates/autor` e `templates/categoria` para HTML.
O prefixo REST `/service` e as rotas MVC no singular seguem o exemplo.
As duas interfaces usam os
mesmos services, repositories e DTOs de requisição.

Os formulários exibem erros de validação e de categoria duplicada.
Após salvar, redirecionam para `/home`, que apresenta o catálogo de livros.
As demais listas ficam nas páginas de cada recurso, acessíveis pelo menu conforme o perfil.

### Livros

- MVC: `GET /livro`, `GET /livro/formulario`, `POST /livro/novo`,
  `GET /livro/{id}/editar` e `POST /livro/{id}`.
- REST: `GET/POST /service/livros` e `GET/PUT/DELETE /service/livros/{id}`.
- DTOs: `RequisicaoNovoLivro` para os dados de entrada e `LivroResposta`
  para a resposta JSON, incluindo categoria e autores sem relacionamentos recursivos.

Exemplo de JSON para cadastrar ou atualizar:

```json
{
  "titulo": "Dom Casmurro",
  "isbn": "9780110002224",
  "anoPublicacao": 1899,
  "categoriaId": 1,
  "autoresIds": [1, 2]
}
```

Use os IDs de categorias e autores já cadastrados. Título, ISBN e categoria
são obrigatórios; ano e autores são opcionais. A lista de autores enviada
substitui a anterior na atualização; envie `[]` para remover os vínculos.
O service valida o ISBN-13 e sua unicidade, aceitando espaços e hífens.
No MVC, as opções e os dados preenchidos são reapresentados quando há erros.

### Usuários

- MVC: `GET /usuario`, `GET /usuario/formulario`, `POST /usuario/novo`,
  `GET /usuario/{id}/editar`, `POST /usuario/{id}` e `POST /usuario/{id}/desativar`.
- REST: `GET/POST /service/usuarios` e `GET/PUT/DELETE /service/usuarios/{id}`.
- DTOs: `RequisicaoNovoUsuario` para cadastro, `RequisicaoAtualizarUsuario`
  para edição e `UsuarioResposta` para a resposta JSON.

Exemplo de JSON para cadastrar:

```json
{
  "nome": "Maria",
  "email": "maria@email.com",
  "senha": "minhaSenha",
  "perfil": "LEITOR"
}
```

Nome, e-mail, senha e perfil são obrigatórios no cadastro. Os perfis disponíveis
são `ADMIN`, `BIBLIOTECARIO` e `LEITOR`. O service normaliza o e-mail, impede
duplicidade e criptografa a senha com BCrypt.

Na edição, envie somente `nome` e `email`. A senha, o perfil e a situação
são preservados, conforme a regra existente no `UsuarioService`.
O `DELETE` da API e o botão Desativar do MVC marcam o usuário como inativo,
mantendo o registro e seu histórico de empréstimos.
A resposta contém `id`, `nome`, `email`, `perfil` e `ativo`, sem senha ou empréstimos.

### Exemplares

- MVC: `GET /exemplar`, `GET /exemplar/formulario`, `POST /exemplar/novo`,
  `GET /exemplar/{id}/editar`, `POST /exemplar/{id}` e `POST /exemplar/{id}/excluir`.
- REST: `GET/POST /service/exemplares` e `GET/PUT/DELETE /service/exemplares/{id}`.
- Use `?livroId=1` nas listagens MVC e REST para consultar os exemplares de um livro.
- DTOs: `RequisicaoNovoExemplar` e `ExemplarResposta`.

Exemplo de JSON para cadastrar ou atualizar:

```json
{
  "codigoTombo": "TOMBO-1",
  "livroId": 1
}
```

O código de tombo é obrigatório, único e limitado a 20 caracteres. O livro deve
estar cadastrado. O service remove espaços nas extremidades e converte o código
para maiúsculas. O exemplar começa como `DISPONIVEL`; a edição preserva seu status.
Exemplares com histórico não podem mudar de livro e exemplares emprestados ou
com histórico não podem ser excluídos. Esses conflitos retornam HTTP 409 na API
e uma mensagem na tela MVC. A resposta inclui os dados do livro, sem ciclos de
relacionamentos ou histórico de empréstimos.

### Empréstimos

- MVC: `GET /emprestimo`, `GET /emprestimo/formulario`, `POST /emprestimo/novo`,
  `POST /emprestimo/{id}/renovar`, `GET /emprestimo/{id}/devolucao` e
  `POST /emprestimo/{id}/devolver`.
- REST: `GET/POST /service/emprestimos`, `GET /service/emprestimos/{id}`,
  `PUT /service/emprestimos/{id}/renovar` e `PUT /service/emprestimos/{id}/devolver`.
- Use `?usuarioId=1` nas listagens MVC e REST para consultar o histórico de um usuário.
- DTOs: `RequisicaoNovoEmprestimo` e `EmprestimoResposta`.

Exemplo de JSON para registrar:

```json
{
  "usuarioId": 1,
  "exemplarId": 1
}
```

O usuário deve estar ativo, com menos de três empréstimos em aberto, e o exemplar
deve estar disponível. O formulário apresenta usuários ativos e exemplares disponíveis.
O cadastro usa a data atual e o prazo de 14 dias definido no modelo.
A renovação acrescenta sete dias e pode ocorrer uma vez, antes da devolução.
As rotas de renovação e devolução não precisam de corpo na requisição.

A devolução usa a data atual, calcula a multa de R$ 1,00 por dia de atraso e
libera o exemplar. O histórico é mantido. Na tela, a multa aparece após a devolução.
A API retorna datas, multa, usuário e exemplar, sem senha ou relacionamentos recursivos.
Violações das regras retornam HTTP 409; referências inexistentes retornam HTTP 404.
No MVC, os erros são exibidos no formulário ou na listagem.

## Swagger

Com a aplicação iniciada, abra <http://localhost:8080/swagger-ui.html> para
consultar e testar a API REST. A especificação OpenAPI está disponível em
<http://localhost:8080/v3/api-docs>.

A documentação inclui somente as rotas `/service/**`, usando
`springdoc-openapi-starter-webmvc-ui` 2.6.0, compatível com o Spring Boot 3.3.
As listagens REST retornam `404` quando não há registros, conforme o padrão
adotado nos slides da disciplina.

## Fluxo de trabalho em equipe

- Uma branch por tarefa (`feature/nome-da-tarefa`), integrada à `main` por pull request.
- Mensagens de commit curtas e no imperativo (ex.: `Adiciona entidade Emprestimo`).
