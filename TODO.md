# TODO — fEconomy

Pendências conhecidas deste plugin (15/10/2026). Riscar quando feito. Detalhes em `docs/superpowers/specs/2026-10-15-feconomy-database-design.md` (no monorepo).

## Funcionalidades
- [ ] `/pay <jogador> <montante>` entre jogadores (precisa de parsing de montantes em `Money`).
- [x] Comandos de administração: dar, tirar e definir saldo (`/money add|remove|set`).
- [ ] Ideias para `/money` admin: alvos offline, registo (log) das alterações de saldo feitas por admins.
- [ ] Expor a economia a outros plugins (por exemplo ao fShop) por `ServicesManager`. Hoje o `EconomyService` não é acessível de fora; quem usar tem de chamar `ensureAccount` antes de depositar.
- [ ] Ideias para `/money send`: impostos/taxas, histórico de transacções, destinatários offline, cooldown/anti-spam.
- [ ] Cooldown no `/money` para não encher a fila de 4 threads da BD.

## Código
- [ ] `PlayerAuthListener`: campo `plugin` sem uso; o log usa `getMessage()` (pode imprimir `null`): usar a causa-raiz como no `MoneyCommand`.
- [ ] `FEconomy.onEnable` apanha `RuntimeException` e só regista `e.getMessage()` (sem stack trace): erros de programação ficam difíceis de diagnosticar.
- [ ] Javadoc de `EconomyRepository.setBalance`: o `@return` ainda fala do `CHECK` para saldos negativos, mas o guarda corre primeiro.
- [ ] `inTransaction` só faz rollback em `SQLException`/`RuntimeException` (num `Error` quem repõe é o pool): apanhar `Throwable`.

## Qualidade
- [ ] Testes em falta: levantamentos concorrentes (double-spend), `format_negativeThrows` com a mensagem, as exceções, `TestDatabaseService.close()` sem `awaitTermination`.
- [ ] O comando e o listener só foram verificados à mão no servidor (sem testes automáticos).

## Limpeza
- [ ] O `economy.db` antigo continua em `plugins/fEconomy/` no servidor de testes, sem uso: apagar quando já não fizer falta.
- [ ] `/money help` hoje só para staff (`feconomy.admin`): abrir a jogadores (mostrando só `/money` e `/money send`).
