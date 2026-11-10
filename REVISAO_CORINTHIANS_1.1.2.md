# Glicozinho 1.1.2 — Área Corinthians 1910

## O que entrou
- Atalho visual colocado imediatamente depois do botão Backup no cabeçalho.
- Escudo estilizado local em SVG, com paleta preto/branco/vermelho e elementos de remo, âncora e a inscrição 1910.
- Texto CORINTHIANS 1910 abaixo do escudo.
- Toque no escudo abre painel com cinco opções: Hino oficial do Corinthians; Aqui tem um bando de loucos; Eu nunca vou te abandonar; Louco por ti, Corinthians; Corinthians, minha vida/minha história.
- Cada opção abre uma pesquisa correspondente no YouTube por Intent Android, fora do WebView.
- A ponte nativa aceita apenas cinco IDs predefinidos; não recebe URLs arbitrárias.
- O aplicativo continua sem permissão INTERNET e o WebView continua bloqueando rede.
- Modal e atalhos possuem alvos de toque e layout adaptado para telas estreitas.
- Atualização para versionCode 13 / versionName 1.1.2 e workflows atualizados.

## Nota sobre o escudo
O arquivo `corinthians-crest.svg` é uma ilustração estilizada original de homenagem, não é o arquivo oficial do clube e não sugere patrocínio ou endosso do Sport Club Corinthians Paulista.

## Verificações executadas
- `python3 scripts/validate_project.py`: aprovado.
- `node scripts/test_web_logic.cjs`: aprovado.
- `node scripts/test_corinthians_feature.cjs`: aprovado.
- Sintaxe JS, IDs HTML, referências getElementById, XML, manifesto, permissões e versões: aprovados.

## Verificações ainda necessárias
- Compilação APK no GitHub Actions.
- Conferência visual no Samsung (posição ao lado do Backup e tamanho em diferentes resoluções).
- Teste de abertura de cada pesquisa com YouTube instalado e também sem ele (navegador padrão).
