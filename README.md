# Extrato — Gestão Financeira Pessoal

Aplicativo completo de controle financeiro pessoal: contas bancárias, receitas
e despesas, contas futuras, cartão de crédito, investimentos, metas, dívidas,
calculadoras financeiras, relatórios e análise automática — com autenticação
JWT, banco PostgreSQL e dashboard com indicadores calculados de verdade.

Este é o **produto completo** (não protótipo): backend Java/Spring Boot com
PostgreSQL e frontend React consumindo a API de verdade. Todos os módulos
descritos no briefing original estão implementados e funcionais.

> Para uma explicação módulo por módulo — o que cada peça faz e por quê —
> veja **[`GUIA_DE_ESTUDO.md`](./GUIA_DE_ESTUDO.md)**.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Frontend | React 18 + Vite + Tailwind CSS + React Router + Recharts + vite-plugin-pwa |
| Backend | Java 21 + Spring Boot 3.3 (Web, Security, Data JPA, Validation) |
| Banco | PostgreSQL + Flyway (migrations versionadas) |
| Autenticação | JWT (jjwt) + Spring Security + BCrypt |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5 + Mockito + AssertJ |

---

## Módulos incluídos

| Módulo | O que faz |
|---|---|
| Autenticação | Login com JWT, usuário único ("master") provisionado por variável de ambiente na subida — sem cadastro público, senha com hash BCrypt |
| Contas bancárias | Contas correntes/poupança/carteira/digital, saldo atualizado automaticamente |
| Categorias | Padrão do sistema + personalizadas, por receita/despesa |
| Receitas e despesas | Lançamentos com status, recorrência, forma de pagamento |
| Dashboard | Patrimônio, disponível, receitas/despesas do mês, saldo previsto, taxa de poupança |
| Contas futuras | Boletos/contas com vencimento, alertas por período, "pagar" gera lançamento real |
| Cartão de crédito | Fatura atual/próxima, parcelamento, limite usado/disponível |
| Investimentos | Renda fixa/variável/outros, rentabilidade, renda estimada |
| Metas financeiras | Progresso, aporte mensal necessário |
| Dívidas | Priorização por método avalanche, parcela e juros estimados (Tabela Price) |
| Calculadoras | Juros compostos, independência financeira, reserva de emergência, inflação, financiamento, aposentadoria |
| Relatórios | Fluxo de caixa, evolução patrimonial, gastos por categoria |
| Análise automática | Insights em linguagem natural gerados a partir dos seus dados reais |
| Configurações de rentabilidade | Taxas estimadas configuráveis, usadas pelas calculadoras |

Veja o **[guia de estudo](./GUIA_DE_ESTUDO.md)** para uma explicação detalhada de cada um.

---

## Estrutura do projeto

```
personal-finance-app/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/java/com/financeapp/
│       ├── config/         → SecurityConfig, OpenApiConfig
│       ├── security/       → JWT (geração, filtro, UserDetails)
│       ├── entity/         → 12 entidades JPA (User, Account, Transaction, Bill, CreditCard...)
│       ├── repository/     → Spring Data JPA
│       ├── dto/            → records de request/response
│       ├── service/        → regras de negócio e cálculos financeiros
│       ├── controller/     → endpoints REST
│       ├── util/           → FinanceMath (juros compostos, Tabela Price)
│       ├── exception/      → tratamento global de erros
│       └── resources/
│           ├── application.yml
│           └── db/migration/V1__init_schema.sql   → schema completo (13 tabelas)
│   └── src/test/java/...   → testes unitários
└── frontend/
    └── src/
        ├── api/client.js        → axios + interceptor JWT
        ├── context/AuthContext.jsx
        ├── components/          → layout, UI (Card, Button, Modal, StatusStamp, MoneyValue)
        └── pages/                → 12 páginas, uma por módulo
```

---

## Rodando localmente

### Pré-requisitos
- Java 21+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 14+ (local ou Docker)

### 1. Banco de dados

```bash
docker run --name financeapp-db -e POSTGRES_USER=financeapp \
  -e POSTGRES_PASSWORD=financeapp -e POSTGRES_DB=financeapp \
  -p 5432:5432 -d postgres:16
```

O Flyway cria todas as 13 tabelas automaticamente na primeira vez que o
backend sobe.

### 2. Backend

```bash
cd backend
cp .env.example .env     # ajuste as variáveis - preencha MASTER_USER_NAME/EMAIL/PASSWORD:
                          # é o único login que vai existir, não há cadastro
mvn spring-boot:run
```

API em `http://localhost:8080`. Swagger UI em `http://localhost:8080/docs`.
O usuário master é criado automaticamente no primeiro start (banco vazio) -
ver `MasterUserSeeder`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Acesse `http://localhost:5173`.

### 4. Testando

```bash
cd backend
mvn test
```

Cobre: login (usuário master); atualização
automática de saldo (criar/editar/excluir lançamentos), agora com trava de
concorrência para uso simultâneo por duas pessoas; pagamento de contas
futuras (bloqueio de pagamento duplicado, geração automática da próxima
conta recorrente); indicadores do
dashboard (taxa de poupança, comprometimento de renda, saldo previsto);
progresso e aporte mensal de metas (idem, com trava de concorrência);
parcelamento de cartão (soma exata em
centavos); e as 6 calculadoras financeiras (juros compostos, Tabela Price,
inflação, independência financeira, reserva de emergência).

---

## API completa

```
POST   /api/auth/login

GET/POST/PUT/DELETE   /api/accounts
GET/POST/DELETE       /api/categories
GET/POST/PUT/DELETE   /api/transactions?start=&end=
GET                   /api/dashboard
GET                   /api/dashboard/insights

GET/POST/PUT/DELETE   /api/bills?nextDays=
POST                  /api/bills/{id}/pay?accountId=

GET/POST/DELETE       /api/credit-cards
GET/POST              /api/credit-cards/{id}/purchases

GET                   /api/investments/types
GET/POST/PUT/DELETE   /api/investments
GET                   /api/investments/summary

GET/POST/PUT/DELETE   /api/goals
POST                  /api/goals/{id}/contribute

GET/POST/PUT/DELETE   /api/debts
GET                   /api/debts/summary

GET/PUT               /api/settings/rates

POST  /api/calculators/compound-interest
POST  /api/calculators/financial-independence
POST  /api/calculators/emergency-fund
POST  /api/calculators/inflation
POST  /api/calculators/financing
POST  /api/calculators/retirement

GET   /api/reports/cashflow?months=
GET   /api/reports/net-worth?months=
GET   /api/reports/expenses-by-category?start=&end=
```

Todas as rotas (exceto `POST /api/auth/login` e `/docs`) exigem header
`Authorization: Bearer <token>` - inclusive as calculadoras (a doc antiga
dizia que elas eram públicas, mas a configuração de segurança nunca as
liberou; nesta revisão a documentação foi corrigida para bater com o
comportamento real). Não existe rota de
cadastro (`/api/auth/register`) - o único usuário é provisionado por
variável de ambiente, ver `MasterUserSeeder`.

---

## Limitações conhecidas (documentadas de propósito)

Um sistema real tem trade-offs — estes estão documentados no código e no
guia de estudo, não escondidos:

1. **Variação do patrimônio** é reconstruída a partir dos lançamentos do mês
   (não há uma tabela de snapshot histórico ainda). Uma extensão natural:
   rodar um job mensal que grava o patrimônio em uma tabela `net_worth_history`.
2. **Evolução do patrimônio nos relatórios** considera apenas o saldo das
   contas — investimentos não têm histórico de valor, só o valor atual.
3. **Ciclo de fatura do cartão** usa uma regra simplificada (parcela = compra
   + N meses); sistemas bancários reais têm regras mais finas para compras
   feitas perto da data de fechamento.

---

## Publicação em produção

Veja o guia completo em [`DEPLOY.md`](./DEPLOY.md) — Vercel (frontend) +
Render (backend) + PostgreSQL gerenciado, **incluindo como instalar o app
no celular** (é um PWA: ícone na tela inicial, tela cheia, atualização
automática — sem precisar de loja de aplicativos).
