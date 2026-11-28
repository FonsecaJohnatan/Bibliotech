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

Para criar o primeiro administrador, copie `.env.example` para `.env` na raiz
do projeto e preencha `BIBLIOTECH_ADMIN_EMAIL` e `BIBLIOTECH_ADMIN_SENHA`.
O Spring carrega esse arquivo ao executar `mvn spring-boot:run` nessa pasta.
As variáveis de ambiente também podem ser usadas e têm prioridade sobre o arquivo.
O `.env` não é versionado; os testes não criam o administrador inicial.
A conta inicial só é criada quando ainda não há um administrador. A senha é armazenada
com BCrypt. Use o e-mail e a senha configurados em `/login`.

O catálogo em `/home` permite buscar por título, autor ou ISBN e filtrar por
categoria e disponibilidade. Ao acessar `/` ou `/home` sem sessão, o controller
redireciona para `/login`. Após entrar, o usuário acessa o catálogo.
As demais rotas MVC, REST e Swagger continuam sem filtro global de acesso.
O menu mostra Entrar quando não há sessão. Os links variam conforme o perfil;
isso organiza a navegação, mas não bloqueia acesso direto às rotas.

O link Criar conta no login abre `/cadastro` para cadastrar um leitor com nome,
e-mail e senha. O login verifica o hash BCrypt e guarda na sessão apenas ID, nome
e perfil. O logout encerra a sessão. Meus empréstimos usa essa sessão para buscar
os registros do próprio usuário e pede login quando não há sessão.

Não há Spring Security, HTTP Basic ou tokens CSRF. A biblioteca
`spring-security-crypto` é usada somente para BCrypt. A sessão usa cookie;
seu identificador não é incluído na URL.

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
Após salvar ou editar, cada formulário redireciona para a lista do próprio recurso.
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
são `ADMIN`, `BIBLIOTECARIO` e `LEITOR`. O campo `ativo` permite criar uma
conta inativa; quando omitido, vale `true`. O service normaliza o e-mail, impede
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
para maiúsculas. No MVC, é possível definir `DISPONIVEL` ou `INDISPONIVEL` e
filtrar por status. `EMPRESTADO` é controlado pelo empréstimo e pela devolução.
Na API, o exemplar começa como `DISPONIVEL` e a edição preserva o status.
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

O usuário deve estar ativo, sem empréstimos atrasados e com menos de três
empréstimos em aberto; o exemplar deve estar disponível. O formulário permite
buscar usuário por nome/e-mail e exemplar por código/título. As verificações
aparecem antes da confirmação e são repetidas no servidor.
O cadastro usa a data atual e o prazo de 14 dias definido no modelo.
A renovação acrescenta sete dias e pode ocorrer uma vez, enquanto o empréstimo
estiver em dia e ainda não tiver sido devolvido. Em `/meus-emprestimos`, cada
usuário pode renovar somente os próprios empréstimos.
A listagem de gestão filtra por `situacao=ABERTOS`, `ATRASADOS` ou `DEVOLVIDOS`.
As rotas de renovação e devolução não precisam de corpo na requisição.

A devolução usa a data atual, calcula a multa de R$ 1,00 por dia de atraso e
libera o exemplar. A janela de confirmação mostra dias de atraso e multa antes
de registrar a devolução. O histórico é mantido.
O total mostrado em Meus empréstimos soma apenas a multa dos empréstimos ainda
em atraso. A devolução retira esse valor do total; não há registro de pagamento.
A API retorna datas, multa, usuário e exemplar, sem senha ou relacionamentos recursivos.
Violações das regras retornam HTTP 409; referências inexistentes retornam HTTP 404.
No MVC, os erros são exibidos no formulário ou na listagem.

## Swagger

Com a aplicação iniciada, abra <http://localhost:8080/swagger-ui.html> para
consultar e testar a API REST sem login.
A especificação OpenAPI está disponível em
<http://localhost:8080/v3/api-docs>.

A documentação inclui somente as rotas `/service/**`, usando
`springdoc-openapi-starter-webmvc-ui` 2.6.0, compatível com o Spring Boot 3.3.
As listagens REST retornam `404` quando não há registros, conforme o padrão
adotado nos slides da disciplina.

## Fluxo de trabalho em equipe

- Uma branch por tarefa (`feature/nome-da-tarefa`), integrada à `main` por pull request.
- Mensagens de commit curtas e no imperativo (ex.: `Adiciona entidade Emprestimo`).
