# Glicozinho Android — versão técnica 1.0.5 (projeto v1.5)

**Glicozinho — Seu companheiro de saúde.** Aplicativo Android beta de organização pessoal, com interface HTML local em WebView e camada nativa Kotlin. Não é dispositivo médico, prontuário clínico validado nem produto liberado para comercialização.

## Novidade desta versão: calendário e agenda

- A tela inicial mostra a data e a hora atuais do próprio aparelho, respeitando idioma, fuso e horário configurados no Android. O relógio visual é atualizado a cada 30 segundos e quando o app retorna ao primeiro plano.
- Calendário mensal navegável com botão para voltar ao dia atual, seleção de datas e marcação visual de dias com compromissos.
- Agenda para consultas, exames e outros compromissos, com data, horário, profissional/clínica, local e observações.
- Opções de aviso: no horário, 10 ou 30 minutos antes, 1 hora antes, 1 dia antes ou sem alarme.
- Aviso flexível (não exige o acesso especial a alarmes exatos; o Android pode atrasá-lo) ou pontual (solicita acesso especial “Alarmes e lembretes” ao usuário).
- O compromisso pode ser concluído, reaberto ou excluído. Concluir cancela o lembrete; reabrir reavalia o agendamento, se ainda estiver no futuro.
- Os detalhes do compromisso ficam no armazenamento criptografado do aparelho e no backup manual criptografado. A notificação na tela bloqueada é genérica e não expõe consulta, clínica ou exame.

## Funções existentes

- Cadastro e histórico local de glicemia, aplicação de insulina, medicamentos, refeições e lembretes.
- Registro de insulina separado em “aplicada”, “não aplicada” e “não tenho certeza”. As unidades são solicitadas somente para uma aplicação que a pessoa registra como realizada. Não calcula dose, não sugere correção e não altera prescrição.
- Registro individual de tomadas de medicamentos e ação para desfazer o registro. O app não presume que a pessoa tomou o medicamento.
- Armazenamento local Android com AES-256-GCM e chave no Android Keystore.
- Backup manual criptografado `.glico`, AES-GCM e PBKDF2-HMAC-SHA256 (210.000 iterações). Senha de 12 a 128 caracteres; não há recuperação de senha.
- Restauração validada antes de substituir os dados; erros preservam os dados existentes.
- Lembretes nativos e reagendamento em reinicialização, mudança de hora/fuso e concessão do acesso a alarmes exatos.
- Exclusão dos dados locais e cancelamento dos lembretes mediante confirmação.
- Modo de texto maior, contraste reforçado, foco e áreas de toque maiores.

## Limites importantes da agenda e dos alarmes

- Um aviso flexível pode atrasar por Doze, economia de bateria ou restrições do fabricante; no Android 12+, `setAndAllowWhileIdle()` pode ser entregue até uma hora após o horário previsto, ou mais tarde sob certas restrições. Não use o app como único meio para compromissos clinicamente críticos. [Documentação Android: agendar alarmes](https://developer.android.com/develop/background-work/services/alarms).
- O aviso pontual usa `SCHEDULE_EXACT_ALARM`, exige concessão especial pelo usuário em versões compatíveis e é reavaliado antes de ser programado. A permissão pode ser revogada; quando isso ocorre, os alarmes exatos futuros são cancelados pelo sistema. [Documentação Android](https://developer.android.com/develop/background-work/services/alarms).
- Notificações exigem autorização de notificações do Android e que o canal do aplicativo não esteja bloqueado. O fabricante pode impor restrições adicionais.
- Se o telefone estiver desligado na hora do alarme, sem bateria ou com o app desinstalado, o lembrete pode não ser entregue naquele momento. O reagendamento após reinicialização só ocorre depois que o sistema inicia e o app recebe o broadcast.
- A agenda ainda não sincroniza com Google Calendar, calendário do aparelho, clínicas ou familiares. Nenhum dado é enviado à internet nesta edição.

## Privacidade e escopo clínico

- O app não pede acesso à internet, contatos, localização ou microfone.
- Não possui login, backend, sincronização em nuvem ou compartilhamento remoto familiar.
- O app não diagnostica, interpreta resultados, calcula doses, recomenda tratamentos ou modifica prescrições.
- Os registros de saúde são dados sensíveis. O armazenamento local criptografado não substitui auditoria independente, controles de ciclo de vida da chave, testes de recuperação, política de privacidade e adequação LGPD.
- Desinstalar o aplicativo ou perder o aparelho pode tornar o armazenamento local indisponível. Exporte backups criptografados e guarde a senha separadamente.

## Requisitos de compilação

- Android Gradle Plugin 8.13.2
- Kotlin Gradle Plugin 2.4.21
- Gradle 8.13
- JDK 17
- Android SDK Platform 36 e Build Tools 36.0.0
- `compileSdk 36`, `buildToolsVersion 36.0.0`, `targetSdk 36`, `minSdk 26`

Consulte a [matriz de compatibilidade do Kotlin Gradle](https://kotlinlang.org/docs/gradle-configure-project.html), as [versões AGP/Gradle](https://developer.android.com/build/releases/about-agp) e as [notas do SDK Build Tools](https://developer.android.com/tools/releases/build-tools) antes de atualizar ferramentas.

## Compilar APK Debug para teste

O workflow `.github/workflows/android-apk.yml` executa `scripts/validate_project.py`, compila `:app:assembleDebug`, verifica a assinatura do APK e disponibiliza `Glicozinho-APK-Debug-v1.0.5`. O pacote Debug usa o sufixo `.debug` e não é destinado à distribuição comercial.

## Compilar APK/AAB Release assinado

1. Mantenha o kit de assinatura privado fora do repositório. **Nunca envie o `.p12`, a senha ou o Base64 da chave para o repositório público.**
2. No GitHub, abra **Settings → Secrets and variables → Actions → New repository secret**.
3. Cadastre estes segredos usando os valores do kit privado:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
4. Abra **Actions → Glicozinho - Release assinado → Run workflow**.
5. O workflow valida os segredos, executa a validação estática, tenta compilar `app-release.apk` e `app-release.aab` e verifica as assinaturas antes de disponibilizar `Glicozinho-Release-Assinado-v1.0.5`.

A assinatura deste projeto é uma chave privada de upload. Guarde uma cópia segura. Perder a chave pode impedir atualizações diretas com a mesma identidade. Uma assinatura própria não garante remoção dos avisos do Play Protect nem aprovação da Play Store.

## Revisão e testes desta versão

O relatório `REVISAO_TECNICA_v1.5.md` descreve as três passagens de revisão, os testes executados e os limites ainda pendentes. Os workflows executam a validação antes de compilar.

**Limite desta entrega:** foram executados testes de lógica JavaScript, verificação de sintaxe JavaScript, análise estrutural de HTML, parsing dos XMLs e validação de configuração do projeto. Este ambiente não tem Gradle/Android SDK instalados; portanto, não foi possível executar uma compilação Gradle real nem testar a instalação, a permissão de notificações, o comportamento com tela bloqueada ou a pontualidade em aparelhos físicos. A primeira compilação real deve ser confirmada no GitHub Actions. Os alarmes precisam ser testados em aparelhos e fabricantes diferentes.

## Antes de uso real ou comercial

Não use esta versão beta como prontuário clínico nem introduza dados reais de saúde nos primeiros testes. Antes de qualquer lançamento, faça testes em aparelhos e versões Android diferentes, auditoria independente de segurança, testes de acessibilidade com pessoas idosas, revisão clínica por profissionais habilitados, documentação LGPD, termos e política de privacidade e avaliação regulatória especializada sobre eventual enquadramento perante a Anvisa. A validação estática não substitui essas etapas.
