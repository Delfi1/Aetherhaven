from __future__ import annotations

import os
from pathlib import Path
from typing import List

from .config import default_villagers_dir


def discover_villager_ids(villagers_dir: Path | None = None) -> List[str]:
    """All villager role ids from `Server/Aetherhaven/Villagers/*.json` filenames."""
    root = villagers_dir or default_villagers_dir()
    if not root.is_dir():
        return []
    ids: List[str] = []
    for path in sorted(root.glob("*.json")):
        ids.append(path.stem)
    return ids


def merged_villager_ids(quest_board_ids: List[str], catalog_ids: List[str] | None = None) -> List[str]:
    """Union of catalog villagers and any ids already present in quest_board.json."""
    env = os.environ.get("AETHERHAVEN_VILLAGERS")
    catalog = catalog_ids if catalog_ids is not None else discover_villager_ids(
        Path(env).resolve() if env else None
    )
    seen: set[str] = set()
    out: List[str] = []
    for vid in catalog + sorted(quest_board_ids):
        if vid not in seen:
            seen.add(vid)
            out.append(vid)
    return out
