from __future__ import annotations

import json
import os
from dataclasses import dataclass, field
from pathlib import Path


def _repo_root_from_here() -> Path:
    here = Path(__file__).resolve()
    return here.parents[4]


def default_quest_board_path() -> Path:
    override = os.environ.get("AETHERHAVEN_QUEST_BOARD")
    if override:
        return Path(override).resolve()
    root = _repo_root_from_here()
    p = root / "src" / "main" / "resources" / "Server" / "Aetherhaven" / "quest_board.json"
    if p.is_file():
        return p
    for anc in Path(__file__).resolve().parents:
        q = anc / "src" / "main" / "resources" / "Server" / "Aetherhaven" / "quest_board.json"
        if q.is_file():
            return q
    return p


def default_villagers_dir() -> Path:
    override = os.environ.get("AETHERHAVEN_VILLAGERS")
    if override:
        return Path(override).resolve()
    root = _repo_root_from_here()
    p = root / "src" / "main" / "resources" / "Server" / "Aetherhaven" / "Villagers"
    if p.is_dir():
        return p
    for anc in Path(__file__).resolve().parents:
        q = anc / "src" / "main" / "resources" / "Server" / "Aetherhaven" / "Villagers"
        if q.is_dir():
            return q
    return p


def default_lang_path() -> Path:
    override = os.environ.get("AETHERHAVEN_QUEST_BOARD_LANG")
    if override:
        return Path(override).resolve()
    root = _repo_root_from_here()
    p = (
        root
        / "src"
        / "main"
        / "resources"
        / "Server"
        / "Languages"
        / "en-US"
        / "aetherhaven_quest_board.lang"
    )
    if p.is_file():
        return p
    for anc in Path(__file__).resolve().parents:
        q = (
            anc
            / "src"
            / "main"
            / "resources"
            / "Server"
            / "Languages"
            / "en-US"
            / "aetherhaven_quest_board.lang"
        )
        if q.is_file():
            return q
    return p


def config_file_path() -> Path:
    return Path(__file__).resolve().parents[2] / "quest_board_editor_config.json"


@dataclass
class AppConfig:
    quest_board_path: str | None = None
    lang_path: str | None = None

    def resolved_quest_board_path(self) -> Path:
        if self.quest_board_path and self.quest_board_path.strip():
            return Path(self.quest_board_path).resolve()
        return default_quest_board_path()

    def resolved_lang_path(self) -> Path:
        if self.lang_path and self.lang_path.strip():
            return Path(self.lang_path).resolve()
        return default_lang_path()

    @staticmethod
    def load() -> "AppConfig":
        path = config_file_path()
        if not path.is_file():
            return AppConfig()
        try:
            raw = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError):
            return AppConfig()
        qb = raw.get("quest_board_path")
        lg = raw.get("lang_path")
        return AppConfig(
            quest_board_path=qb if isinstance(qb, str) and qb.strip() else None,
            lang_path=lg if isinstance(lg, str) and lg.strip() else None,
        )

    def save(self) -> None:
        path = config_file_path()
        data = {
            "quest_board_path": self.quest_board_path,
            "lang_path": self.lang_path,
        }
        path.write_text(
            json.dumps(data, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
        )
