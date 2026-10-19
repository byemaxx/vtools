"""Safe local regression checks; never invokes ADB or executes device actions."""
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
application = manifest.find("application")
for name in ["com.omarea.scene_mode.ReceiverSceneMode", "com.omarea.vtools.ReceiverCompileState", "com.omarea.scene_mode.ReceiverShortcut"]:
    receiver = next(item for item in application.findall("receiver") if item.get(android + "name") == name)
    assert receiver.get(android + "exported") == "false", name
assert not any(item.get(android + "name") == "com.omarea.scene_mode.TriggerExecutorService"
               for item in application.findall("service"))
policy = ET.parse(root / "app/src/main/res/xml/network_security_config.xml").getroot()
assert policy.find("base-config").get("cleartextTrafficPermitted") == "false"
exceptions = set()
for config in policy.findall("domain-config"):
    if config.get("cleartextTrafficPermitted") == "true":
        for domain in config.findall("domain"):
            assert domain.get("includeSubdomains", "false") == "false"
            exceptions.add(domain.text)
assert exceptions == {"127.0.0.1", "localhost"}
accessibility = ET.parse(root / "app/src/main/res/xml/phone_accessibility.xml").getroot()
assert int(accessibility.get(android + "notificationTimeout")) > 0
assert accessibility.get(android + "canRequestFilterKeyEvents") == "false"
assert "typeWindowContentChanged" not in accessibility.get(android + "accessibilityEventTypes")

tests = [
    ("app", "com.omarea.utils.MiniMonitorPosition", "MiniMonitorPositionTest"),
    ("app", "com.omarea.utils.AndroidPackageName", "AndroidPackageNameTest"),
    ("app", "com.omarea.scene_mode.TriggerExecutionPolicy", "TriggerExecutionPolicyTest"),
    ("app", "com.omarea.scene_mode.TaskExecutionPolicy", "TaskExecutionPolicyTest"),
    ("app", "com.omarea.vtools.FreezeCallerPolicy", "FreezeCallerPolicyTest"),
    ("app", "com.omarea.utils.CrashLogWriter", "CrashLogWriterTest"),
    ("common", "com.omarea.common.shared.DocumentCopy", "DocumentCopyTest"),
]
sources = []
for module, implementation, test in tests:
    package, name = implementation.rsplit(".", 1)
    directory = package.replace(".", "/")
    sources.extend([root / module / "src/main/java" / directory / (name + ".java"),
                    root / module / "src/test/java" / directory / (test + ".java")])
classes = root / "build/modern-test"
classes.mkdir(parents=True, exist_ok=True)
subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes), *map(str, sources)], check=True)
for _, implementation, test in tests:
    package = implementation.rsplit(".", 1)[0]
    subprocess.run(["java", "-ea", "-cp", str(classes), package + "." + test], check=True)
print("Manifest, network and accessibility configuration checks passed")
