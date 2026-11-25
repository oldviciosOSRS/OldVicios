# OldVicios

Plugin oficial do clã **OldVicios** para o RuneLite. Disponível no Plugin Hub; o que mudou em cada versão está no [CHANGELOG.md](CHANGELOG.md), também postado no Discord do clã.

## Funções

- Envio de drops e pets para o Discord do clã (ative em **Enviar drops**; vem desligado por padrão), com mensagem personalizável em **Mensagem do drop** (`%USERNAME%`, `%LOOT%`, `%SOURCE%`, `%COUNT%`, `%TOTAL_VALUE%`, `%WORLD%`). Só vão para o Discord itens que valem 3m ou mais.
- Envio de drops PK para o canal de PK, sem valor mínimo (opcional, ative em **Enviar drops PK**)
- Abas **Avisos** (publicados pela staff) e **Eventos** (próximos eventos do clã)
- Notificação quando sai um evento, um aviso ou uma mensagem pra você (configurável em **Notificações**)
- PBs do clã (em breve)

O plugin só funciona para quem está no clã (grupo do OldVicios no Wise Old Man).

## Instalação e configuração

1. No RuneLite, abra o **Plugin Hub**, procure **OldVicios** e instale.
2. Na engrenagem do plugin:
   - **Geral → Posição na sidebar**: onde o ícone aparece na barra lateral.
   - **Drops → Enviar drops** e **Enviar drops PK**: ligam o envio para o Discord (desligados por padrão).
   - **Drops → Mensagem do drop**: o texto que acompanha o drop.
   - **Notificações → Mensagem no chat** e **Notificação do RuneLite**: como avisar quando sai algo novo.

Nada é enviado em mundos Seasonal, Deadman, LMS, beta, torneio, Fresh Start e PvP Arena.

## Privacidade

Este plugin se comunica com o servidor do OldVicios assim que é instalado. Ele envia:

- seu endereço IP (como qualquer requisição de rede);
- o nome da sua conta no jogo;
- um identificador anônimo da conta (hash do `accountHash` do RuneLite), usado para reconhecer sua conta mesmo após troca de nick;
- seus drops e pets (item, valor e uma captura de tela), se **Enviar drops** estiver ativo. Drops de PvP só são enviados se você ativar **Enviar drops PK**.

Para as abas e notificações, o plugin consulta o servidor do OldVicios a cada 45 segundos e só recarrega as abas quando algo mudou.

O plugin não coleta credenciais nem dados de outros jogadores.

## Desenvolvimento

Requer JDK 11 ou superior.

```bash
./gradlew run
```

Abre um RuneLite de desenvolvimento com o plugin carregado. Para logar com Jagex Account, siga [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

O plugin sempre fala com a API de produção (`api.oldvicios.com.br`), inclusive no cliente de desenvolvimento. Para não mandar drop de teste para o canal do clã, deixe **Enviar drops** e **Enviar drops PK** desligados enquanto testa.

Testes: `./gradlew test`. Como publicar uma versão nova e escrever o changelog: [AGENTS.md](AGENTS.md), seção "Releasing a new version".

## Licença

BSD 2-Clause — veja [LICENSE](LICENSE).
