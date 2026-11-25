# CLAUDE.md — plugin OldVicios

## OldVicios

OldVicios é um clã brasileiro de Old School RuneScape, com servidor no Discord, site em `oldvicios.com.br` (Wix) e grupo 715 no Wise Old Man. **O nome é sempre `OldVicios`, tudo junto**, em qualquer texto visível (plugin, painel, mensagens no Discord, documentação).

| Projeto | Repo | O que faz | Onde roda |
|---|---|---|---|
| Plugin RuneLite (este) | `oldviciosOSRS/OldVicios` | Drops, pets, avisos e eventos dentro do jogo | Plugin Hub do RuneLite |
| Backend | `oldvicios/Oldback` | API central: banco, Discord, Wix, plugin | Render, `api.oldvicios.com.br`; jobs no Railway |
| Painel da staff | `oldviciosOSRS/Staff` | Gestão do clã pela staff, com login pelo Discord | Vercel, `staff.oldvicios.com.br` |
| Bot do Discord | `oldvicios/OVBot` | Tickets, troca de nick, comandos | Railway |

Como conversam: o painel chama o backend com um JWT da sessão, o bot chama com `API_KEY`, e o plugin chama `/v1/plugin` identificado pelo header `X-OV-Account`. O backend guarda tudo no Postgres (Supabase) e fala com o Discord (bot e webhooks), o Wix, o Wise Old Man e o DeepL.

## Este plugin

- O plugin só chega aos jogadores quando o PR da versão é aceito no `runelite/plugin-hub`. Um push na `main` não publica nada.
- A API é fixa em `OldViciosApi.BASE_URL` = `https://api.oldvicios.com.br/v1/plugin`, ou seja, **sempre produção**, inclusive no `./gradlew run`. Toda chamada leva `X-OV-Version`.
  - `POST /auth`: nick + `X-OV-Account` (sha256 do `accountHash` com sal, calculado no plugin). O backend confere no Wise Old Man se a conta está no clã.
  - `POST /loot`: drops e pets em multipart, com o print. O backend aplica o valor mínimo (3m por item; PK sem mínimo) e posta no Discord.
  - `GET /notices`, `/events`, `/{aba}/:id/image` e `/updates`. Este último é consultado a cada 45s e só recarrega as abas quando algo muda.
  - O contrato está em `src/routes/pluginRoutes.ts` e `src/services/pluginService.ts` do backend. Mudança de payload precisa ir nos dois repos.
- Testando no cliente de desenvolvimento, deixe **Enviar drops** e **Enviar drops PK** desligados, senão o drop de teste vai para o canal real do clã.
- Teste só pelo `./gradlew run`, com login pelo painel de contas Jagex do próprio cliente (passo a passo no `README.md`).
  - Sideload no RuneLite normal (`~/.runelite/sideloaded-plugins`) não funciona: com launcher, o modo de desenvolvedor fica desligado.
  - Não sugira `--insecure-write-credentials`: grava a sessão da conta em arquivo.
  - Quem testa no jogo é uma pessoa. Nunca automatize input do jogo (ver "Testing" no `AGENTS.md`).

### Regras de configuração

- Toda opção que envia dados ao servidor começa **desligada** e tem o `warning` de servidor de terceiros (`THIRD_PARTY_WARNING`). É regra do Plugin Hub (ver "Config" no `AGENTS.md`).
- **Não crie toggle de "conectar" nem configuração de valor mínimo.**
  - O aviso de servidor externo é o campo `warning=` do manifesto em `plugins/oldvicios` no `runelite/plugin-hub` (mostrado na instalação).
  - O valor mínimo do drop é regra do backend (`PLUGIN_MIN_LOOT_VALUE`), não escolha do jogador.
- Textos para o jogador são em português.
- `runelite-plugin.properties` é lido como ISO-8859-1: acento vira `ã` etc., e o `VersionTest` falha com byte fora do ASCII. Edite pelo shell (`sed`); editores que salvam UTF-8 quebram o arquivo.

### Versões e changelog

Ver "Releasing a new version" no `AGENTS.md`. Toda versão nova ganha a seção dela no `CHANGELOG.md`, e o workflow posta no Discord sozinho quando o PR do Plugin Hub é mergeado.

@AGENTS.md
