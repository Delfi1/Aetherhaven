from __future__ import annotations

from pathlib import Path

from quest_board_editor.villager_catalog import discover_villager_ids, merged_villager_ids


def test_discover_villager_ids_from_repo():
    here = Path(__file__).resolve()
    villagers_dir = (
        here.parents[3]
        / "src"
        / "main"
        / "resources"
        / "Server"
        / "Aetherhaven"
        / "Villagers"
    )
    if not villagers_dir.is_dir():
        return
    ids = discover_villager_ids(villagers_dir)
    assert "Aetherhaven_Miner" in ids
    assert "Aetherhaven_Bard" in ids
    assert len(ids) >= 10


def test_merged_villager_ids_includes_both_sources():
    merged = merged_villager_ids(
        ["Aetherhaven_Custom_Old"],
        ["Aetherhaven_Miner", "Aetherhaven_Bard"],
    )
    assert merged == ["Aetherhaven_Miner", "Aetherhaven_Bard", "Aetherhaven_Custom_Old"]
