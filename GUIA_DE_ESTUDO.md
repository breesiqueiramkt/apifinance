# Guia de Estudo — Extrato (App de Gestão Financeira Pessoal)

Este guia explica **o que** cada módulo faz, **onde** o código está, e —
mais importante para estudo — **por que** ele foi construído daquele jeito.
Não é um manual de usuário; é uma leitura de código guiada.

Recomendo ler na ordem: primeiro a arquitetura geral, depois o banco de
dados, depois autenticação, e só então os módulos de negócio um por um.
Cada módulo segue sempre o mesmo caminho: **Entity → Repository → DTO →
Service → Controller → página React** — uma vez que esse padrão fizer
sentido no módulo de Contas, os outros nove ficam muito mais fáceis de ler.

---

## 1. Arquitetura geral

### 1.1 As cinco camadas do backend

```
Requisição HTTP
      │
      ▼
Controller   → só traduz HTTP ⇄ Java. Não tem regra de negócio.
      │
      ▼
Service      → TODA a regra de negócio e os cálculos vivem aqui.
      │
      ▼
Repository   → só sabe conversar com o banco (Spring Data JPA gera o SQL).
      │
      ▼
Entity       → representa uma tabela do banco como uma classe Java.
```

Um **DTO** (Data Transfer Object) não é uma camada, é um "envelope": os
dados que entram e saem pela API nunca são as Entities diretamente. Isso
importa por dois motivos práticos:

1. **Segurança** — a Entity `User` tem um campo `passwordHash`. Se a API
   devolvesse a Entity direto, o hash da senha vazaria em toda resposta.
   O DTO `AuthResponse` simplesmente não tem esse campo.
2. **Estabilidade** — você pode mudar o nome de uma coluna no banco sem
   quebrar o contrato da API, porque o Service é quem faz a "tradução"
   entre Entity e DTO.

Exemplo concreto: veja `dto/AccountDtos.java`. `AccountRequest` (o que a
API recebe) e `AccountResponse` (o que a API devolve) são **records** do
Java — uma classe imutável, sem boilerplate de getters/setters, ideal para
"pacotes de dados" que não têm comportamento.

### 1.2 Por que Service nunca fala direto com o Controller de outro módulo

Repare em `BillService`: ele injeta `TransactionService` e chama
`transactionService.create(...)` para marcar uma conta como paga. Ele
**não** duplica a lógica de "criar lançamento e atualizar saldo" — reusa o
Service que já faz isso. Essa é a regra de ouro: regra de negócio mora em
exatamente um lugar. Se amanhã a forma de atualizar saldo mudar, muda em
um arquivo só, e todo mundo que reusa `TransactionService` ganha a correção
de graça.

### 1.3 Por que Java *records* para DTOs

```java
public record AccountRequest(
        @NotBlank String name,
        @NotNull AccountType type,
        @NotNull BigDecimal balance
) {}
```

Antes do Java 16, isso exigiria uma classe com campos privados, construtor,
getters, `equals`/`hashCode`/`toString` — uns 40 linhas de boilerplate. Um
`record` gera tudo isso automaticamente a partir da assinatura. Para um DTO
(que é só dado, sem comportamento), é a ferramenta certa.

### 1.4 Arquitetura do frontend

```
main.jsx          → monta React, Router e AuthProvider
  App.jsx         → define as rotas (uma por módulo)
    Layout.jsx    → sidebar + topbar, envolve toda página autenticada
      <Página>    → busca dados da API, mostra Cards/Modals/Tabelas
```

Cada página segue o mesmo esqueleto (veja `pages/Accounts.jsx` como
referência): `useState` para a lista e para o formulário → `useEffect`
busca os dados na API quando a página monta → funções `handleSubmit` /
`handleDelete` chamam a API e recarregam a lista. É o mesmo padrão repetido
12 vezes; aprender um, você lê os outros onze de relance.

---

## 2. Banco de dados

Arquivo: `backend/src/main/resources/db/migration/V1__init_schema.sql`.

O projeto usa **Flyway**: em vez de deixar o Hibernate criar/alterar tabelas
sozinho (`ddl-auto: update`, comum em tutoriais mas perigoso em produção),
toda mudança de schema é um arquivo SQL versionado. `V1__init_schema.sql`
roda uma vez, na ordem, e o Flyway guarda em uma tabela própria
(`flyway_schema_history`) quais migrations já rodaram. Isso é o que
qualquer time profissional usa: histórico auditável do banco, igual ao
Git para código.

### 2.1 Mapa das 13 tabelas

```
users ──┬── accounts ──────┬── transactions ── categories
        │                  │        (receitas e despesas
        │                  │         na mesma tabela, ver §4.3)
        ├── bills ─────────┘
        ├── credit_cards ── credit_card_transactions
        ├── investments ── investment_types
        ├── goals
        ├── debts
        └── financial_settings
```

Toda tabela de dado do usuário tem uma coluna `user_id` com
`ON DELETE CASCADE` — apagar o usuário limpa tudo que é dele. Isso é o que
implementa, no nível do banco, a regra "cada usuário só vê seus próprios
dados" (a outra metade dessa regra está no Service, que sempre filtra por
`userId` — ver §3.3).

### 2.2 Por que uma tabela `transactions` e não `income` + `expenses`

O briefing original sugeria três tabelas. O código usa uma só
(`Transaction`, com um campo `type` que vale `INCOME` ou `EXPENSE`).
Motivo: receita e despesa têm **exatamente os mesmos campos** (descrição,
valor, data, conta, categoria, status, recorrência). Duas tabelas idênticas
significaria duplicar toda regra de negócio (validação, atualização de
saldo, filtros) duas vezes. Uma tabela com discriminador é o padrão
"Single Table" e é a escolha certa quando as variações têm o mesmo formato
e comportamento parecido — o comentário no topo de `entity/Transaction.java`
documenta essa decisão.

---

## 3. Autenticação e segurança (JWT)

### 3.1 O fluxo completo, passo a passo

1. Usuário envia `POST /api/auth/register` com nome, e-mail, senha.
2. `AuthService.register()` chama `passwordEncoder.encode(senha)` — a senha
   em texto puro **nunca** é salva; só o hash BCrypt vai para o banco
   (coluna `password_hash`). BCrypt é de propósito lento (ajustável por um
   "custo"), o que torna ataques de força bruta caros mesmo se o banco
   vazar.
3. O Service gera um **JWT** (`JwtService.generateToken`): um token
   assinado contendo o e-mail do usuário e seu ID, válido por 24h (config
   `app.jwt.expiration-ms`).
4. O frontend guarda esse token em `localStorage` (`api/client.js`) e o
   envia em todo request futuro no header `Authorization: Bearer <token>`.
5. A cada request, `JwtAuthenticationFilter` roda **antes** de qualquer
   Controller: lê o header, valida a assinatura do token, e se for válido,
   registra o usuário como "autenticado" no `SecurityContext` do Spring.
6. `SecurityConfig` diz quais rotas são públicas (`/api/auth/**`,
   `/docs/**`) e quais exigem esse contexto autenticado (todo o resto —
   `anyRequest().authenticated()`).
7. Dentro de um Controller, `@AuthenticationPrincipal UserPrincipal user`
   injeta automaticamente o usuário logado — é assim que
   `AccountController.list(user)` sabe de quem são as contas, sem o
   frontend precisar mandar um `userId` (que seria inseguro: qualquer um
   poderia forjar o ID de outra pessoa).

### 3.2 Por que JWT é *stateless*

`SecurityConfig` configura
`sessionCreationPolicy(SessionCreationPolicy.STATELESS)`. Isso significa: o
servidor não guarda "quem está logado" em memória. Toda informação
necessária para autenticar já está dentro do próprio token, assinado
criptograficamente. Vantagem prática: o backend pode ter 10 instâncias
rodando atrás de um load balancer sem precisar compartilhar sessão entre
elas — qualquer instância consegue validar qualquer token sozinha.

### 3.3 A segunda camada de segurança: filtro por dono

JWT resolve "quem é você". Não resolve "você pode ver/editar isso". Repare
que **todo** método de Service que busca um registro específico faz assim:

```java
accountRepository.findByIdAndUserId(accountId, userId)
        .orElseThrow(() -> new ResourceNotFoundException(...));
```

Nunca `findById(accountId)` sozinho. Se o usuário 2 tentar editar a conta
do usuário 1 (adivinhando o ID na URL), a query simplesmente não encontra
nada — devolve 404, não 403, para nem revelar que o registro existe. Essa
checagem se repete em Account, Category, Transaction, Bill, CreditCard,
Investment, Goal e Debt. É repetitivo de propósito: segurança por dado é
mais fácil de auditar quando está explícita em cada método do que quando
está "escondida" em alguma camada genérica.

---

## 4. Módulo por módulo

### 4.1 Contas Bancárias (`Account`)

**Arquivos:** `entity/Account.java`, `service/AccountService.java`,
`controller/AccountController.java`, `pages/Accounts.jsx`.

O conceito mais simples do sistema: CRUD puro. A única regra de negócio é
que o **saldo não é editado diretamente pelo usuário no dia a dia** — ele
começa com um valor inicial e depois só muda através de lançamentos (ver
§4.3). Editar a conta permite corrigir o saldo manualmente (por exemplo, ao
cadastrar uma conta que já tem histórico), mas isso é uma correção pontual,
não o fluxo normal.

### 4.2 Categorias (`Category`)

**Arquivos:** `entity/Category.java`, `service/CategoryService.java`.

Ideia central: `user_id` **nulo** significa "categoria padrão do sistema",
visível para todo mundo; `user_id` preenchido significa "categoria
personalizada" daquele usuário. A query que busca categorias
(`findVisibleToUser` em `CategoryRepository`) reflete isso:

```java
WHERE c.user IS NULL OR c.user.id = :userId
```

As 20 categorias padrão (13 despesas + 7 receitas, exatamente as listadas
no briefing) são inseridas uma vez por `ensureDefaultCategoriesExist()`,
chamado a cada registro de usuário — mas a checagem
`existsByNameIgnoreCaseAndUserIsNull` garante que rodar isso mil vezes não
crie duplicatas (**idempotência**: rodar uma operação várias vezes tem o
mesmo efeito que rodar uma vez).

### 4.3 Receitas e Despesas (`Transaction`) — o coração do sistema

**Arquivos:** `entity/Transaction.java`, `service/TransactionService.java`.

Este é o módulo mais importante para entender bem, porque quase todo outro
módulo (Dashboard, Relatórios, Análise Automática) lê dados daqui.

**A regra de saldo automático** (seção 5 do briefing original): o saldo de
uma conta só muda quando um lançamento tem status `PAID`. Um lançamento
`PENDING`/`SCHEDULED`/`LATE` aparece nas telas, mas não mexe no dinheiro,
porque ele representa algo que *vai* acontecer, não algo que já aconteceu.
Isso é modelado em três métodos espelhados em `TransactionService`:

```java
applyToBalance()     // soma se for INCOME, subtrai se for EXPENSE
reverseFromBalance()  // faz exatamente o oposto
```

`update()` sempre **reverte o efeito antigo antes de aplicar o novo** —
mesmo que só a descrição tenha mudado. Por quê? Porque senão, editar uma
despesa de R$100 para R$150 aplicaria só a diferença errada, ou pior,
mudar a *conta* de um lançamento deixaria o saldo desincronizado entre a
conta antiga e a nova. "Desfazer tudo, refazer do zero" é mais simples de
provar correto do que tentar calcular deltas — e é mais fácil de testar
(veja `TransactionServiceTest`).

**Exercício sugerido:** leia `TransactionServiceTest.java` linha por linha
e tente prever o saldo final *antes* de olhar o `assertThat`. Se acertar
todos os 4 testes de cabeça, você entendeu o módulo.

### 4.4 Dashboard

**Arquivos:** `service/DashboardService.java`, `pages/Dashboard.jsx`.

O Dashboard não guarda dado nenhum — ele é 100% cálculo em cima do que já
existe em Account, Transaction e Investment. Os números, um por um:

| Campo | Fórmula | Por quê |
|---|---|---|
| `netWorth` (patrimônio) | soma das contas + soma dos investimentos | "quanto eu tenho no total" |
| `available` (disponível) | só soma das contas | "quanto eu posso gastar agora" (investimento não é líquido) |
| `previousMonthNetWorth` | `netWorth - receita do mês + despesa do mês` | reconstrói o patrimônio de 1 mês atrás **sem precisar de uma tabela de histórico** — ver limitação documentada no código |
| `projectedBalance` | `disponível + receita pendente do mês - despesa pendente do mês` | "quanto vou ter no fim do mês se tudo que está agendado se confirmar" |
| `savingsRate` | `(receita - despesa) / receita × 100` | indicador clássico de educação financeira |
| `expenseCommitment` | `despesa / receita × 100` | o inverso complementar da taxa de poupança |

Todo cálculo de percentual passa por um método `percentage()` que devolve
zero quando a receita é zero — sem essa guarda, o sistema quebraria com uma
`ArithmeticException: / by zero` assim que um usuário novo, sem nenhuma
receita cadastrada, abrisse o dashboard pela primeira vez. É um detalhe
pequeno, mas é o tipo de "caso de borda" que separa código que funciona só
na demonstração de código que funciona na vida real.

### 4.5 Contas Futuras / Boletos (`Bill`)

**Arquivos:** `entity/Bill.java`, `service/BillService.java`, `pages/Bills.jsx`.

O detalhe mais interessante aqui é `markAsPaid()`. Em vez de só mudar um
campo `status` para `PAID` (o que deixaria o boleto "pago" mas sem nenhum
reflexo no saldo da conta), o método **cria um lançamento de verdade**
chamando `transactionService.create(...)` com status `PAID`. Isso significa
que pagar uma conta futura automaticamente:

1. Aparece no extrato de transações.
2. Diminui o saldo da conta escolhida.
3. Entra nos relatórios e na análise automática do mês.

Sem essa integração, "Contas Futuras" seria uma lista solta, desconectada
do resto do sistema — um checklist, não um módulo financeiro de verdade.

### 4.6 Cartão de Crédito

**Arquivos:** `entity/CreditCard.java`, `entity/CreditCardTransaction.java`,
`service/CreditCardService.java`, `pages/CreditCards.jsx`.

Este é o módulo com a lógica de datas mais elaborada do projeto. Dois
conceitos separados:

**(a) Parcelamento.** Uma compra de R$300 em 3x vira **três linhas** na
tabela `credit_card_transactions`, cada uma com `purchase_date` projetada
um mês à frente da anterior (`purchaseDate.plusMonths(i - 1)`). Assim,
"quanto vou pagar em maio" é sempre só um filtro de data — nunca é preciso
"lembrar" que uma parcela pertence a uma compra maior.

Reparem no arredondamento em `addPurchase()`: R$100 em 3 parcelas dá
33,333... Se cada parcela virasse R$33,33, a soma seria R$99,99 — um
centavo desaparecendo. A correção:

```java
BigDecimal remainder = valorTotal - (parcelaBase × N);
// a sobra vai inteira pra última parcela
```

`CreditCardServiceTest.splitsPurchaseIntoInstallmentsThatSumExactlyToTheOriginalAmount`
existe exatamente para travar essa regra — é o tipo de bug que passa
despercebido em teste manual (ninguém confere centavo por centavo na tela)
mas quebra a confiança do usuário no sistema.

**(b) Ciclo de fatura.** Dado o "dia de fechamento" do cartão, o método
`currentCycle()` calcula qual intervalo de datas é a fatura que está aberta
agora, e `nextCycle()` calcula a próxima. A regra: se hoje já passou do dia
de fechamento deste mês, a fatura "atual" (ainda aberta) é a que fecha
*mês que vem*. `limitUsed` soma todas as parcelas com data a partir do
início do ciclo atual — ou seja, tudo que ainda vai ser cobrado, presente
ou futuro, conta como limite comprometido.

### 4.7 Investimentos

**Arquivos:** `entity/Investment.java`, `entity/InvestmentType.java`,
`service/InvestmentService.java`, `pages/Investments.jsx`.

Estrutura simples (o usuário informa quanto investiu e quanto tem hoje;
o sistema calcula a diferença), mas dois pontos valem atenção:

- **Seed de tipos.** Igual às categorias, `ensureDefaultTypesExist()` é
  idempotente e roda sob demanda (na primeira chamada a `listTypes()`),
  não precisa de um script de setup separado.
- **Renda estimada.** `summary()` calcula quanto o portfólio *deveria*
  render por mês, somando `valorAtual × taxaEsperada / 12` de cada
  investimento que tem uma taxa informada. É só uma estimativa — por isso
  toda tela que mostra esse número exibe também o aviso "Rentabilidade
  estimada. Os valores reais podem variar." (seção 10 do briefing exige
  isso explicitamente, para nunca parecer uma promessa de retorno).

### 4.8 Metas Financeiras (`Goal`)

**Arquivos:** `entity/Goal.java`, `service/GoalService.java`, `pages/Goals.jsx`.

A conta que a seção 11 do briefing pede — "quanto preciso guardar por mês
para bater a meta" — está em `toResponse()`:

```java
mesesRestantes = mesesEntre(hoje, prazo)
aporteMensal   = (valorObjetivo - valorAtual) / mesesRestantes
```

Dois detalhes de robustez: o progresso nunca passa de 100% mesmo que o
usuário tenha guardado mais do que precisava (`.min(BigDecimal.valueOf(100))`
em `GoalService`), e se a meta não tem prazo definido, `monthsRemaining` e
`monthlyContributionNeeded` voltam `null` em vez de gerar uma divisão por
zero.

### 4.9 Dívidas (`Debt`)

**Arquivos:** `entity/Debt.java`, `service/DebtService.java`, `pages/Debts.jsx`.

Duas ideias emprestadas de educação financeira real:

**Método avalanche.** A lista de dívidas vem sempre ordenada por taxa de
juros decrescente (`DebtRepository`, com `NULLS LAST` explícito para dívida
sem taxa informada não furar a fila por acidente). Matematicamente, quitar
primeiro a dívida mais cara é a estratégia que minimiza o total de juros
pagos — é a alternativa "racional" ao método bola-de-neve (que prioriza a
menor dívida por motivação psicológica).

**Tabela Price reaproveitada.** `DebtService` usa a mesma função
`FinanceMath.pricePayment()` que a calculadora de financiamento usa, para
estimar qual seria a parcela e quanto de juros ainda falta pagar dada a
taxa e o número de parcelas restantes. Isso é o benefício prático de ter
extraído a matemática financeira para uma classe utilitária única (§4.11) —
dois módulos completamente diferentes reusam a mesma fórmula testada uma
vez só.

### 4.10 Calculadoras Financeiras

**Arquivos:** `util/FinanceMath.java`, `service/CalculatorService.java`,
`pages/Calculators.jsx`.

Todas as 6 calculadoras do briefing (juros compostos, independência
financeira, reserva de emergência, inflação, financiamento, aposentadoria)
são **stateless** — não leem nem gravam nada no banco, só recebem números e
devolvem números. Por isso não checam dono nem exigem usuário logado
consistente (só passam por autenticação porque são rotas atrás do filtro
JWT, mas nenhuma tem `WHERE user_id = ...` porque não haveria o que
filtrar).

Duas fórmulas merecem destaque:

**Conversão de taxa anual para mensal.** `annualToMonthlyRate()` usa
`(1 + taxaAno)^(1/12) - 1`, não `taxaAno / 12`. A diferença importa: taxas
financeiras "compõem" (juros sobre juros), então dividir por 12
*subestima* sistematicamente o efeito real. `FinanceMathTest` prova isso
numericamente: 12,6825% ao ano equivale a exatamente 1% ao mês pela fórmula
composta.

**Tabela Price.** `pricePayment()` implementa
`PMT = PV × i / (1 - (1+i)^-n)` — a fórmula-padrão de financiamento com
parcelas fixas usada por praticamente todo financiamento de carro/imóvel no
Brasil. É a mesma função usada em Financiamento (calculadora) e em Dívidas
(estimativa de parcela restante).

**Regra dos 4%** (usada em Independência Financeira e Aposentadoria): a
ideia (popular em movimentos como FIRE — *Financial Independence, Retire
Early*) é que um patrimônio investido pode sustentar indefinidamente um
saque de ~4% ao ano sem se esgotar, historicamente. Por isso
`patrimônio necessário = despesa anual ÷ taxa esperada`: é a conta invertida
da mesma regra.

### 4.11 Relatórios

**Arquivos:** `service/ReportService.java`, `pages/Reports.jsx`.

Três relatórios, todos derivados de `Transaction` (nenhum dado novo é
armazenado):

- **Fluxo de caixa** — receita e despesa mês a mês, para o gráfico de
  barras.
- **Evolução patrimonial** — reconstrói o patrimônio de cada mês passado
  "andando para trás" a partir do valor de hoje, subtraindo o saldo líquido
  (`receita - despesa`) de cada mês. Isso só é possível porque, neste
  sistema, saldo de conta só muda por lançamento `PAID` (§4.3) — se um dia
  o app permitir edição manual de saldo sem lançamento, esse relatório
  ficaria impreciso, por isso o comentário no código avisa exatamente essa
  limitação.
- **Gastos por categoria** — mesma lógica de agrupamento usada no
  Dashboard, mas parametrizada por qualquer intervalo de datas.

### 4.12 Análise Financeira Automática

**Arquivos:** `service/InsightService.java`.

Este módulo não faz nenhum cálculo novo — ele **interpreta** os cálculos
que os outros módulos já fazem e transforma em frases. Cinco regras, cada
uma isolada em seu próprio método privado:

1. Compara gasto por categoria deste mês vs. mês passado; se alguma
   categoria subiu 10% ou mais, vira uma frase.
2. Soma despesas com recorrência diferente de `NONE` (interpretado como
   "gasto fixo") e calcula que fração da renda elas representam.
3. Reaproveita a taxa de poupança do dashboard.
4. Calcula que fração do patrimônio total está investida.
5. Simula 5 anos usando `FinanceMath.simulate()` com o aporte mensal atual
   (receita menos despesa deste mês) e a taxa configurada em
   `financial_settings` para "renda fixa".

Se nenhuma regra disparar (usuário muito novo no sistema, sem dado
suficiente), uma mensagem-padrão explica isso em vez de mostrar uma lista
vazia sem contexto — um detalhe pequeno de UX que evita o app parecer
quebrado.

### 4.13 Configurações de Rentabilidade

**Arquivos:** `entity/FinancialSettings.java`,
`service/FinancialSettingsService.java`.

Implementa a seção 10 do briefing: as taxas usadas nas calculadoras e na
projeção de investimentos **não são fixas no código** — ficam numa tabela,
com uma linha "global" (`user_id` nulo, criada sob demanda com valores
padrão realistas) e, opcionalmente, uma linha por usuário que sobrescreve o
que quiser. `updateRates()` mescla o que o usuário mandou com o que faltar
do default global, para nunca salvar um campo nulo por acidente.

---

## 5. Frontend em detalhe

### 5.1 Por que Context API e não Redux

`AuthContext.jsx` guarda só uma coisa global de verdade: quem está logado.
Isso é pouco estado compartilhado — exatamente o caso em que a Context API
nativa do React é suficiente, e trazer uma biblioteca de state management
(Redux, Zustand, etc.) seria complexidade desnecessária. Cada página busca
seus próprios dados com `useState` + `useEffect` local; não há necessidade
de um estado global de "lista de contas" ou "lista de transações"
compartilhado entre páginas.

### 5.2 O padrão de página (CRUD + Modal)

Toda página de listagem (`Accounts.jsx`, `Goals.jsx`, `Debts.jsx`...) segue
o mesmo esqueleto:

```jsx
const [items, setItems] = useState([])
const [modalOpen, setModalOpen] = useState(false)
const [form, setForm] = useState(EMPTY_FORM)

function loadItems() { api.get('/recurso').then(...) }
useEffect(loadItems, [])

async function handleSubmit(e) {
  e.preventDefault()
  await api.post('/recurso', form)   // ou put se estiver editando
  setModalOpen(false)
  loadItems()                          // recarrega em vez de atualizar local
}
```

Recarregar a lista inteira após um `POST`/`PUT`/`DELETE` (em vez de tentar
atualizar o estado local "na mão") é uma escolha deliberada de simplicidade:
evita bugs sutis de estado dessincronizado, ao custo de uma chamada de rede
extra que, para o volume de dados de um app pessoal, é imperceptível.

### 5.3 O cliente HTTP (`api/client.js`)

Um único `axios.create()` com dois interceptors:

- **Request:** injeta `Authorization: Bearer <token>` em toda chamada,
  automaticamente — nenhuma página precisa se preocupar com isso.
- **Response:** se qualquer chamada voltar 401 (token expirado/inválido),
  limpa o `localStorage` e redireciona para `/login`. Isso centraliza a
  lógica de "sessão expirou" em um lugar só, em vez de espalhar
  `try/catch` de 401 em cada página.

### 5.4 Identidade visual

O tema (`index.css` + `tailwind.config.js`) usa variáveis CSS
(`--bg`, `--primary`, etc.) que mudam de valor quando a classe `.dark` está
presente no `<html>` — é assim que o dark/light mode funciona sem duplicar
nenhum componente. A escolha de paleta (verde-esmeralda + papel, tipografia
`Space Grotesk`/`Manrope`/`JetBrains Mono`) evoca extrato bancário/ledger de
propósito: números financeiros usam fonte monoespaçada (`.money` em
`index.css`) para as casas decimais alinharem verticalmente como em um
extrato de verdade, e o selo de status (`StatusStamp.jsx`) imita um carimbo
de "PAGO" em boleto.

---

## 6. Testes: o que testamos e por quê

Arquivo por arquivo, em `backend/src/test/java`:

| Teste | O que garante |
|---|---|
| `AuthServiceTest` | senha nunca é salva em texto puro; e-mail duplicado é rejeitado |
| `TransactionServiceTest` | saldo sobe/desce corretamente; pendente não mexe no saldo; excluir reverte o efeito |
| `DashboardServiceTest` | taxa de poupança, comprometimento e saldo previsto batem com a conta manual; divisão por zero não quebra o app |
| `GoalServiceTest` | progresso e aporte mensal necessário calculam certo; progresso nunca passa de 100% |
| `CreditCardServiceTest` | parcelas somam exatamente o valor da compra (sem perder centavo); cada parcela cai um mês depois da anterior |
| `CalculatorServiceTest` | as 4 calculadoras com fórmula fechada batem com o resultado esperado |
| `FinanceMathTest` | a fórmula de juros compostos e a Tabela Price batem com os valores clássicos de referência |

**Padrão usado:** todos são testes unitários com **Mockito** — os
repositórios são simulados (`@Mock`), então o teste roda em milissegundos e
não precisa de um banco de verdade. O que se testa é *a lógica*, não a
integração com o Postgres. Isso é intencional: o objetivo aqui é travar os
**cálculos financeiros** (que são o risco real de bug silencioso — ninguém
percebe um erro de centavos numa tela), não testar se o Spring Data sabe
gerar SQL (isso é responsabilidade do framework, já testado por quem o
mantém).

---

## 7. Limitações conhecidas (e por que estão documentadas, não escondidas)

Todo sistema real tem simplificações. Documentá-las é parte do trabalho —
esconder é o que gera surpresa desagradável depois:

1. **Sem histórico de patrimônio.** `previousMonthNetWorth` e o relatório de
   evolução patrimonial são *reconstruídos* a partir dos lançamentos do
   período, não lidos de um valor gravado no passado. Funciona bem enquanto
   a única forma de o saldo mudar for por lançamento `PAID` — quebraria se
   o usuário editasse o saldo de uma conta manualmente no meio do mês.
   Extensão natural: um job mensal que grava uma "foto" do patrimônio numa
   tabela `net_worth_history`.
2. **Investimentos sem histórico de valor.** Só o valor atual é guardado;
   não há como saber quanto um investimento valia há 3 meses, então ele
   fica de fora do gráfico de evolução patrimonial.
3. **Fatura de cartão simplificada.** A regra "parcela = compra + N meses"
   não trata perfeitamente compras feitas exatamente no dia de fechamento —
   sistemas bancários reais têm regras mais finas (geralmente definidas
   pela bandeira do cartão, não pelo banco).

---

## 8. Sugestão de exercício: adicionar um módulo novo

Uma boa forma de fixar o que este projeto ensina é implementar algo que
ainda não existe, seguindo o mesmo padrão. Candidato razoável: **contas
compartilhadas/familiares** (item citado na seção 29 do briefing original
como evolução futura). Passos, na ordem que o próprio projeto segue:

1. Adicionar tabela(s) na migration `V2__...sql` (nunca edite `V1`; Flyway
   trata migrations como imutáveis depois de aplicadas).
2. Criar a Entity (`@Entity`, Lombok `@Builder`).
3. Criar o Repository (interface, Spring Data gera a implementação).
4. Criar os DTOs (`record` de request e de response).
5. Criar o Service com a regra de negócio (e o filtro por dono, sempre).
6. Criar o Controller (fino, só traduz HTTP).
7. Criar a página React seguindo o esqueleto do §5.2.
8. Escrever pelo menos um teste unitário para a regra de negócio mais
   arriscada do módulo novo.

Se esse fluxo fizer sentido de cabeça, o objetivo deste guia foi cumprido.
