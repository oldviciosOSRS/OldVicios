# OldVicios

Plugin oficial do clã **OldVicios** para o RuneLite.

> Em desenvolvimento.

## Funções

- Envio de drops e pets para o Discord do clã (desative em **Enviar drops**)
- Envio de drops PK para o canal de PK (opcional, ative em **Enviar drops PK**)
- PBs do clã (em breve)

## Privacidade

Este plugin se comunica com o servidor do OldVicios assim que é instalado. Ele envia:

- seu endereço IP (como qualquer requisição de rede);
- o nome do seu personagem;
- um identificador anônimo da conta (hash do `accountHash` do RuneLite), usado para reconhecer sua conta mesmo após troca de nick;
- seus drops e pets (item, valor e uma captura de tela), se **Enviar drops** estiver ativo. Drops de PvP só são enviados se você ativar **Enviar drops PK**.

O plugin não coleta credenciais nem dados de outros jogadores.

## Desenvolvimento

Requer JDK 11 ou superior.

```bash
./gradlew run
```

Abre um RuneLite de desenvolvimento com o plugin carregado. Para logar com Jagex Account, siga [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Licença

BSD 2-Clause — veja [LICENSE](LICENSE).
