#!/usr/bin/env python3
"""
Find shop loot items that lack an explicit price in shop_prices.json.

Reads every defaults/shop_loot/*.json table, compares itemId values against the
prices map in defaults/shop_prices.json, and writes a sidecar JSON file listing
only the missing entries. Does not modify shop_prices.json.

Run from repo root:
  python scripts/find_missing_shop_prices.py
  python scripts/find_missing_shop_prices.py --out scripts/shop_prices_missing.json
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_LOOT_DIR = REPO_ROOT / "src" / "main" / "resources" / "defaults" / "shop_loot"
DEFAULT_PRICES = REPO_ROOT / "src" / "main" / "resources" / "defaults" / "shop_prices.json"
DEFAULT_OUT = REPO_ROOT / "scripts" / "shop_prices_missing.json"


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def collect_loot_items(loot_dir: Path) -> tuple[dict[str, list[str]], int]:
    """Return itemId -> loot table stems, and total loot entry count."""
    item_sources: dict[str, list[str]] = {}
    entry_count = 0

    for loot_file in sorted(loot_dir.glob("*.json")):
        table_id = loot_file.stem
        data = load_json(loot_file)
        for entry in data.get("entries", []):
            entry_count += 1
            item_id = entry.get("itemId")
            if not isinstance(item_id, str) or not item_id.strip():
                continue
            item_id = item_id.strip()
            item_sources.setdefault(item_id, [])
            if table_id not in item_sources[item_id]:
                item_sources[item_id].append(table_id)

    return item_sources, entry_count


def has_explicit_price(prices: dict[str, Any], item_id: str) -> bool:
    return item_id in prices


def build_missing_prices_doc(
    catalog: dict[str, Any],
    missing_ids: list[str],
    item_sources: dict[str, list[str]],
) -> dict[str, Any]:
    default_gold = int(catalog.get("defaultGoldPrice", 5))
    default_batch = int(catalog.get("defaultBatchSize", 1))
    catalog_revision = int(catalog.get("catalogRevision", 1))

    prices: dict[str, dict[str, int]] = {
        item_id: {"gold": default_gold, "batchSize": 1} for item_id in missing_ids
    }

    return {
        "catalogRevision": catalog_revision,
        "defaultGoldPrice": default_gold,
        "defaultBatchSize": default_batch,
        "_generatedBy": "scripts/find_missing_shop_prices.py",
        "_generatedNote": (
            "Items from shop_loot tables without an explicit entry in shop_prices.json. "
            f"Each entry starts at defaultGoldPrice ({default_gold}) with batchSize 1; "
            "adjust as needed, then copy entries into shop_prices.json."
        ),
        "_missingCount": len(missing_ids),
        "_lootTables": {
            item_id: item_sources[item_id] for item_id in missing_ids
        },
        "prices": prices,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--loot-dir", type=Path, default=DEFAULT_LOOT_DIR, help="Shop loot tables directory")
    parser.add_argument("--prices", type=Path, default=DEFAULT_PRICES, help="Existing shop_prices.json")
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT, help="Output JSON for missing prices")
    args = parser.parse_args()

    if not args.loot_dir.is_dir():
        print(f"Loot directory not found: {args.loot_dir}", file=sys.stderr)
        return 1
    if not args.prices.is_file():
        print(f"Shop prices file not found: {args.prices}", file=sys.stderr)
        return 1

    item_sources, entry_count = collect_loot_items(args.loot_dir)
    catalog = load_json(args.prices)
    prices = catalog.get("prices")
    if not isinstance(prices, dict):
        print(f"Invalid prices map in {args.prices}", file=sys.stderr)
        return 1

    missing_ids = sorted(
        item_id for item_id in item_sources if not has_explicit_price(prices, item_id)
    )
    configured_ids = sorted(
        item_id for item_id in item_sources if has_explicit_price(prices, item_id)
    )

    out_doc = build_missing_prices_doc(catalog, missing_ids, item_sources)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out_doc, indent=2) + "\n", encoding="utf-8")

    print(f"Loot tables scanned: {len(list(args.loot_dir.glob('*.json')))}")
    print(f"Loot entries scanned: {entry_count}")
    print(f"Unique loot item ids: {len(item_sources)}")
    print(f"With explicit shop price: {len(configured_ids)}")
    print(f"Missing shop price: {len(missing_ids)}")
    print(f"Wrote {args.out}")

    if missing_ids:
        print("\nMissing items:")
        for item_id in missing_ids:
            tables = ", ".join(item_sources[item_id])
            print(f"  {item_id}  ({tables})")
    else:
        print("\nAll shop loot items have explicit prices configured.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
