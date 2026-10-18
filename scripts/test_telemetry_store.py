"""Exercise the actual telemetry schema and report queries with SQLite."""
from pathlib import Path
import re
import sqlite3

source = (Path(__file__).resolve().parents[1] / "app/src/main/java/com/omarea/store/TelemetryStore.kt").read_text(encoding="utf-8")
db = sqlite3.connect(":memory:")
db.execute("PRAGMA foreign_keys=ON")
for sql in re.findall(r'db\.execSQL\("(CREATE [^"]+)"\)', source):
    db.execute(sql)
assert {row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")} == {"sessions", "samples", "frames"}
db.execute("INSERT INTO sessions VALUES(1, 'game', 'app.example', 1000, 60)")
db.execute("INSERT INTO sessions VALUES(2, 'charge', 'Charging', 2000, 60)")
db.executemany("INSERT INTO frames VALUES(?,?,?)", [(1, 100, 16), (1, 110, 70), (1, 220, 120), (2, 100, 999)])
stats_sql = re.search(r'fun frameStats.*?"(SELECT [^"]+)"', source, re.S).group(1)
assert db.execute(stats_sql, ("50.0", "100.0", "1")).fetchone() == (3, 120.0, 2, 1)
peak_sql = re.search(r'fun framePeaks.*?"(SELECT [^"]+)"', source, re.S).group(1)
assert db.execute(peak_sql, ("1",)).fetchall() == [(100, 70.0), (200, 120.0)]
db.execute("INSERT INTO samples(session,elapsed,time,watts) VALUES(1,0,1000,NULL)")
assert db.execute("SELECT watts FROM samples WHERE session=1").fetchone() == (None,)
try:
    db.execute("INSERT INTO frames VALUES(999,0,16)")
    raise AssertionError("orphan frame accepted")
except sqlite3.IntegrityError:
    pass
db.commit()
try:
    with db:
        db.execute("INSERT INTO frames VALUES(1,999,16)")
        db.execute("INSERT INTO frames VALUES(999,999,16)")
except sqlite3.IntegrityError:
    pass
assert db.execute("SELECT count(*) FROM frames WHERE elapsed=999").fetchone()[0] == 0
assert db.execute("SELECT count(*) FROM frames WHERE session=1").fetchone()[0] == 3
assert db.execute(stats_sql, ("50", "100", "123")).fetchone() == (0, None, None, None)
latest_sql = re.search(r'fun latestSessionId.*?"(SELECT [^"]+)"', source, re.S).group(1)
assert db.execute(latest_sql, ("charge",)).fetchone() == (2,)
db.execute("INSERT INTO sessions VALUES(3, 'game', 'other.app', 3000, 60)")
assert db.execute(latest_sql, ("charge",)).fetchone() == (2,)
db.execute("INSERT INTO sessions VALUES(4, 'charge', 'Charging', 4000, 60)")
assert db.execute(latest_sql, ("charge",)).fetchone() == (4,)
assert db.execute("SELECT count(*) FROM samples WHERE session=4").fetchone() == (0,)
assert db.execute(latest_sql, ("missing",)).fetchone() is None
print("Telemetry SQLite schema / query / rollback checks passed")
