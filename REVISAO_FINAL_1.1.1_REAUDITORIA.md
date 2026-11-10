# Glicozinho 1.1.1 — reauditoria adicional

## Escopo
Revisão adicional do pacote 1.1.0, mantendo as seis áreas já auditadas (Início, Saúde, Remédios, Agenda, Progresso e Perfil) e concentrando a rechecagem nas duas mudanças solicitadas: foto de perfil e navegação inferior no Android.

## Alterações desta reauditoria
1. **Foto de perfil:** a normalização de dados agora exige um data URL JPEG/PNG/WebP com conteúdo Base64 estruturalmente válido; payloads malformados deixam de ser aceitos durante leitura/restauração.
2. **Imagem excessivamente grande:** antes de desenhar no canvas, o app rejeita imagens sem dimensões válidas, com lado acima de 12.000 px ou mais de 40 megapixels, reduzindo risco de consumo excessivo de memória.
3. **Avatar no cabeçalho:** aumentei o avatar de 24 para 30 CSS px para ficar mais visível.
4. **Barra inferior:** mantive a altura ampliada, os alvos de toque e o cálculo do inset inferior do Android; o CSS posiciona a navegação acima da área de navegação do sistema.
5. **Versionamento:** versionCode 12 / versionName 1.1.1 e nomes dos artefatos do GitHub Actions atualizados.

## Verificações executadas
- `python scripts/validate_project.py` — aprovado.
- `node scripts/test_web_logic.cjs` — aprovado, incluindo aceitação de foto Base64 válida e rejeição de payload Base64 malformado.
- Sintaxe JavaScript, estrutura HTML/IDs, manifesto e recursos XML — aprovados pelo validador.
- Integridade do ZIP e SHA-256 — gerados para este pacote.

## Limites que continuam pendentes
Este ambiente não possui Android SDK/Gradle configurado para uma compilação local. Portanto, a compilação real deve ser confirmada no GitHub Actions. Também não é possível confirmar visualmente o posicionamento final no modelo Samsung do usuário sem instalar o APK no aparelho. A barra inferior precisa ser conferida nos dois modos Android (gestos e três botões), com escala de fonte/tela padrão e ampliada. O seletor de arquivos deve ser testado escolhendo uma foto real da Galeria e removendo-a depois.

## Plano para o piloto de 30 dias
- Não usar o app como instrumento de decisão ou cálculo de dose; ele apenas organiza registros e lembretes.
- Fazer backup protegido antes de atualizar ou reinstalar.
- Testar foto, navegação, glicemia, aplicações, medicamentos, agenda, notificações, exportação e restauração.
- Registrar data, tela, ação, resultado esperado, resultado observado e captura de tela para cada problema.
- Não considerar o piloto concluído até 30 dias de uso e revisão dos problemas relatados.

**Status:** código-fonte revalidado estaticamente; APK ainda depende do workflow e da conferência física no Samsung.
