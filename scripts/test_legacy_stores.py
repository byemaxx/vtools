"""Exercise production SQL without reading or changing the device databases."""
import json
from pathlib import Path
import re
import sqlite3
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
stores = root / "app/src/main/java/com/omarea/store"


def source(name):
    return (stores / (name + ".java")).read_text(encoding="utf-8")


def database(text):
    db = sqlite3.connect(":memory:")
    for expression in re.findall(r'db\.execSQL\(\s*("create table.*?);', text, re.S):
        statement = "".join(json.loads(token) for token in re.findall(r'"(?:[^"\\]|\\.)*"', expression))
        db.execute(statement)
    return db


def query(text, method):
    body = text.split(" " + method + "(", 1)[1]
    return re.search(r'rawQuery\("([^"]+)"', body).group(1)


charge = source("ChargeSpeedStore")
db = database(charge)
assert db.execute(query(charge, "getSum")).fetchone() == (None,)
db.executemany("INSERT INTO charge_history(time,io,capacity,temperature) VALUES(?,?,?,?)", [
    (1000, 3600, 50, 31.25), (2000, 7200, 50, 32.75), (3000, 1800, 51, 33.5)])
assert db.execute(query(charge, "getSum")).fetchone() == (12600,)
assert db.execute(query(charge, "statistics")).fetchall() == [(50, 5400.0), (51, 1800.0)]
assert db.execute(query(charge, "getTemperature")).fetchall() == [(50, 32.75), (51, 33.5)]
assert db.execute(query(charge, "chargeTime")).fetchall() == [(50, 1000, 2000), (51, 3000, 3000)]
db.close()

fps = source("FpsWatchStore")
db = database(fps)
db.execute("INSERT INTO session VALUES(1,'test.app',1000,2000)")
db.executemany("INSERT INTO fps_history(session,fps,temperature) VALUES(?,?,?)", [(1, 59.5, 32.75), (1, 61.25, 33.5)])
assert db.execute(query(fps, "sessions")).fetchone() == (1, "test.app", 1000, 2000)
assert db.execute(query(fps, "sessionFpsData"), (1,)).fetchall() == [(59.5,), (61.25,)]
assert db.execute(query(fps, "sessionAvgFps"), (1,)).fetchone() == (60.375,)
assert db.execute(query(fps, "sessionTemperatureData"), (1,)).fetchall() == [(32.75,), (33.5,)]
db.commit()
db.execute("CREATE TRIGGER reject_delete BEFORE DELETE ON fps_history BEGIN SELECT RAISE(ABORT, 'test failure'); END")
delete_sql = re.findall(r'execSQL\("(delete[^"]+)"', fps)
try:
    with db:
        for statement in delete_sql[:2]:
            db.execute(statement)
    raise AssertionError("Deletion failure fixture did not fire")
except sqlite3.IntegrityError:
    pass
assert db.execute("SELECT count(*) FROM session").fetchone() == (1,)
assert db.execute("SELECT count(*) FROM fps_history").fetchone() == (2,)
db.close()

scene = source("SceneConfigStore")
current = database(scene)
required = set(re.findall(r'getColumnIndexOrThrow\("([^"]+)"', scene))
assert required <= {row[1] for row in current.execute("PRAGMA table_info(scene_config3)")}
old = sqlite3.connect(":memory:")
old.execute("CREATE TABLE scene_config3(id TEXT PRIMARY KEY, alone_light INT DEFAULT 0, light INT DEFAULT -1, dis_notice INT DEFAULT 0, dis_button INT DEFAULT 0, gps_on INT DEFAULT 0, freeze INT DEFAULT 0)")
old.execute("INSERT INTO scene_config3(id,freeze) VALUES('keep.app',1)")
for statement in re.findall(r'execSQL\("(alter table[^"]+)"', scene):
    old.execute(statement)
assert required <= {row[1] for row in old.execute("PRAGMA table_info(scene_config3)")}
assert old.execute("SELECT id,freeze,CAST(show_monitor AS INTEGER) FROM scene_config3").fetchone() == ("keep.app", 1, 0)
old.close()
current.close()

android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
assert not any(item.get(android + "name") == "android.permission.BIND_ACCESSIBILITY_SERVICE" for item in manifest.findall("uses-permission"))
service = next(item for item in manifest.find("application").findall("service") if item.get(android + "name") == ".AccessibilityScenceMode")
assert service.get(android + "permission") == "android.permission.BIND_ACCESSIBILITY_SERVICE"
print("Legacy SQL queries, decimal values, atomic rollback, migration preservation and service permission checks passed")
