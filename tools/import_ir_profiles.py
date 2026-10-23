#!/usr/bin/env python3
"""Create the offline TV power-code catalog from the pinned Flipper IRDB revision."""

from __future__ import annotations

import json
import re
import concurrent.futures
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/assets/ir_profiles.json"
REVISION = "f7b15366521cc81ba11b341538f0097bddbb748b"
TREE_URL = f"https://api.github.com/repos/flipperdevices/IRDB/git/trees/{REVISION}?recursive=1"
RAW_BASE = f"https://raw.githubusercontent.com/flipperdevices/IRDB/{REVISION}/"

# Brand names correspond to the original LeTV system's first/second-page choices.
BRANDS = [
    ("创维", "Skyworth", 3),
    ("飞利浦", "Philips", 3),
    ("海信", "Hisense", 3),
    ("海尔", "Haier", 3),
    ("长虹", "Changhong", 3),
    ("三星", "Samsung", 5),
    ("康佳", "Konka", 3),
    ("夏普", "Sharp", 2),
    ("TCL", "TCL", 3),
    ("索尼", "Sony", 3),
    ("小米", "Xiaomi", 2),
    ("LG", "LG", 4),
    ("东芝", "Toshiba", 3),
    ("松下", "Panasonic", 3),
    ("日立", "Hitachi", 2),
]


def get(url: str) -> bytes:
    request = urllib.request.Request(
        url,
        headers={"User-Agent": "LetvOfflineRemoteKey/1.1 (offline catalog build)"},
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        return response.read()


def parse_power_signal(source: str) -> tuple[int, list[int]] | None:
    sections = re.split(r"(?m)^\s*#\s*$", source)
    for section in sections:
        values: dict[str, str] = {}
        for line in section.splitlines():
            if ":" not in line:
                continue
            key, value = line.split(":", 1)
            values[key.strip().lower()] = value.strip()
        name = re.sub(r"[^a-z0-9]", "", values.get("name", "").lower())
        if name not in {"power", "powertoggle", "poweronoff"}:
            continue
        if values.get("type", "").lower() != "raw":
            continue
        try:
            frequency = int(values["frequency"])
            pulses = [int(value) for value in values["data"].split()]
        except (KeyError, ValueError):
            continue
        if not 20_000 <= frequency <= 60_000:
            continue
        if not 12 <= len(pulses) <= 512:
            continue
        if any(value < 0 or value > 100_000 for value in pulses):
            continue
        return frequency, pulses
    return None


def main() -> None:
    tree = json.loads(get(TREE_URL))
    paths = [
        entry["path"]
        for entry in tree.get("tree", [])
        if entry.get("type") == "blob"
        and entry.get("path", "").startswith("database/categories/TVs/")
        and entry.get("path", "").endswith(".ir")
    ]

    catalog: list[dict[str, object]] = []
    for chinese_name, source_brand, limit in BRANDS:
        brand_prefix = f"database/categories/TVs/{source_brand}/"
        candidates = [path for path in paths if path.startswith(brand_prefix)]
        # Prefer explicit handset/model names over numeric cloud-code identifiers.
        candidates.sort(key=lambda path: (bool(re.search(r"_(kk|xm)_", path, re.I)), path.lower()))
        brand_profiles: list[dict[str, object]] = []
        # Fetch in bounded concurrent batches. Many directories have several
        # non-power profiles, so the next batch is only needed if necessary.
        for start in range(0, len(candidates), 16):
            batch = candidates[start : start + 16]
            with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
                futures = {pool.submit(get, RAW_BASE + path): path for path in batch}
                downloaded: dict[str, bytes] = {}
                for future in concurrent.futures.as_completed(futures):
                    path = futures[future]
                    try:
                        downloaded[path] = future.result()
                    except Exception as error:  # Community gaps do not block other profiles.
                        print(f"skip {path}: {error}")
            for path in batch:
                if path not in downloaded:
                    continue
                signal = parse_power_signal(downloaded[path].decode("utf-8", "replace"))
                if signal is None:
                    continue
                frequency, pulses = signal
                model_id = Path(path).parent.name
                brand_profiles.append(
                    {
                        "brand": chinese_name,
                        "model": model_id,
                        "frequency": frequency,
                        "pulses": pulses,
                        "source": f"{RAW_BASE}{path}",
                    }
                )
                if len(brand_profiles) >= limit:
                    break
            if len(brand_profiles) >= limit:
                break
        catalog.extend(brand_profiles)
        found = len(brand_profiles)
        print(f"{chinese_name}: {found} raw power profiles")

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(
        json.dumps(
            {
                "source": "flipperdevices/IRDB",
                "revision": REVISION,
                "profiles": catalog,
            },
            ensure_ascii=False,
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )
    print(f"wrote {len(catalog)} profiles to {OUT}")


if __name__ == "__main__":
    main()
