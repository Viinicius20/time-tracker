# Time Tracker

Controle de tempo de uso de programas e jogos no Windows. Você define um limite diário por aplicativo; o app conta só o tempo em que ele está em **primeiro plano** e avisa com uma notificação do sistema ao passar do limite.

![Time Tracker](screenshots/timetracker-tela.png)

## Como funciona

- Detecta o aplicativo em primeiro plano pela API do Windows (JNA).
- Uma tarefa agendada, a cada segundo, soma o tempo ao app cujo executável está em foco.
- O uso zera todo dia (inclusive se o dia virar com o app aberto) e se mantém se o backend reiniciar no mesmo dia.
- A barra de cada app muda de cor conforme chega perto do limite (verde, âmbar, vermelho).
- Ao estourar o limite, mostra uma notificação do sistema (*"Tempo esgotado: você atingiu o limite diário de X"*) e repete o aviso a cada 5 minutos enquanto o app continuar em uso.
- API REST (CRUD de apps e limites), persistência com JPA/H2 e interface web simples servida pelo próprio Spring Boot.

## Estrutura

- `ForegroundWatcher`: a cada segundo identifica o app em foco, soma o tempo, faz o reset diário e dispara os avisos.
- `Notifier`: notificação pela bandeja do Windows.
- `TrackedAppController`: API REST para listar, cadastrar, mudar o limite e remover apps.

## API

| Método | Rota | O que faz |
|---|---|---|
| `GET` | `/api/apps` | Lista os apps com uso, limite e se está em foco agora |
| `POST` | `/api/apps` | Cadastra `{ "name", "exe", "limitSeconds" }` |
| `PUT` | `/api/apps/{id}/limit` | Muda o limite `{ "limitSeconds" }` |
| `DELETE` | `/api/apps/{id}` | Remove o app |

## Stack

Java, Spring Boot, JPA/Hibernate, H2, JNA, Maven. Testes com JUnit 5 e Mockito.

## Requisitos

Windows, JDK 21 ou superior e Maven (ou a IDE com Maven embutido, como o IntelliJ).

## Como rodar

```bash
mvn spring-boot:run
```

Depois é só abrir `http://localhost:8080`. Também dá pra abrir o projeto no IntelliJ e rodar a classe `TimeTrackerApplication`.

No meu uso pessoal, o backend sobe junto com o Windows e um atalho na área de trabalho abre a interface em janela própria.

## Testes

```bash
mvn test
```

Ou, no IntelliJ, botão direito em `src/test/java` e **Run 'All Tests'**.

- `ForegroundWatcherTest`: conta só o app em foco, reset diário, aviso ao estourar o limite e a cada 5 minutos.
- `TrackedAppControllerTest`: validação do cadastro e do limite, CRUD e marcação do app em foco.

A detecção da janela (JNA) e a notificação da bandeja dependem do Windows e não têm teste automatizado.

## Limitações

- Só funciona no Windows (usa a API nativa para achar a janela em foco).
- Conta o tempo enquanto o app está em foco, mesmo sem atividade (não detecta ociosidade).
- Projeto de uso pessoal, feito para rodar local.