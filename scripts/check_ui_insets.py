"""Check saved device layout evidence; does not access or change the phone."""
from pathlib import Path
import re
import struct
import xml.etree.ElementTree as ET

folder = Path(__file__).resolve().parents[1] / "build/ui-audit"
window = (folder / "window-after.txt").read_text(encoding="utf-8-sig")
status = int(re.search(r"type=statusBars,.*?insetsSize=Insets\{left=0, top=(\d+)", window, re.S).group(1))
navigation = int(re.search(r"type=navigationBars,.*?insetsSize=Insets\{left=0, top=0, right=0, bottom=(\d+)", window, re.S).group(1))

def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds", ""))))

for page in ("charge", "settings", "fps", "telemetry"):
    nodes = list(ET.parse(folder / f"{page}-after.xml").iter("node"))
    toolbar = next(n for n in nodes if n.get("resource-id", "").endswith("/toolbar"))
    assert bounds(toolbar)[1] == status, (page, bounds(toolbar), status)
    with (folder / f"{page}-after.png").open("rb") as image:
        header = image.read(24)
    height = struct.unpack(">II", header[16:24])[1]
    for node in nodes:
        box = bounds(node)
        if node.get("package") == "com.omarea.vtools" and node.get("clickable") == "true" and len(box) == 4 and box[3] > box[1]:
            assert box[1] >= status and box[3] <= height - navigation, (page, node.get("resource-id"), box)
    print(f"{page}: toolbar top {status}px; visible controls clear of system bars")

old = ET.parse(folder / "telemetry-before.xml")
old_toolbar = next(n for n in old.iter("node") if n.get("resource-id", "").endswith("/toolbar"))
assert bounds(old_toolbar)[1] < status
assert any(n.get("resource-id", "").endswith("/telemetry_content") for n in nodes)
keyboard_window = (folder / "window-keyboard.txt").read_text(encoding="utf-8-sig")
keyboard_top = int(re.search(r"InputMethod, frame=.*?touchableRegion=SkRegion\(\(0,(\d+)", keyboard_window).group(1))
editor = ET.parse(folder / "editor-keyboard.xml")
for suffix in ("/command_script", "/btn_confirm"):
    node = next(n for n in editor.iter("node") if n.get("resource-id", "").endswith(suffix))
    assert bounds(node)[3] <= keyboard_top, (suffix, bounds(node), keyboard_top)
print("Editor: input and save button clear of keyboard; prior overlap regression reproduced")
