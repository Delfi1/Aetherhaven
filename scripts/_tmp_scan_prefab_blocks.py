#!/usr/bin/env python3
"""Temporary: scan plot building prefabs for block ids worth reviewing for skip rules."""

from __future__ import annotations

import json
import re
from collections import Counter, defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
BUILDINGS = REPO / "src/main/resources/Server/Aetherhaven/Buildings"
PREFABS = REPO / "src/main/resources/Server/Prefabs"
MATERIALS = BUILDINGS / "PrefabMaterials"
CONVERSIONS = Path(__file__).resolve().parent / "prefab_material_conversions.txt"

_STATE_RE = re.compile(r"_State_Definitions_.*$")
_HOLLOW = "_Hollow"


def load_skip_exact() -> set[str]:
    exact = {"Empty"}
    if CONVERSIONS.is_file():
        for line in CONVERSIONS.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, val = line.partition("=")
            key = key.strip()
            if key.lower().startswith("pattern:"):
                continue
            if val.strip().lower() == "skip":
                exact.add(key)
    return exact


def normalize(name: str) -> str | None:
    n = name.strip()
    if not n or n == "Empty":
        return None
    if n.startswith("*"):
        n = n[1:]
    n = _STATE_RE.sub("", n)
    if n.endswith(_HOLLOW):
        n = n[: -len(_HOLLOW)] or n
    return n or None


def scan_prefab(path: Path) -> tuple[Counter[str], Counter[str]]:
    """Returns (normalized counts, raw counts). Anchor cells only (filler==0)."""
    data = json.loads(path.read_text(encoding="utf-8"))
    norm: Counter[str] = Counter()
    raw: Counter[str] = Counter()
    for b in data.get("blocks") or []:
        if not isinstance(b, dict):
            continue
        if b.get("filler", 0) != 0:
            continue
        name = b.get("name")
        if not isinstance(name, str):
            continue
        raw[name] += 1
        nid = normalize(name)
        if nid:
            norm[nid] += 1
    return norm, raw


def main() -> None:
    already_skip = load_skip_exact()

    building_files = sorted(BUILDINGS.glob("plot_*.json"))
    missing_materials = []
    hytiny = []
    all_new = []

    for bf in building_files:
        if bf.parent.name == "PrefabMaterials":
            continue
        cid = bf.stem
        bdef = json.loads(bf.read_text(encoding="utf-8"))
        prefab_name = bdef.get("prefabPath", "")
        if not prefab_name:
            continue
        prefab_path = PREFABS / prefab_name
        if not prefab_path.is_file():
            continue
        has_mat = (MATERIALS / f"{cid}.json").is_file()
        is_hytiny = "hytiny" in cid.lower() or "hytinys" in prefab_name.lower()
        entry = (cid, prefab_name, prefab_path, has_mat, is_hytiny)
        if not has_mat:
            missing_materials.append(entry)
        if is_hytiny:
            hytiny.append(entry)
        if not has_mat or is_hytiny:
            all_new.append(entry)

    # de-dupe by prefab path
    seen: set[str] = set()
    targets = []
    for e in all_new:
        if e[1] not in seen:
            seen.add(e[1])
            targets.append(e)

    per_prefab: dict[str, Counter[str]] = {}
    global_counts: Counter[str] = Counter()
    prefab_for_id: dict[str, set[str]] = defaultdict(set)

    for cid, prefab_name, prefab_path, has_mat, is_hytiny in targets:
        norm, _ = scan_prefab(prefab_path)
        per_prefab[prefab_name] = norm
        for bid, c in norm.items():
            global_counts[bid] += c
            prefab_for_id[bid].add(prefab_name)

    def suggest_reason(bid: str) -> str:
        if bid in already_skip:
            return "already skip in conversions"
        if bid == "Editor_Empty":
            return "plot-creator air marker"
        if bid.startswith("Aetherhaven_"):
            return "mod mechanic block"
        if bid.startswith("Editor_"):
            return "editor-only block"
        if bid.startswith("Prototype_"):
            return "prototype / not craftable?"
        if bid.startswith("Bench_") and bid not in already_skip:
            return "functional bench (may be provided by building)"
        return ""

    print("=== Prefabs scanned (missing PrefabMaterials and/or hytiny) ===")
    for cid, prefab_name, _, has_mat, is_hytiny in targets:
        flags = []
        if not has_mat:
            flags.append("NO_MATERIALS")
        if is_hytiny:
            flags.append("HYTINY")
        print(f"  {cid} -> {prefab_name} [{', '.join(flags)}]")

    print("\n=== Suggested skip candidates (normalized ids) ===")
    candidates = []
    for bid, count in global_counts.most_common():
        reason = suggest_reason(bid)
        if reason:
            candidates.append((bid, count, sorted(prefab_for_id[bid]), reason))

    for bid, count, prefabs, reason in candidates:
        print(f"  {bid}\t{count}\t{reason}\t{', '.join(prefabs)}")

    print("\n=== Other Aetherhaven_* ids in scanned prefabs (not already skip) ===")
    for bid, count in global_counts.most_common():
        if bid.startswith("Aetherhaven_") and bid not in already_skip and not suggest_reason(bid):
            print(f"  {bid}\t{count}\t{', '.join(sorted(prefab_for_id[bid]))}")

    print("\n=== Per-prefab mechanic / special blocks ===")
    special_prefixes = ("Aetherhaven_", "Editor_", "Bench_")
    for cid, prefab_name, _, _, _ in targets:
        norm = per_prefab[prefab_name]
        specials = [(b, c) for b, c in norm.items() if b.startswith(special_prefixes) or b == "Editor_Empty"]
        if specials:
            print(f"\n{prefab_name} ({cid}):")
            for b, c in sorted(specials, key=lambda x: (-x[1], x[0])):
                tag = suggest_reason(b) or "?"
                print(f"    {b}: {c}  [{tag}]")


if __name__ == "__main__":
    main()
