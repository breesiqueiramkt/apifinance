# Guia de publicação — Vercel + Render + PostgreSQL

Este guia assume que o projeto já está num repositório Git (GitHub, GitLab ou
Bitbucket) — tanto a Vercel quanto o Render publicam a partir de um repo.

```bash
cd personal-finance-app
git init
git add .
git commit -m "Fase 1: auth, contas, transações, dashboard"
git branch -M main
git remote add origin <URL_DO_SEU_REPOSITORIO>
git push -u origin main
```

A ordem importa: **banco → backend → frontend**, porque cada etapa depende
da URL gerada na anterior.

---

## 1. Banco de dados PostgreSQL (via Render)

1. Acesse [render.com](https://render.com) → **New** → **PostgreSQL**.
2. Dê um nome (ex: `financeapp-db`), escolha a região mais próxima dos seus
   usuários e o plano (o free tier serve para validar o projeto).
3. Depois de criado, abra o banco e copie a **Internal Database URL** (você
   vai usar essa, não a externa, porque o backend também vai rodar no Render
   — conexão interna é mais rápida e não conta no limite de conexões
   externas).
4. Guarde também usuário e senha, que aparecem na mesma tela — vai precisar
   deles se decidir usar `DB_USERNAME`/`DB_PASSWORD` separados em vez da URL
   completa.

> Alternativas equivalentes: [Neon](https://neon.tech) ou [Supabase](https://supabase.com)
> também oferecem PostgreSQL gerenciado com free tier, caso prefira manter o
> banco fora do Render.

---

## 2. Backend (Render — Web Service)

1. No Render: **New** → **Web Service** → conecte o repositório.
2. Configurações:
   - **Root Directory**: `backend`
   - **Runtime**: Docker *ou* Java (se o Render detectar o `pom.xml`
     automaticamente, ele usa buildpack Java — funciona sem Dockerfile).
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/finance-app-1.0.0.jar`
3. Em **Environment Variables**, adicione:

   | Variável | Valor |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://<host-interno>/<database>` (monte a partir da Internal Database URL do passo 1 — troque `postgres://` por `jdbc:postgresql://` e remova usuário/senha da URL, pois eles vão nas variáveis abaixo) |
   | `DB_USERNAME` | usuário do banco (passo 1) |
   | `DB_PASSWORD` | senha do banco (passo 1) |
   | `JWT_SECRET` | gere uma string aleatória forte — veja comando abaixo |
   | `JWT_EXPIRATION_MS` | `86400000` (24h) ou o que preferir |
   | `CORS_ALLOWED_ORIGINS` | a URL da Vercel (você só vai ter essa depois do passo 3 — pode deixar `http://localhost:5173` por enquanto e voltar aqui pra atualizar) |
   | `PORT` | `8080` (o Render injeta a própria `PORT` automaticamente em muitos planos — se o serviço não subir, confira o valor que o Render está passando) |
   | `MASTER_USER_NAME` | ex: `Família` — só usado para exibir "Bom dia, Família 👋" no topo do app |
   | `MASTER_USER_EMAIL` | o e-mail que você e sua esposa vão usar para logar |
   | `MASTER_USER_PASSWORD` | uma senha forte — compartilhem entre vocês, não existe tela de cadastro |

   Para gerar um `JWT_SECRET` forte:
   ```bash
   openssl rand -base64 48
   ```

4. Clique em **Create Web Service**. O Render builda e sobe o backend; o
   Flyway roda as migrations automaticamente no primeiro start, e o usuário
   master (`MASTER_USER_EMAIL`/`MASTER_USER_PASSWORD`) é criado
   automaticamente nessa primeira subida — não existe tela de cadastro
   neste app, então sem essas duas variáveis preenchidas **ninguém consegue
   entrar**.
5. Quando o deploy terminar, copie a URL pública, algo como
   `https://financeapp-backend.onrender.com`.
6. Teste rapidamente:
   ```bash
   curl https://financeapp-backend.onrender.com/docs
   ```
   Deve carregar o Swagger UI.

> **Nota sobre o free tier do Render:** serviços gratuitos "dormem" após um
> período de inatividade e demoram alguns segundos para acordar na primeira
> requisição. Para uso real, considere o plano pago Starter.

---

## 3. Frontend (Vercel)

1. Acesse [vercel.com](https://vercel.com) → **Add New** → **Project** →
   importe o mesmo repositório.
2. Configurações:
   - **Root Directory**: `frontend`
   - **Framework Preset**: Vite (a Vercel detecta automaticamente)
   - **Build Command**: `npm run build` (padrão)
   - **Output Directory**: `dist` (padrão)
3. Em **Environment Variables**, adicione:

   | Variável | Valor |
   |---|---|
   | `VITE_API_BASE_URL` | a URL do backend do passo 2, ex: `https://financeapp-backend.onrender.com` (sem barra no final) |

4. Clique em **Deploy**. Ao terminar, você recebe uma URL como
   `https://extrato-financeiro.vercel.app`.

---

## 4. Fechando o CORS

Volte ao Render, no backend, e atualize a variável `CORS_ALLOWED_ORIGINS`
para a URL real da Vercel:

```
CORS_ALLOWED_ORIGINS=https://extrato-financeiro.vercel.app
```

Salve — o Render reinicia o serviço automaticamente. Sem esse passo, o
navegador bloqueia as chamadas do frontend para a API (erro de CORS no
console).

Se for usar um domínio próprio depois, adicione as duas origens separadas
por vírgula:
```
CORS_ALLOWED_ORIGINS=https://extrato-financeiro.vercel.app,https://app.seudominio.com
```

---

## 5. Checklist final

- [ ] `https://<backend>.onrender.com/docs` carrega o Swagger
- [ ] `https://<frontend>.vercel.app` carrega a tela de login
- [ ] Login funciona com o `MASTER_USER_EMAIL`/`MASTER_USER_PASSWORD` que
      você configurou no passo 2 (não existe tela de cadastro)
- [ ] Consegue cadastrar uma conta bancária e um lançamento
- [ ] Saldo da conta atualiza automaticamente após lançar uma despesa paga
- [ ] Nenhuma senha, `JWT_SECRET`, `MASTER_USER_PASSWORD` ou credencial de
      banco está commitada no repositório (confira que `.env` está no
      `.gitignore` — já está, mas vale conferir antes do primeiro push)

---

## 6. Instalando no celular (seu e da sua esposa)

O frontend já vem configurado como **PWA** (Progressive Web App): depois
de publicado, o celular consegue "instalar" o site como se fosse um app —
ícone próprio na tela inicial, abre em tela cheia (sem barra de endereço),
e funciona com atualização automática quando você publicar uma versão nova.

Isso só funciona **depois do deploy** (passos 1-4 acima) — instalar direto
do `localhost` do computador não funciona no celular, porque o celular
precisa acessar a URL pela internet.

### No iPhone (Safari — precisa ser o Safari, outros navegadores não suportam isso no iOS)
1. Abra `https://<seu-frontend>.vercel.app` no Safari.
2. Toque no ícone de compartilhar (o quadrado com a seta pra cima).
3. Toque em **"Adicionar à Tela de Início"**.
4. Confirme o nome ("Extrato") e toque em **Adicionar**.

### No Android (Chrome)
1. Abra a URL no Chrome.
2. Chrome deve mostrar sozinho um banner **"Instalar app"** na parte de
   baixo — toque nele. Se não aparecer, toque nos três pontinhos (⋮) no
   canto superior direito → **"Instalar aplicativo"** (ou "Adicionar à
   tela inicial").
3. Confirme.

Depois disso, o ícone "Extrato" aparece na tela inicial dos dois celulares,
abre em tela cheia, e qualquer atualização que você publicar depois chega
sozinha (o service worker atualiza em segundo plano).

### Um único login, para vocês dois

Este app não tem tela de cadastro: existe **um único usuário**, criado
automaticamente no primeiro start do backend a partir das variáveis
`MASTER_USER_NAME`/`MASTER_USER_EMAIL`/`MASTER_USER_PASSWORD` (passo 2). Você
e sua esposa entram com esse mesmo e-mail e senha, nos dois celulares, e veem
exatamente os mesmos dados em tempo real — é a mesma conta no banco, não duas
contas "sincronizadas".

Se um dia quiser trocar a senha, é preciso alterar direto no banco de dados
(gerar um novo hash BCrypt e fazer um `UPDATE` na tabela `users`) — não existe
tela de "esqueci minha senha" neste app.

---

## Domínio próprio (opcional)

- **Vercel**: Project Settings → Domains → adicione seu domínio e siga as
  instruções de DNS (CNAME/A record).
- **Render**: Settings → Custom Domain, mesmo processo.

Lembre de atualizar `CORS_ALLOWED_ORIGINS` no backend sempre que adicionar
um novo domínio de onde o frontend será servido.
