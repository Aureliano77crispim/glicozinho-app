# Relatório da revisão técnica — Glicozinho v1.5

**Versão do app:** 1.0.5 (`versionCode` 6)  
**Escopo:** revisão de código e estrutura, com inclusão da data do aparelho, calendário mensal e agenda de consultas/exames.  
**Resultado:** revisão estática e testes locais aprovados; compilação Gradle e ensaios em aparelho ainda pendentes.

## Resumo da revisão em três passagens

### Passagem 1 — Produto, experiência e fluxo de agenda

- Confirmada a decisão de exibir na tela inicial a data e a hora locais do próprio telefone, e não uma data fixa. O relógio é atualizado periodicamente e ao retornar ao aplicativo.
- Implementado calendário mensal com navegação entre meses, seleção de dia, realce do dia atual e indicação dos dias que têm compromissos.
- Implementada agenda para consultas, exames e outros compromissos, com título, data/hora, profissional/clínica, local e observações opcionais.
- Incluídas opções de lembrete e escolha entre aviso flexível e pontual.
- Incluídas ações de concluir, reabrir e excluir. Concluir cancela o lembrete; reabrir agenda novamente apenas se o horário ainda estiver no futuro.

### Passagem 2 — Dados, validação e comportamento

- Validação estrita de datas locais de consulta/exame, incluindo rejeição de datas impossíveis como 31 de fevereiro.
- IDs de compromisso duplicados são rejeitados na restauração do backup e removidos durante a normalização defensiva de dados.
- Limites explícitos de quantidade de registros e de tamanho dos campos; validação nativa adicional antes de persistir o JSON criptografado.
- Datas/horários de alarme são validados como instantes ISO; estados de conclusão devem ser booleanos. O instante do aviso é recalculado a partir do horário local do compromisso ao voltar ao app, e o horário do alarme já registrado é comparado para corrigir mudanças de fuso/relógio.
- O estado apresentado no calendário distingue compromisso concluído, prazo de aviso passado, alarme pontual, aviso flexível, falta de permissão e falha de agendamento.
- O app verifica se notificações continuam autorizadas antes de apresentar um alarme como agendado.
- Informações sensíveis do compromisso não são copiadas para a notificação; a notificação pública permanece genérica.

### Passagem 3 — Android, permissões e compilação

- `SCHEDULE_EXACT_ALARM` permanece opt-in do usuário; `USE_EXACT_ALARM` não é declarado. O app verifica `canScheduleExactAlarms()` e solicita a tela de configurações quando necessário.
- Avisos flexíveis usam `setAndAllowWhileIdle`; avisos pontuais usam `setExactAndAllowWhileIdle` após conferência de permissão.
- Reagendamento é acionado em inicialização, mudança de fuso/horário e alteração da autorização de alarmes; o receiver de boot ficou não exportado. A interface compara o horário esperado com os metadados nativos para reagendar se o fuso ou o relógio mudou.
- O app permanece offline nesta edição: não declara `INTERNET`, contatos, localização ou microfone. Backup Android automático permanece desligado; dados locais usam AES-GCM/Android Keystore; backup exportado tem criptografia por senha.
- Build version incrementado para `versionCode 6` / `versionName 1.0.5`; fluxos Debug e Release atualizados para build-tools 36.0.0 e artefatos identificados como 1.0.5.

## Testes automatizados executados

- `node scripts/test_web_logic.cjs` — aprovado. Inclui validação de backup, estados de insulina, limites, datas futuras, recorrência, esquema de compromisso, datas locais impossíveis, IDs duplicados, instantes de lembrete, estado booleano, normalização defensiva, grade mensal de 42 dias e rollback de gravação.
- `node --check` no JavaScript extraído do HTML — aprovado.
- Verificação de IDs HTML duplicados e referências de `getElementById` — aprovada pela rotina do validador.
- Parsing de todos os XMLs sob `app/src/main` — aprovado.
- `python scripts/validate_project.py` — aprovado localmente; inclui as verificações anteriores, manifesto/permissões, armazenamento, criptografia, workflows, versão, build-tools e proteção da chave. Os workflows repetem a validação antes da compilação.
- ZIP será verificado com `unzip -t` antes da entrega.

## Limites que não podem ser resolvidos apenas por revisão estática

1. **Sem compilação Gradle local:** este ambiente não dispõe de Gradle/Android SDK instalados. A compilação real precisa ser feita pelo GitHub Actions; qualquer erro de compilação encontrado deverá ser corrigido com base no log.
2. **Sem teste físico/emulador:** não foram validados o Android 13–16, diferentes fabricantes, tela bloqueada, economia de bateria, notificações desativadas, reboot, troca de fuso ou concessão/revogação de alarmes exatos.
3. **Entrega de notificações não é garantida:** alarmes flexíveis podem atrasar; alarmes pontuais exigem permissão especial e ainda dependem do estado do dispositivo. Não usar como único aviso para obrigações clínicas críticas.
4. **Sem servidor/sincronização/família:** recursos remotos não estão implementados nesta versão e não devem ser anunciados como disponíveis.
5. **Sem validação clínica, segurança independente ou Anvisa/LGPD:** o app é um organizador beta, não fornece recomendações ou cálculos terapêuticos e ainda precisa de revisão profissional antes de uso real/comercial.

## Decisão

A versão 1.0.5 fica **pronta para seguir ao próximo estágio de compilação e teste controlado**, não declarada como livre de todos os defeitos nem pronta para publicação. A compilação Debug/Release, a instalação e a validação dos lembretes em dispositivos reais são os próximos gates obrigatórios.
