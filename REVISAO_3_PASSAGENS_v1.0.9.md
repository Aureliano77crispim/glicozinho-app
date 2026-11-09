# Glicozinho v1.0.9 — revisão profunda em três passagens

**Data da revisão:** 9 de novembro de 2026  
**Base:** projeto Android v1.0.8, histórico de auditoria v1.5.2/v1.5.4 e todas as telas enviadas nesta conversa.  
**Objetivo:** preservar o que já funciona e corrigir falhas de privacidade, restauração e lembretes sem transformar o Glicozinho em sistema de diagnóstico ou cálculo de doses.

## Escopo funcional conferido

Foram consideradas as seis telas e seus caminhos de acesso: **Início**, **Saúde**, **Remédios**, **Agenda e rotina**, **Progresso** e **Perfil/Privacidade**. Também foram considerados o cadastro de glicemia, aplicação de insulina, cadastro/tomada/desfazer medicamento, refeições, lembretes recorrentes e avulsos, compromissos, perfil, contraste, texto ampliado, CNS, backup, restauração e exclusão dos dados. O perfil é aberto pelo botão do cabeçalho; as cinco abas inferiores não representam todas as seis telas.

## Passagem 1 — telas, navegação e fluxos do usuário

- Conferidas as telas a partir das capturas e do código da versão-base.
- Conferida a navegação entre as cinco abas inferiores e a abertura do Perfil pelo cabeçalho.
- Conferidos na tela inicial: saudação, data/hora do dispositivo, CNS mascarado, registros recentes e indicadores.
- Conferidos no Perfil: família/cuidador remoto indisponível nesta edição, privacidade, exportação protegida, apagar dados locais e informações da versão.
- Executada automação visual em Chromium com viewport móvel de 390 × 844 px.
- **Resultado:** 53 verificações de interface aprovadas, abrangendo navegação, cadastro e histórico de glicemia, aplicação/estado da insulina, medicamentos e desfazer tomada, refeições, lembretes, agenda, conclusão/reabertura/exclusão de compromissos, perfil, acessibilidade, CNS, indicadores, exclusão e restauração.

## Passagem 2 — lógica, integridade, segurança e privacidade

**Correções aplicadas em v1.0.9:**

1. **CNS:** permanece mascarado por padrão; ao revelar, volta a ocultar após 15 segundos, ao sair da tela Início ou quando o documento/app fica em segundo plano.
2. **Leitura por voz:** não verbaliza o título, tipo, clínica ou detalhes de consultas/exames; informa apenas que há compromisso futuro.
3. **Lembretes vencidos:** lembretes avulsos já disparados/expirados agora podem ser removidos do histórico; os ativos continuam oferecendo Cancelar.
4. **Permissão de notificações:** ao ser revogada, a interface mostra “Autorize notificações”, suspende os alarmes pendentes e os reprograma quando a autorização volta; a ação de remover/cancelar permanece disponível.
5. **Restauração em outro aparelho:** após validar e gravar o backup, o app limpa os alarmes nativos antigos e recria os lembretes de rotina e compromissos futuros do arquivo restaurado.
6. **Atualização do aplicativo:** o receiver de sistema também reage a `MY_PACKAGE_REPLACED` para reconstruir alarmes após atualização, além de reinicialização e mudanças de hora/fuso.

**Revisão estática de segurança:**

- O manifesto não declara permissão INTERNET, contatos, localização precisa ou microfone.
- A WebView bloqueia carregamentos de rede e navegação externa dentro do app; acesso universal entre arquivos está desativado.
- Os dados locais são protegidos com AES-GCM e chave Android Keystore; o backup manual usa AES-GCM com derivação PBKDF2-HMAC-SHA256 (210.000 iterações).
- O backup automático do Android permanece desativado; a exportação protegida é manual.
- As notificações têm conteúdo genérico e visibilidade privada; títulos de consultas/clínicas não são enviados no texto da notificação.
- Família/cuidador remoto continua **desativado**; não há servidor, login, convites, compartilhamento remoto nem sincronização em nuvem nesta edição. Isso é uma limitação intencional de segurança, não um recurso concluído.
- Os limites de tamanho e quantidade de dados, schema de compromissos, IDs duplicados, datas futuras/inválidas e rollback de falha de gravação são exercitados nos testes de lógica.

## Passagem 3 — validação final e compatibilidade

Executado novamente após as alterações finais:

- `python3 scripts/validate_project.py` — **APROVADO**.
- `node scripts/test_web_logic.cjs` — **APROVADO**: validação/rejeição de backup, estados de insulina, limites, perfil, recorrência, schema de compromissos, datas estritas, IDs duplicados, normalização, fuso/horário de lembrete diário, calendário, limites de coleções e rollback.
- `python3 -m py_compile scripts/validate_project.py` — **APROVADO**.
- Automação Playwright/Chromium móvel — **53/53 verificações aprovadas**, sem erros JavaScript durante os fluxos testados. O bridge Android foi simulado nessa automação.
- XML do manifesto e recursos — parsing estático aprovado. IDs HTML duplicados e referências internas ausentes — nenhuma encontrada.
- ZIP da entrega — integridade conferida com `unzip -t`.
- Identidade do build — `versionName 1.0.9`, `versionCode 10`.
- Configuração inspecionada: AGP 8.13.2, Gradle 8.14.4, Kotlin Gradle Plugin 2.4.21, JDK 17 e SDK 36. A documentação oficial consultada declara AGP 8.13 com Gradle mínimo 8.13 e JDK 17; a matriz Kotlin lista compatibilidade de KGP 2.4.21 com AGP 8.5.2–9.3.1 e Gradle 7.6.3–9.7.0.

## O que ainda não está certificado

1. **APK:** este ambiente não possui Gradle nem Android SDK; portanto, não foi possível executar `assembleDebug` ou `assembleRelease`. O APK só deve ser considerado gerado quando o workflow GitHub Actions terminar com sucesso e publicar o artefato.
2. **Celular real:** as notificações, permissões, alarmes exatos/flexíveis, Doze, economia de bateria, reinicialização, mudança de fuso, bloqueio de tela, leitor TTS e comportamento no Samsung ainda precisam de teste físico.
3. **Criptografia em execução:** o código foi inspecionado estaticamente, mas a criptografia Android Keystore, exportação do arquivo `.glico`, senha errada, corrupção do arquivo e restauração em um segundo aparelho ainda precisam de teste nativo real. O teste de UI usou uma ponte simulada.
4. **Segurança e saúde:** esta revisão não é auditoria independente de segurança, avaliação clínica, aprovação regulatória, adequação LGPD nem teste com pessoas usuárias. O aplicativo continua beta e não diagnostica, interpreta resultados, prescreve ou calcula doses.
5. **Acessibilidade:** modo de texto maior e contraste foram exercitados em navegador; ainda faltam avaliações com leitor de tela e pessoas com diferentes necessidades de acessibilidade.

## Conclusão

A revisão encontrou e corrigiu problemas reais de privacidade e sincronização de lembretes, e a validação estática, os testes de lógica e os 50 testes automatizados de interface passaram. **Não declarar o APK pronto nem distribuir como aplicativo de saúde validado** antes da compilação no GitHub Actions, dos testes no aparelho Android e das avaliações profissionais indicadas acima.
