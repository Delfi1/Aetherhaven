from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict


def load_quest_board(path: Path) -> Dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_quest_board(path: Path, data: Dict[str, Any]) -> None:
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    path.write_text(text, encoding="utf-8")
