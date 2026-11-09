# Glicozinho — revisão geral v1.0.8

## Correções aplicadas nesta rodada

1. Sincronizada a versão apresentada no rodapé com a versão real do aplicativo Android: beta 1.0.8.
2. Incrementado o versionCode Android para 9, permitindo atualização sobre a versão anterior sem desinstalar o app.
3. Atualizados os nomes dos artefatos de APK/AAB e a validação estática para a versão 1.0.8.
4. Melhorada a função Ouvir: fala em português, usa o nome salvo no perfil, informa a data local e o próximo compromisso pendente; se não houver, informa isso. Não lê resultados de glicemia, doses, CNS ou outros dados de saúde sensíveis.
5. Mantidos os recursos existentes de registro, histórico, medicamentos, refeições, agenda, alarmes, contraste, texto ampliado, backup criptografado, restauração validada e exclusão confirmada.

## Recursos já implementados e conferidos

- Armazenamento Android local criptografado com AES-GCM e chave no Android Keystore.
- Backup externo criptografado por senha, com confirmação da senha e restauração validada antes da substituição dos dados.
- Exclusão local com confirmação digitando APAGAR e cancelamento dos lembretes.
- Acesso remoto familiar explicitamente desativado; não há servidor, conta ou sincronização nesta versão.
- O app não calcula doses, não interpreta glicemia, não prescreve e não substitui a equipe de saúde.

## Verificações

- `python3 scripts/validate_project.py`
- `node scripts/test_web_logic.cjs`
- `python3 -m py_compile scripts/validate_project.py`
- `unzip -t` no pacote final.

A compilação Gradle real e o teste em aparelho físico dependem do GitHub Actions e do dispositivo Android. Não declarar APK gerado até o workflow terminar com sucesso.
