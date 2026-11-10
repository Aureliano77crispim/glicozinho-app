# Glicozinho 1.1.3 — auditoria de prontidão para teste final

Data: 10/11/2026. Base examinada: pacote v1.1.3.

## Resultado
O código-fonte passou na validação estática e nos testes automatizados disponíveis. Está pronto para ser enviado ao GitHub Actions para compilar um APK de teste e, depois, instalado no Samsung para validação real.

**Ainda não é correto declarar o aplicativo totalmente aprovado:** a compilação Android e o teste físico não foram executados neste ambiente. O player não contém MP3s; portanto, a reprodução do hino/cantos só funcionará depois de adicionar gravações autorizadas.

## Confirmação da foto de perfil
1. Na tela inicial, tocar em **Perfil** no cabeçalho.
2. Na tela **Meu perfil**, tocar em **Escolher foto**.
3. O Android abre o seletor nativo de imagens/arquivos.
4. A imagem é validada, redimensionada para no máximo 512 px no maior lado e comprimida.
5. A foto é salva em `db.profile.photo`, atualiza o avatar do perfil e o avatar pequeno do cabeçalho.
6. Ao fechar e reabrir o app, `applyProfile()` chama `renderProfilePhoto()` para restaurar a foto.
7. **Remover foto** limpa o campo da imagem.

A foto acompanha os dados do perfil e, portanto, o backup protegido. O botão **Perfil** do cabeçalho abre primeiro a tela de perfil; depois, escolha **Escolher foto**.

## Passagem 1 — Perfil e galeria
- Conferidos botão Perfil, input `image/*` e botão Escolher foto.
- Conferidos limites do arquivo, dimensões, compressão e tamanho final.
- Conferidos salvamento, renderização nos dois avatares, restauração e remoção.
- Conferida a preservação da foto na normalização e backup.

## Passagem 2 — Integração Android e regressão
- Conferidos `WebChromeClient.onShowFileChooser`, `ACTION_OPEN_DOCUMENT` e retorno da URI ao WebView.
- Rodados testes de lógica, validação estrutural e testes do player Corinthians.
- Conferidos manifesto, permissões, configuração Gradle e integridade do ZIP.

## Testes executados
- `python3 scripts/validate_project.py` — aprovado.
- `node scripts/test_web_logic.cjs` — aprovado.
- `node scripts/test_corinthians_feature.cjs` — aprovado.
- `node scripts/test_profile_photo_android.cjs` — aprovado.
- Integridade do ZIP — aprovada.

## Pendências no aparelho/CI
1. Executar GitHub Actions e confirmar `assembleDebug` e assinatura do APK.
2. No Samsung, testar escolher, trocar, remover e persistir foto após fechar/reabrir.
3. Conferir barra inferior versus gestos/botões do Android.
4. Testar alarmes/notificações reais, reinicialização e permissões.
5. Testar backup e restauração com senha.
6. Adicionar MP3s autorizados em `app/src/main/assets/music/`. O pacote atual contém somente `LEIA-ME.txt`, sem gravações. A reprodução automática só inicia se `hino-oficial.mp3` estiver empacotado.

## Roteiro rápido para testar a foto
Abra o app → Perfil → Escolher foto → selecione uma imagem → confirme os dois avatares → feche completamente e reabra → confira se a foto persiste → troque → remova.

Antes de instalar uma compilação com assinatura diferente, exporte e confira um backup protegido. Não desinstale a versão atual nem apague os dados antes de confirmar que o backup pode ser restaurado.

**Conclusão:** código-fonte pronto para compilação e teste físico; ainda não aprovado como versão final de produção.
