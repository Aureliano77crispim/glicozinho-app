#!/usr/bin/env python3
"""Static safety and consistency checks for the Glicozinho Android project."""
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from html.parser import HTMLParser

ROOT = Path(__file__).resolve().parents[1]
HTML = ROOT / "app/src/main/assets/index.html"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
GRADLE = ROOT / "app/build.gradle.kts"
MAIN_KT = ROOT / "app/src/main/java/br/com/glicozinho/app/MainActivity.kt"
SCHEDULER_KT = ROOT / "app/src/main/java/br/com/glicozinho/app/ReminderScheduler.kt"
RECEIVER_KT = ROOT / "app/src/main/java/br/com/glicozinho/app/ReminderReceiver.kt"
NOTIFICATION_ICON = ROOT / "app/src/main/res/drawable/ic_stat_glico.xml"
README = ROOT / "README.md"
REPORT = ROOT / "REVISAO_TECNICA_v1.5.md"
TEST_JS = ROOT / "scripts/test_web_logic.cjs"
WORKFLOW_DEBUG = ROOT / ".github/workflows/android-apk.yml"
WORKFLOW_RELEASE = ROOT / ".github/workflows/android-release.yml"
errors: list[str] = []
checks: list[str] = []

required = [HTML, MANIFEST, GRADLE, MAIN_KT, SCHEDULER_KT, RECEIVER_KT, NOTIFICATION_ICON, README, REPORT, TEST_JS, WORKFLOW_DEBUG, WORKFLOW_RELEASE]
for path in required:
    if not path.is_file() or path.stat().st_size == 0:
        errors.append(f"Arquivo obrigatório ausente/vazio: {path.relative_to(ROOT)}")

if not errors:
    # JavaScript syntax
    html = HTML.read_text(encoding="utf-8")
    if "Versão beta 1.0.9" not in html:
        errors.append("Rodapé HTML deve exibir a mesma versão beta 1.0.9 do aplicativo.")
    if "Você tem um compromisso futuro na sua agenda." not in html or "speechSynthesis.speak(u)" not in html:
        errors.append("Botão Ouvir deve oferecer leitura em voz alta da saudação e do próximo compromisso.")
    scripts = re.findall(r"<script[^>]*>(.*?)</script>", html, flags=re.I | re.S)
    if not scripts:
        errors.append("Nenhum bloco JavaScript foi encontrado no HTML.")
    else:
        try:
            subprocess.run(["node", "--version"], check=True, capture_output=True, text=True)
            with tempfile.NamedTemporaryFile("w", suffix=".js", encoding="utf-8", delete=False) as f:
                f.write("\n".join(scripts))
                js_path = Path(f.name)
            try:
                result = subprocess.run(["node", "--check", str(js_path)], capture_output=True, text=True)
                if result.returncode:
                    errors.append("Sintaxe JavaScript inválida: " + (result.stderr or result.stdout).strip())
                else:
                    checks.append("Sintaxe JavaScript verificada pelo Node.js")
            finally:
                js_path.unlink(missing_ok=True)
        except (FileNotFoundError, subprocess.CalledProcessError):
            errors.append("Node.js não está disponível para validar o JavaScript.")

    # HTML structural check: unique ids and all static getElementById references present.
    class IdCollector(HTMLParser):
        def __init__(self):
            super().__init__()
            self.ids = []
        def handle_starttag(self, tag, attrs):
            for key, value in attrs:
                if key == "id" and value:
                    self.ids.append(value)
    collector = IdCollector()
    collector.feed(html)
    duplicates = sorted({item for item in collector.ids if collector.ids.count(item) > 1})
    if duplicates:
        errors.append("IDs HTML duplicados: " + ", ".join(duplicates))
    referenced_ids = set(re.findall(r"getElementById\(['\"]([^'\"]+)['\"]\)", html))
    missing_ids = sorted(referenced_ids - set(collector.ids))
    if missing_ids:
        errors.append("Referências getElementById sem elemento HTML: " + ", ".join(missing_ids))
    if not errors:
        checks.append("Estrutura HTML: IDs únicos e referências internas resolvidas")

    # Run focused JavaScript behavior tests (backup schema acceptance/rejection).
    logic_test = subprocess.run(["node", str(TEST_JS)], cwd=ROOT, capture_output=True, text=True)
    if logic_test.returncode:
        errors.append("Testes de lógica do aplicativo falharam: " + (logic_test.stderr or logic_test.stdout).strip())
    else:
        checks.append("Testes de lógica: backups válidos aceitos e dados inválidos rejeitados")

    # Parse every XML file in app resources and manifest
    for path in (ROOT / "app/src/main").rglob("*.xml"):
        try:
            ET.parse(path)
        except ET.ParseError as exc:
            errors.append(f"XML inválido em {path.relative_to(ROOT)}: {exc}")
    if not errors:
        checks.append("Manifesto e recursos XML analisados")

    manifest_text = MANIFEST.read_text(encoding="utf-8")
    manifest = ET.parse(MANIFEST).getroot()
    android = "{http://schemas.android.com/apk/res/android}"
    app = manifest.find("application")
    permissions = {node.get(android + "name") for node in manifest.findall("uses-permission")}
    if app is None or app.get(android + "allowBackup") != "false":
        errors.append("Backup automático do Android deve permanecer desativado; use backup criptografado manual.")
    if "android.permission.POST_NOTIFICATIONS" not in permissions:
        errors.append("Permissão POST_NOTIFICATIONS ausente.")
    if "android.permission.RECEIVE_BOOT_COMPLETED" not in permissions:
        errors.append("Permissão RECEIVE_BOOT_COMPLETED ausente.")
    for forbidden in ("android.permission.INTERNET", "android.permission.READ_CONTACTS", "android.permission.ACCESS_FINE_LOCATION", "android.permission.RECORD_AUDIO"):
        if forbidden in permissions:
            errors.append(f"Permissão não justificada encontrada: {forbidden}")
    receivers = {n.get(android + "name") for n in app.findall("receiver")} if app is not None else set()
    if ".ReminderReceiver" not in receivers or ".BootReceiver" not in receivers:
        errors.append("Receivers de notificação/reinicialização ausentes no manifesto.")
    boot_receiver = next((n for n in app.findall("receiver") if n.get(android + "name") == ".BootReceiver"), None) if app is not None else None
    if boot_receiver is not None and boot_receiver.get(android + "exported") != "false":
        errors.append("BootReceiver deve ser não exportado; somente broadcasts do sistema devem reagendar lembretes.")
    if "android.permission.SCHEDULE_EXACT_ALARM" not in permissions:
        errors.append("A permissão opcional SCHEDULE_EXACT_ALARM é necessária para alarmes pontuais de agenda.")
    if "android.permission.USE_EXACT_ALARM" in permissions:
        errors.append("Não use USE_EXACT_ALARM irrestrita; mantenha SCHEDULE_EXACT_ALARM concedida pelo usuário.")
    if not errors:
        checks.append("Permissões Android limitadas; backup automático desativado; receivers declarados")

    gradle = GRADLE.read_text(encoding="utf-8")
    if 'buildToolsVersion = "36.0.0"' not in gradle:
        errors.append("buildToolsVersion precisa ser 36.0.0")
    for pattern, message in [
        (r"compileSdk\s*=\s*36\b", "compileSdk precisa ser 36"),
        (r"targetSdk\s*=\s*36\b", "targetSdk precisa ser 36"),
        (r"minSdk\s*=\s*26\b", "minSdk precisa ser 26"),
        (r"versionCode\s*=\s*10\b", "versionCode precisa ser 10"),
        (r'versionName\s*=\s*"1\.0\.9"', "versionName precisa ser 1.0.9"),
    ]:
        if not re.search(pattern, gradle):
            errors.append(message)
    if "isDebuggable = false" not in gradle or "signingConfig = signingConfigs.getByName(\"release\")" not in gradle:
        errors.append("Release precisa estar não depurável e usar a configuração de assinatura privada.")
    root_build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
    if 'com.android.application\") version \"8.13.2\"' not in root_build:
        errors.append("Versão AGP esperada (8.13.2) não encontrada.")
    if 'org.jetbrains.kotlin.android\") version \"2.4.21\"' not in root_build:
        errors.append("Kotlin Gradle Plugin 2.4.21 não foi encontrado no build raiz.")
    # Detect the exact issue shown in the user's GitHub Actions log: legacy kotlinOptions/jvmTarget String.
    if "kotlinOptions" in gradle or re.search(r'jvmTarget\s*=\s*[\"\']17[\"\']', gradle):
        errors.append("DSL Kotlin antiga encontrada: remova kotlinOptions/jvmTarget String e use compilerOptions com JvmTarget.JVM_17.")
    if gradle.count("compileOptions {") != 1:
        errors.append("app/build.gradle.kts deve conter exatamente um bloco compileOptions.")
    if "compilerOptions" not in gradle or "JvmTarget.JVM_17" not in gradle:
        errors.append("Configuração tipada compilerOptions/JVM_17 ausente.")
    android_end = gradle.find("\n}\n\nkotlin {")
    if android_end < 0:
        errors.append("O bloco kotlin deve ficar fora e depois do bloco android.")
    if not errors:
        checks.append("Configuração Android Release/API 36 e versão 1.0.9 conferidas")

    main_kt = MAIN_KT.read_text(encoding="utf-8")
    scheduler = SCHEDULER_KT.read_text(encoding="utf-8")
    receiver = RECEIVER_KT.read_text(encoding="utf-8")
    if "blockNetworkLoads = true" not in main_kt:
        errors.append("WebView precisa bloquear carregamentos de rede nesta edição local/offline.")
    for needle, message in [
        ("AndroidKeyStore", "Armazenamento precisa usar Android Keystore"),
        ("AES/GCM/NoPadding", "Armazenamento precisa usar AES-GCM"),
        ("PBKDF2WithHmacSHA256", "Backup precisa derivar chave com PBKDF2-HMAC-SHA256"),
        ("BACKUP_ITERATIONS = 210_000", "A quantidade de iterações do backup precisa permanecer adequada"),
        ("deleteAllData()", "O aplicativo precisa permitir apagar dados locais a pedido da pessoa usuária"),
    ]:
        if needle not in main_kt:
            errors.append(message)
    for needle, message in [
        ("nextRoutineReminderInstant", "Restauração precisa recalcular lembretes de rotina"),
        ("syncRoutineReminders()", "Restauração precisa reconstruir lembretes de rotina"),
        ("cancelAllReminders", "Restauração precisa remover alarmes antigos que não pertencem ao backup"),
        ("hideCnsNumber", "O número CNS deve ser ocultado automaticamente"),
    ]:
        if needle not in html:
            errors.append(message)
    if "setAndAllowWhileIdle" not in scheduler or "setExactAndAllowWhileIdle" not in scheduler or "rescheduleAll" not in scheduler or "BOOT_COMPLETED" not in receiver:
        errors.append("Agendamento flexível/pontual ou reagendamento de inicialização incompleto.")
    if "MY_PACKAGE_REPLACED" not in receiver or "android.intent.action.MY_PACKAGE_REPLACED" not in manifest_text:
        errors.append("Lembretes precisam ser reconstruídos após atualização do aplicativo.")
    if "fun cancelAllReminders()" not in main_kt:
        errors.append("Bridge nativa precisa permitir limpar alarmes antigos durante restauração do backup.")
    if "Autorize notificações" not in html or "pending_notification_permission" not in html:
        errors.append("A interface precisa sinalizar quando a permissão de notificações foi revogada.")
    if "canScheduleExactAlarms" not in scheduler or "ACTION_REQUEST_SCHEDULE_EXACT_ALARM" not in main_kt:
        errors.append("A autorização especial de alarmes pontuais precisa ser verificada e solicitada corretamente.")
    if "appointments" not in main_kt or "validAppointments" not in main_kt or "LocalDateTime.parse" not in main_kt or "reminderNumber.toDouble()" not in main_kt or "appointments" not in html or "scheduleAppointmentReminder" not in main_kt or "areNotificationsEnabled" not in main_kt:
        errors.append("Agenda de consultas/exames não está integrada ao armazenamento nativo e aos alarmes.")
    for element_id in ("deviceDate", "deviceTime", "calendarGrid", "appointmentList", "appointmentForm"):
        if element_id not in html:
            errors.append(f"Componente da agenda/data ausente: {element_id}")
    if "setVisibility(Notification.VISIBILITY_PRIVATE)" not in receiver:
        errors.append("Notificações devem ocultar detalhes na tela bloqueada.")
    if "reminder_title" in receiver or "reminder.title" in scheduler or "put(\"title\"" in scheduler:
        errors.append("Títulos potencialmente sensíveis não devem ser persistidos na camada nativa de lembretes.")
    if "setSmallIcon(R.drawable.ic_stat_glico)" not in receiver:
        errors.append("Notificações precisam usar o ícone monocromático dedicado.")
    if "commitData(mutator)" not in html or "db=JSON.parse(previous)" not in html:
        errors.append("Alterações de dados devem reverter a memória se a persistência falhar.")
    if "isReminderScheduled" not in html or "isScheduled(context" not in scheduler or "isReminderScheduledAt" not in html or "fun isScheduledAt" not in scheduler or "appointmentReminderInstant" not in html:
        errors.append("A interface precisa refletir quando lembretes únicos já foram disparados.")
    if "window.onGlicozinhoNativeResume" not in html or "onResume()" not in main_kt or "setInterval(updateDeviceDate,30000)" not in html:
        errors.append("Os alarmes de agenda precisam ser reavaliados ao voltar das configurações Android.")
    if "GlicozinhoNative.loadData" not in html or "GlicozinhoNative.saveData" not in html:
        errors.append("HTML não está usando armazenamento nativo criptografado no Android.")
    if "decryptBackup" not in html or ".glico" not in html or "if(storageLocked){toast('Os dados locais não puderam ser abertos." not in html:
        errors.append("Fluxo de backup criptografado ausente ou exportação liberada com armazenamento bloqueado.")
    if "scheduleReminder" not in html or "cancelReminder" not in html or "fun scheduleReminder(id: String, triggerAtMillis: Long, daily: Boolean)" not in main_kt:
        errors.append("Fluxo de criação/cancelamento de lembretes não está conectado ao Android.")
    if "deleteAllData" not in html or "APAGAR" not in html:
        errors.append("Fluxo de exclusão dos dados locais não está disponível.")
    if "localStorage" in html and "window.GlicozinhoNative" not in html:
        errors.append("Armazenamento local web não está isolado do aplicativo Android.")
    if not errors:
        checks.append("Criptografia local, backup protegido, lembretes e privacidade de notificações conferidos")

    # Check workflow references and secrets handling
    debug_wf = WORKFLOW_DEBUG.read_text(encoding="utf-8")
    release_wf = WORKFLOW_RELEASE.read_text(encoding="utf-8")
    for action in ("actions/checkout@v7", "actions/setup-java@v6", "gradle/actions/setup-gradle@v6", "actions/upload-artifact@v7"):
        if action not in debug_wf or action not in release_wf:
            errors.append(f"Action esperada ausente em um dos workflows: {action}")
    for wf_name, workflow in (("Debug", debug_wf), ("Release", release_wf)):
        if "gradle-version: '8.14.4'" not in workflow:
            errors.append(f"Workflow {wf_name} não está fixado em Gradle 8.14.4.")
        if "kotlinOptions" in workflow or "gradle-version: '8.9'" in workflow or "gradle-version: '8.13'" in workflow:
            errors.append(f"Workflow {wf_name} contém configuração antiga de compilação.")
    if "run: gradle --no-daemon --stacktrace :app:clean :app:assembleDebug" not in debug_wf:
        errors.append("Workflow Debug não executa a compilação limpa do APK.")
    if ":app:assembleRelease :app:bundleRelease" not in release_wf:
        errors.append("Workflow Release não compila APK e AAB.")
    for secret in ("ANDROID_KEYSTORE_BASE64", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD"):
        if secret not in release_wf:
            errors.append(f"Segredo de assinatura ausente no workflow Release: {secret}")
    if "build-tools;36.0.0" not in debug_wf or "build-tools;36.0.0" not in release_wf:
        errors.append("Workflows precisam instalar Android Build Tools 36.0.0 para o SDK 36.")
    if "persist-credentials: false" not in debug_wf or "persist-credentials: false" not in release_wf:
        errors.append("Checkout deve usar persist-credentials: false.")
    if not errors:
        checks.append("Workflows de teste/Release revisados e segredos fora do código-fonte")

    # Keep the separately delivered signing kit out of the public repository.
    gitignore = (ROOT / ".gitignore").read_text(encoding="utf-8")
    for pattern in ("*.p12", "ANDROID_KEYSTORE_BASE64.txt", "SEGREDOS_GITHUB.txt", "*glicozinho-upload-key*"):
        if pattern not in gitignore:
            errors.append(f".gitignore deve bloquear material privado: {pattern}")

    # Detect private signing material accidentally stored inside repository
    forbidden_suffixes = {".p12", ".jks", ".keystore", ".pem", ".key"}
    for file in ROOT.rglob("*"):
        if file.is_file() and file.suffix.lower() in forbidden_suffixes:
            errors.append(f"Material privado não pode estar no repositório: {file.relative_to(ROOT)}")
    if not errors:
        checks.append("Nenhum arquivo de chave privada dentro do projeto")

if errors:
    print("VALIDAÇÃO ESTÁTICA REPROVADA")
    for error in errors:
        print("- " + error)
    sys.exit(1)

print("VALIDAÇÃO ESTÁTICA APROVADA")
for check in checks:
    print("- " + check)
print("Limite: não substitui compilação Gradle em runner Android, teste em dispositivo, auditoria independente, avaliação clínica ou regulatória.")
