# OldVicios

Plugin oficial do clã **Old Vicios** para o RuneLite.

> Em desenvolvimento.

## Funções

- Envio de drops e pets para o Discord do clã
- PBs do clã (em breve)

## Privacidade

Todas as funções online vêm **desativadas por padrão** e só funcionam depois que você ativa **Conectar ao Old Vicios** nas configurações.

Com a conexão ativa, o plugin envia ao servidor do Old Vicios:

- seu endereço IP (como qualquer requisição de rede);
- o nome do seu personagem;
- um identificador anônimo da conta (hash do `accountHash` do RuneLite), usado para reconhecer sua conta mesmo após troca de nick;
- os dados das funções que você ativar (por exemplo, o item, o valor e uma captura de tela do drop).

O plugin não coleta credenciais nem dados de outros jogadores.

## Desenvolvimento

Requer JDK 11 ou superior.

```bash
./gradlew run
```

Abre um RuneLite de desenvolvimento com o plugin carregado. Para logar com Jagex Account, siga [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Licença

BSD 2-Clause — veja [LICENSE](LICENSE).
