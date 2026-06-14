from __future__ import annotations

from quest_board_editor.lang_keys import (
    quest_description_lang_key,
    quest_title_lang_key,
    target_label_lang_key,
    villager_slug,
)


def test_villager_slugs():
    assert villager_slug("Aetherhaven_Miner") == "miner"
    assert villager_slug("Aetherhaven_Elder_Lyren") == "elder"
    assert villager_slug("Aetherhaven_Guild_Master") == "guild_master"


def test_generated_keys():
    assert quest_title_lang_key("Aetherhaven_Farmer", "wheat_bundle").endswith(
        "aetherhaven.questBoard.farmer.wheat_bundle.title"
    )
    assert quest_description_lang_key("Aetherhaven_Farmer", "wheat_bundle").endswith(
        ".description"
    )
    assert target_label_lang_key("undead").endswith("aetherhaven.questBoard.targets.undead")
