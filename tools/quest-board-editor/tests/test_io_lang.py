from __future__ import annotations

from quest_board_editor.io_lang import LangDocument, parse_lang, serialize_lang
from quest_board_editor.lang_keys import json_key_to_lang_key, lang_key_to_json_key


def test_parse_and_serialize():
    text = "# comment\n\naetherhaven.questBoard.miner.stone_haul.title=Stone\n"
    doc = parse_lang(text)
    assert doc.get("aetherhaven.questBoard.miner.stone_haul.title") == "Stone"
    out = serialize_lang(doc)
    assert "Stone" in out
    assert "# comment" in out


def test_upsert_new_key():
    doc = LangDocument(lines=[])
    doc.rebuild_index()
    doc.set("aetherhaven.questBoard.test.title", "Hello")
    assert doc.get("aetherhaven.questBoard.test.title") == "Hello"


def test_prefix_mapping():
    jk = lang_key_to_json_key("aetherhaven.questBoard.miner.stone_haul.title")
    assert jk == "aetherhaven_quest_board.aetherhaven.questBoard.miner.stone_haul.title"
    assert json_key_to_lang_key(jk) == "aetherhaven.questBoard.miner.stone_haul.title"
