# Glicozinho — alteração CNS v1.5.3

## Alteração implementada
- Campo opcional para o número do Cartão Nacional de Saúde (CNS) no Perfil.
- Validação de exatamente 15 algarismos; a entrada aceita apenas números.
- Exibição abaixo da data na tela inicial, inicialmente mascarada, mostrando somente os quatro últimos dígitos.
- Botão Mostrar/Ocultar para revelar temporariamente o número completo.
- O CNS acompanha o backup criptografado e é validado na restauração.
- Dados antigos continuam compatíveis: se não houver CNS cadastrado, a linha não aparece.

## Revisões realizadas
1. Sintaxe JavaScript conferida com Node.js: aprovada.
2. Testes automatizados de lógica existentes: aprovados.
3. Validação estática do projeto: aprovada.

## Limitações
A validação confirma formato (15 dígitos), não confirma que o número pertence ao usuário nem consulta sistemas do SUS. O dado é sensível: cadastre-o somente se desejar, proteja o aparelho e não compartilhe capturas de tela com o número visível. A alteração ainda precisa ser compilada no GitHub Actions e testada no aparelho antes de ser considerada aprovada.
