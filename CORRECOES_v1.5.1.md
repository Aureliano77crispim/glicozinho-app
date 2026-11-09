# Glicozinho — correções de compilação v1.5.1

Esta entrega substitui integralmente os arquivos do projeto v1.5. Não é um patch isolado.

## Correções aplicadas
- Removido o uso obsoleto de `kotlinOptions { jvmTarget = "17" }`.
- Configurado o alvo JVM 17 pela DSL `kotlin.compilerOptions` tipada.
- Mantido um único bloco `compileOptions` dentro de `android`.
- Mantido o bloco `kotlin` separado, fora de `android`.
- Atualizado o Gradle do workflow para 8.14.4 para evitar o aviso de versão mínima futura mostrado no log.
- Mantidas as funcionalidades da versão 1.5: agenda/calendário, lembretes nativos, registros, backup protegido e verificações.

## Verificações executadas nesta entrega
- `python3 scripts/validate_project.py` — aprovado.
- `node scripts/test_web_logic.cjs` — aprovado.
- Conferência da estrutura Gradle — conferida estaticamente.

## Limite importante
Não foi possível executar uma compilação Android real neste ambiente porque não há Gradle/Android SDK instalado aqui. A confirmação final depende do GitHub Actions executar `:app:assembleDebug` e produzir o APK. Não declarar o APK como compilado até o workflow ficar verde.
