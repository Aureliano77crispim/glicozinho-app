# Glicozinho — correções visuais e de área segura v1.0.7

## Alterações aplicadas
1. Barra inferior mantida com fundo branco opaco, sem transparência/desfoque, prioridade de camada e botões com área de toque ampliada.
2. Barras do sistema Android com cores sólidas e ícones escuros; removido o padding manual de insets no WebView, que interferia em elementos fixos.
3. Cabeçalho responsivo em telas estreitas, com quebra controlada entre marca e ações.
4. Ícone neutro de calendário em SVG; removido o emoji que sempre exibe 17 de julho, apesar da data real do aparelho.
5. Versão incrementada para 1.0.7 (versionCode 8).

## Verificações e limites
- Executar os testes JavaScript e o validador antes do envio.
- A revisão de código não substitui teste no aparelho físico.
- A compilação APK deve ser confirmada pelo GitHub Actions; não declarar APK pronto antes do workflow terminar com sucesso.
- Não limpar dados nem desinstalar a versão atual antes de confirmar backup.
