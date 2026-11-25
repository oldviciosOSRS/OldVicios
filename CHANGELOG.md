# Changelog

Novidades de cada versão do plugin OldVicios, da mais nova para a mais antiga.

<!--
Toda versão nova ganha uma seção aqui, no mesmo commit que muda o version= do
runelite-plugin.properties. Quando o PR da versão é mergeado no runelite/plugin-hub, o workflow
.github/workflows/changelog.yml posta a seção no Discord (#🪵┃plugin-changelog) com a data do
merge. Formato e regras: AGENTS.md, seção "Releasing a new version".
-->

## OldVicios 0.1.2

**Corrigido**
- O painel piscava e recarregava os cards a cada teleporte, troca de mundo ou entrada em instância
- O texto dos avisos e eventos era cortado na lateral do painel

**Modificado**
- Avisos e eventos editados ou removidos pela staff atualizam sozinhos em menos de 1 minuto. Antes levava até 5 minutos e fechava o "Ver mais"
- O texto dos cards agora é justificado e ocupa a largura toda

## OldVicios 0.1.1

**Corrigido**
- A descrição do plugin no Plugin Hub aparecia com caracteres quebrados ("clÃ£")

**Modificado**
- Nova descrição no Plugin Hub: "Plugin oficial do clã OldVicios"

## OldVicios 0.1.0

*primeira versão, em testes com a staff*

**Adicionado**
- Conexão automática com a conta do clã, sem cadastro nem senha. A conta continua reconhecida depois de troca de nick
- Painel na barra lateral com o ícone do seu cargo no clã, seu nick, links do site, Discord e Wise Old Man e botão de configurações
- Envio de drops para o Discord do clã, com print, valor e kill count, incluindo raids e baús (ative em **Enviar drops**)
- Envio de pets para o Discord (junto com **Enviar drops**)
- Envio de drops PK para o canal de PK, sem valor mínimo (ative em **Enviar drops PK**)
- Mensagem do drop personalizável, com %USERNAME%, %LOOT%, %SOURCE%, %COUNT%, %TOTAL_VALUE% e %WORLD%
- Aba **Avisos**, com os avisos publicados pela staff
- Aba **Eventos**, com os próximos eventos, data e hora de Brasília e quanto tempo falta
- Cards com imagem e texto completo em "Ver mais"
- Notificação no chat do jogo e do RuneLite quando sai um evento ou aviso novo (ajuste em **Notificações**)
- Opção **Posição na sidebar**
- Nada é enviado em mundos Seasonal, Deadman, LMS, beta, torneio, Fresh Start e PvP Arena
