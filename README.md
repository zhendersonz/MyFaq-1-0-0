# MyFaq 1.0.0

Plugin para **Paper 1.21.1** que responde automaticamente perguntas frequentes no chat.

Criado por **zhendersonz**.

---

## Instalação

1. Baixe o arquivo `MyFaq-1.0.0.jar`
2. Coloque na pasta `plugins/` do seu servidor Paper 1.21.1
3. Copie o `config.yml` de dentro do JAR para `plugins/MyFaq/config.yml` (se quiser personalizar)
4. Reinicie o servidor ou execute `/plugman load MyFaq`

---

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/faq` | Mostra o status atual do FAQ (ativado/desativado) | `myfaq.toggle` |
| `/faq toggle` | Ativa ou desativa o FAQ para você | `myfaq.toggle` |
| `/faq admin` | Lista todos os comandos administrativos | `myfaq.admin` |
| `/faq lista` | Lista todas as FAQs cadastradas com ID, palavras-chave e ativações | `myfaq.admin` |
| `/faq recarregar` | Recarrega o config.yml sem reiniciar o servidor | `myfaq.admin` |
| `/faq top` | Mostra o ranking das FAQs mais ativadas | `myfaq.admin` |
| `/faq test <mensagem>` | Testa uma mensagem contra todas as FAQs, mostrando scores | `myfaq.admin` |
| `/faq clear <jogador>` | Zera as estatísticas de um jogador | `myfaq.admin` |

Aliases: `/myfaq`, `/faqs`, `/perguntas`

---

## Permissões

| Permissão | Efeito | Padrão |
|---|---|---|
| `myfaq.admin` | Acesso a todos os comandos administrativos | OP |
| `myfaq.notify` | Receber notificação quando alguém ativar uma FAQ | false |
| `myfaq.ignorar` | Não ativar FAQs para este jogador | false |
| `myfaq.toggle` | Pode ativar/desativar o FAQ para si mesmo | true |

---

## Como funciona

Quando um jogador digita uma mensagem no chat, o plugin verifica em **4 níveis** se a mensagem corresponde a alguma FAQ cadastrada:

1. **Contém** — A mensagem contém exatamente uma das palavras-chave
2. **Palavra por palavra** — Cada palavra da keyword aparece na mensagem (ignorando palavras curtas de ≤2 letras)
3. **Similaridade (Levenshtein)** — Similaridade por caractere entre cada palavra da keyword e palavras da mensagem
4. **Janela deslizante** — Similaridade em janelas contínuas de palavras

A resposta é enviada **de forma privada** para o jogador (não aparece no chat público).

### Cooldown

- Cada FAQ tem um cooldown configurável (padrão: 60 segundos)
- Mensagens anti-loop como "obrigado", "vlw", "valeu" não disparam FAQs
- Jogadores com `myfaq.ignorar` não ativam FAQs
- NPCs (Citizens) são ignorados automaticamente

### Notificações para staff

Jogadores com `myfaq.notify` recebem uma mensagem quando alguém ativa uma FAQ, informando qual FAQ foi ativada e por quem.

---

## Configuração (`config.yml`)

```yaml
# Prefixo usado nas mensagens do plugin
prefix: "&6[MyFaq]"

# Cooldown global entre ativacoes (segundos)
cooldown: 60

# Palavras anti-loop (nao ativam FAQ)
antiloop:
  - "obrigado"
  - "vlw"
  - "valeu"
  - "blz"
  - "ok"

# Dias para manter logs antes de limpar
limpar-log-dias: 3

# Notificar staff quando alguem ativar FAQ
notificar-staff: true
```

### Estrutura de uma FAQ

```yaml
faqs:
  vip:
    keywords:
      - "comprar vip"
      - "adquirir vip"
      - "preco do vip"
    regex: ""
    responses:
      - "Compre vip em www.example.com"
    command: ""
    responseType: "mensagem"
    permission: ""
    permissionDenied: ""
    clickableText: ""
    clickableCommand: ""
    clickableUrl: ""
    sound: ""
    eventStart: 0
    eventEnd: 0
```

### Campos da FAQ

| Campo | Descrição |
|---|---|
| `keywords` | Palavras-chave que ativam esta FAQ |
| `regex` | Expressão regular alternativa (prioritário) |
| `responses` | Lista de respostas (uma aleatória é enviada) |
| `command` | Comando a ser executado quando ativar |
| `responseType` | `mensagem` ou `title` |
| `permission` | Permissão necessária para ver esta FAQ |
| `permissionDenied` | Mensagem quando não tem permissão |
| `clickableText` | Texto do botão clicável |
| `clickableCommand` | Comando do botão clicável |
| `clickableUrl` | URL do botão clicável |
| `sound` | Som a tocar ao ativar (ex: `BLOCK_NOTE_BLOCK_PLING`) |
| `eventStart` | Início de evento (timestamp em millis, 0 = sem evento) |
| `eventEnd` | Fim de evento (timestamp em millis, 0 = sem evento) |

### Eventos temporários

Você pode criar FAQs que só funcionam durante um período específico usando `eventStart` e `eventEnd` (timestamps em milissegundos). Enquanto o evento estiver ativo, a FAQ funciona normalmente. Fora do período, ela é ignorada.

---

## FAQ Padrão (28 entradas)

O plugin já vem com 28 FAQs pré-cadastradas cobrindo tópicos comuns:

- VIP (compra, preço, como adquirir)
- Loja / Site
- Hack / Hacker
- X-Ray / Cave Finder
- Killaura / Autoclick
- Dupe / Duping
- Lag / Ping
- Reportar / Denunciar
- Staff / Ajuda
- Sugestão / Ideia
- Bug / Glitch
- Tag / Grupo / Cargo
- Warp / Spawn / Spawners
- Kit / Kits
- Money / Dinheiro / Economy
- Land / Terreno
- Clans / Guildas
- Caixa / Key / Chave
- Evento / Eventos
- Live / Youtube / Twitch
- Discord
- Site / Loja
- Como pegar / pegar itens
- Spawner / Spawners
- Enchant / Encantamento
-技能 / KitPvP
- Boss / Bosses
- Dungeon

---

## Compilando (para desenvolvedores)

```bash
mvn clean package
```

O JAR será gerado em `target/MyFaq-1.0.0.jar`.

### Testes

```bash
mvn test
```

60 testes unitários validando similaridade, matching, construtores e lógica do plugin.
