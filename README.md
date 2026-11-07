# Glicozinho para Android — projeto inicial

Este é um projeto Android Studio que empacota o protótipo HTML local em um aplicativo Android usando WebView.

## Abrir e compilar
1. Instale Android Studio (versão atual) e JDK 17.
2. Abra esta pasta no Android Studio: `GlicozinhoAndroid`.
3. Aguarde a sincronização do Gradle e instale o Android SDK Platform 35 se solicitado.
4. Conecte um celular Android com depuração USB ou crie um emulador.
5. Selecione `app` e pressione **Run**.
6. Para gerar APK de teste: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

O APK de debug ficará normalmente em `app/build/outputs/apk/debug/app-debug.apk`.

## O que esta versão Android inclui
- Interface HTML/CSS/JavaScript embarcada no app; abre sem internet.
- Armazenamento local do WebView para dados de demonstração.
- Importação de backup JSON pelo seletor de arquivos Android.
- Exportação de backup JSON pelo seletor de destino Android.
- Navegação de retorno do Android.

## Limitações — não é versão comercial nem clínica
- Esta é uma embalagem Android de protótipo, não um produto pronto para venda.
- Não há autenticação, servidor, sincronização em nuvem ou compartilhamento familiar real.
- Os lembretes ainda não geram notificações nativas confiáveis com o app fechado.
- Não há integração com glicosímetros, sensores ou serviços de saúde.
- Os dados ficam no armazenamento local do aplicativo; desinstalar ou limpar dados pode apagá-los. Faça backup.
- Não utilizar dados médicos reais nesta versão de teste.
- Não calcula nem recomenda doses de insulina ou medicamentos.

Antes de comercializar: implementar notificações com permissões adequadas, autenticação, criptografia e backend seguro; realizar testes em aparelhos; validar fluxos clínicos com profissionais habilitados; revisar LGPD, Anvisa e políticas do Google Play; e confirmar os direitos comerciais da marca e dos ativos visuais.

## Identificação
- Nome exibido: Glicozinho
- applicationId: `br.com.glicozinho.app`
- Versão inicial: 1.0.0
- Android mínimo: 8.0 (API 26)


## Gerar o APK sem instalar Android Studio (compilação na nuvem)
O projeto inclui `.github/workflows/android-apk.yml` para compilar o APK em um computador na nuvem do GitHub Actions.

1. Crie uma conta gratuita em https://github.com.
2. Crie um repositório novo chamado `GlicozinhoAndroid`.
3. Envie todos os arquivos e pastas deste ZIP para o repositório (incluindo a pasta oculta `.github`).
4. No GitHub, abra a aba **Actions** e selecione **Build Glicozinho APK**.
5. Clique em **Run workflow** e aguarde o processo terminar.
6. Na execução concluída, baixe o artefato **Glicozinho-Android-APK**.
7. Extraia o arquivo baixado; dentro estará `app-debug.apk`. Transfira-o para o Android e instale permitindo a instalação dessa origem.

A compilação na nuvem requer conta GitHub e conexão com a internet. O APK gerado será de teste (debug), não uma versão assinada para publicação comercial na Play Store.
