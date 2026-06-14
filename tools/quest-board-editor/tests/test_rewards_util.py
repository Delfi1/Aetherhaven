from __future__ import annotations

from quest_board_editor.rewards_util import (
    GRANT_TO_QUEST_GIVER,
    apply_kind_change,
    default_item_reward,
    default_reputation_reward,
    normalize_reward,
)


def test_normalize_item_reward():
    raw = {"kind": "item", "itemId": "Rock_Stone", "count": 3, "grantTo": "player"}
    out = normalize_reward(raw)
    assert out == raw
    assert "amount" not in out
    assert "npcRoleId" not in out


def test_normalize_reputation_reward():
    raw = {
        "kind": "reputation",
        "amount": 10,
        "npcRoleId": "Aetherhaven_Miner",
        "grantTo": "quest_giver_npc",
    }
    out = normalize_reward(raw)
    assert out["kind"] == "reputation"
    assert out["amount"] == 10
    assert out["npcRoleId"] == "Aetherhaven_Miner"
    assert out["grantTo"] == GRANT_TO_QUEST_GIVER
    assert "itemId" not in out
    assert "count" not in out


def test_normalize_reputation_strips_item_fields():
    raw = {
        "kind": "reputation",
        "amount": 5,
        "itemId": "should_be_removed",
        "count": 99,
        "grantTo": "quest_giver_npc",
    }
    out = normalize_reward(raw)
    assert "itemId" not in out
    assert "count" not in out


def test_apply_kind_change_to_reputation():
    item = default_item_reward()
    rep = apply_kind_change(item, "reputation")
    assert rep["kind"] == "reputation"
    assert rep["amount"] == 5
    assert rep["grantTo"] == GRANT_TO_QUEST_GIVER


def test_default_reputation_reward_uses_npc_role():
    out = default_reputation_reward("Aetherhaven_Bard")
    assert out["npcRoleId"] == "Aetherhaven_Bard"
