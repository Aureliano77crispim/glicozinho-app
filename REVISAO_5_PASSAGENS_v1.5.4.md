# Glicozinho — revisão em 5 passagens (pacote v1.5.4)

## 1. Estrutura
- Conferida a presença do módulo Android, HTML embarcado, recursos, scripts e workflows.
- O pacote contém o código-fonte e os arquivos de validação.

## 2. Lógica e funcionamento
- `node scripts/test_web_logic.cjs`: aprovado.
- Cobertura reportada pelo teste: backup, estado de insulina, limites de dose, datas futuras, perfil, recorrência, agenda, datas locais, duplicidade de IDs, normalização, lembretes e falha de gravação.

## 3. Interface e acessibilidade
- Corrigida a barra inferior: fundo branco opaco, sem transparência nem desfoque; contraste reforçado; z-index elevado; botões com área mínima de 58 px e interação por toque.
- Aumentado o espaço inferior do conteúdo para evitar que a navegação cubra os cartões.
- Limitação: não foi possível executar inspeção visual em aparelhos físicos nesta revisão.

## 4. Dados e privacidade
- Campo opcional de CNS de 15 dígitos presente no perfil.
- A tela inicial exibe o CNS abaixo da data, mascarado por padrão, com controle Mostrar/Ocultar.
- Validação rejeita números com quantidade diferente de 15 dígitos.
- Recursos de armazenamento local e backup precisam continuar sendo testados no aparelho; não foi feita auditoria independente de segurança.

## 5. Build e entrega
- Atualizado `versionCode` para 7 e `versionName` para 1.0.6.
- Workflow do GitHub Actions atualizado para nomear o artefato Debug v1.0.6.
- `python scripts/validate_project.py`: aprovado após atualização das expectativas de versão.
- A compilação Gradle desta revisão ainda precisa ser executada pelo GitHub Actions. Não declarar APK novo pronto até o job ficar verde e publicar o artefato v1.0.6.

## Resultado
As verificações locais de lógica e estrutura passaram. A correção da barra e a identificação de versão foram aplicadas ao código-fonte. Falta confirmar a compilação remota e validar a interface no celular antes de substituir a instalação atual.
