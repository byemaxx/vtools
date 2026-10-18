"""Check saved ADB evidence from toggling the two optional settings on a device."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1] / "build"


def preference(snapshot, name):
    values = ET.parse(root / snapshot).getroot()
    item = values.find(f"boolean[@name='{name}']")
    return item is not None and item.attrib["value"] == "true"


assert preference("switch-preferences-auto-on.xml", "auto_accessibility_root")
assert preference("switch-preferences-on.xml", "leak_launcher_icon")
assert not preference("switch-preferences-final.xml", "auto_accessibility_root")
assert not preference("switch-preferences-final.xml", "leak_launcher_icon")
component = "leakcanary.internal.activity.LeakLauncherActivity"
for snapshot in ["switch-package-default.txt", "switch-package-off.txt"]:
    dump = (root / snapshot).read_text(encoding="utf-8-sig")
    disabled = dump.split("disabledComponents:", 1)[1].split("enabledComponents:", 1)[0]
    assert component in disabled, snapshot
dump = (root / "switch-package-on.txt").read_text(encoding="utf-8-sig")
assert component in dump.split("enabledComponents:", 1)[1].split("User 999:", 1)[0]
before = (root / "accessibility-privacy-check.txt").read_text(encoding="utf-8-sig").strip()
after = (root / "accessibility-after-switch-check.txt").read_text(encoding="utf-8-sig").strip()
assert before == after, "System accessibility list changed"
print("Both preferences toggle and persist; launcher visibility follows the setting; both end off; accessibility unchanged")
