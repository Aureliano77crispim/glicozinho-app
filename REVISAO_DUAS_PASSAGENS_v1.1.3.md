# Glicozinho 1.1.3 — revisão em duas passagens

## Escopo
Base: projeto completo v1.1.2 enviado anteriormente. Mantidas as telas e os fluxos de saúde, medicamentos, agenda, progresso, perfil, backup e lembretes. Alterações concentradas na área Corinthians 1910 e no player compacto.

## Passagem 1 — interface e integração
- Substituído o escudo anterior pela arte da águia/escudo enviada pelo usuário, com fundo branco removido e transparência PNG.
- O atalho Corinthians 1910 agora rola até o player na tela inicial, sem abrir modal.
- Removida a lista de músicas. A troca é feita por anterior/próxima.
- Criado player compacto com Play/Pause, anterior, próxima, barra de progresso, volume, aleatório e repetir.
- Incluído nome da faixa atual e indicação da próxima faixa.
- Removido o fluxo de busca/abertura do YouTube.

## Passagem 2 — lógica, segurança e regressão
- Cinco faixas locais com nomes de arquivo fixos e allowlist na ponte Android.
- Reprodução automática do hino somente se o MP3 estiver realmente empacotado no APK; WebView configurado para permitir autoplay.
- Se o MP3 não estiver presente, o player informa isso sem fingir que está tocando.
- Mantido o app offline e sem permissão INTERNET.
- Verificados IDs únicos, referências de elementos, sintaxe JavaScript, fluxos de dados, backups, lembretes, manifesto e configuração de versão.

## Arquivos de áudio pendentes
Por direitos autorais, não foram incluídas gravações comerciais nem gravações de torcida. Para ativar o som, o usuário precisa colocar arquivos que tenha autorização para usar em `app/src/main/assets/music/`, com os nomes descritos em `app/src/main/assets/music/LEIA-ME.txt`. Sem esses MP3s, controles e interface estão integrados, mas não existe áudio para reproduzir.

## Resultado
- Validação estática do projeto: aprovada.
- Testes de lógica existentes: aprovados.
- Testes de interface/player: aprovados.
- Sintaxe JavaScript: aprovada.
- Compilação Gradle/Android e teste em aparelho: ainda precisam ser executados no GitHub Actions e no Samsung.
