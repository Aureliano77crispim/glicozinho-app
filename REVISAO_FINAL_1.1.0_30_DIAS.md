# Glicozinho 1.1.0 — revisão final para piloto pessoal de 30 dias

## Alterações incluídas

1. **Foto de perfil:** adicionada à tela Perfil. O usuário escolhe uma imagem pela galeria, pode removê-la e vê a miniatura também no botão Perfil do cabeçalho. A imagem é redimensionada para no máximo 512 px e convertida para JPEG antes de ser salva. A carga é limitada a 450 KB, validada como imagem e armazenada dentro do conjunto de dados local protegido pelo Android. A foto também acompanha o backup manual criptografado.
2. **Barra inferior:** aumentada em altura, com ícones e alvos de toque maiores. O WebView agora calcula o inset inferior real da barra de navegação/gestos do Android e informa esse valor ao CSS. A navegação do app fica acima da área do sistema e o conteúdo recebe espaço extra para não ficar encoberto.
3. **Versão:** versionCode 11 e versionName 1.1.0. O mesmo applicationId é mantido.
4. **Seletor de fotos:** o WebView passa o tipo `image/*` para o seletor nativo do Android quando a página solicita imagem.

## Três passagens de revisão

### Passagem 1 — Interface e navegação

- Conferidos os seis destinos já auditados: Início, Saúde, Remédios, Agenda, Progresso e Perfil.
- Revisados os alvos da barra inferior, altura, espaço inferior do conteúdo e reposicionamento acima da barra do sistema.
- Conferida a inclusão da seção Foto de perfil no Perfil e da miniatura no cabeçalho.

### Passagem 2 — Dados e segurança

- Foto é reduzida no aparelho antes de entrar no banco local.
- São aceitos somente data URLs de JPEG, PNG ou WebP com limite de 450.000 caracteres.
- Arquivos de origem acima de 8 MB são recusados antes do processamento.
- Foto não é enviada à internet; fica no conjunto de dados local e entra no backup manual protegido.
- A remoção da foto usa o mesmo fluxo de gravação com rollback já usado pelos demais dados.

### Passagem 3 — Validação

- `python scripts/validate_project.py`: aprovado.
- `node scripts/test_web_logic.cjs`: aprovado, incluindo casos de foto válida, payload não-imagem e foto acima do limite.
- `node --check` do JavaScript inline: aprovado.
- Verificação de IDs e referências HTML, Manifest e recursos XML: aprovada pela validação estática.
- Configuração Gradle declarada: versionCode 11, versionName 1.1.0, compile/target SDK 36 e minSdk 26.

## Limites ainda não confirmados

Este ambiente não tem Android SDK/Gradle instalados, portanto não foi possível executar `assembleDebug` aqui. O workflow GitHub Actions precisa compilar o APK desta versão. A barra inferior deve ser confirmada no Samsung físico, pois a altura e o modo de navegação (gestos ou três botões) variam por aparelho. A escolha de foto deve ser testada com imagens da Galeria e do app Arquivos. A atualização por cima da versão anterior depende de assinatura compatível; se o APK anterior foi assinado com uma chave debug efêmera de outro runner, o Android pode rejeitar a atualização.

## Instruções seguras para os 30 dias

1. Antes de instalar, exporte um backup `.glico` protegido e confirme que o arquivo foi criado. Guarde a senha separadamente.
2. Envie o ZIP ao GitHub, mantendo os arquivos na raiz do repositório, e aguarde o workflow `Glicozinho-APK-Debug-v1.1.0` ficar verde.
3. Se o Android aceitar atualização, instale por cima e confira se os registros antigos continuam presentes. Se informar conflito de assinatura, **não apague o app sem antes confirmar o backup**; será necessário instalar com a assinatura correta ou desinstalar e restaurar o backup depois.
4. Teste a foto: selecionar, fechar e reabrir o app, remover, exportar backup e verificar se a foto acompanha a restauração em um ambiente de teste.
5. Teste a barra inferior em modo de gestos e em modo de três botões, conferindo Início, Saúde, Remédios, Agenda e Progresso sem sobreposição.
6. Durante 30 dias, registre falhas, travamentos, perda de dados, atrasos de lembretes, problemas de foto e dificuldades de leitura. Mantenha outro meio para avisos clinicamente importantes.
7. Faça backups periódicos. O Glicozinho é organizador auxiliar; não calcula doses, não substitui prescrição, glicosímetro, plano de tratamento ou orientação profissional.

## Resultado

**Código-fonte validado estaticamente e testes JavaScript aprovados. APK 1.1.0 ainda precisa ser gerado no GitHub Actions e instalado/testado no Samsung.** Esta é uma versão de piloto pessoal de 30 dias, não um lançamento comercial ou uma certificação clínica.
