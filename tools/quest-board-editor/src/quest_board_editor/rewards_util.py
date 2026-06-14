from __future__ import annotations

import copy
from typing import Callable, Dict, List, Optional

REWARD_KINDS = ("item", "reputation", "currency", "unlock", "learn_recipe")

GRANT_TO_QUEST_GIVER = "quest_giver_npc"
GRANT_TO_QUEST_BENEFICIARY = "quest_beneficiary_npc"
GRANT_TO_PLAYER = "player"


def normalize_reward(reward: dict) -> dict:
    kind = str(reward.get("kind", "item")).strip() or "item"
    if kind == "reputation":
        out: Dict[str, object] = {
            "kind": "reputation",
            "amount": max(1, int(reward.get("amount", 1))),
            "grantTo": str(reward.get("grantTo", GRANT_TO_QUEST_GIVER)).strip() or GRANT_TO_QUEST_GIVER,
        }
        role = reward.get("npcRoleId")
        if isinstance(role, str) and role.strip():
            out["npcRoleId"] = role.strip()
        return out
    if kind == "item":
        return {
            "kind": "item",
            "itemId": str(reward.get("itemId", "")).strip(),
            "count": max(1, int(reward.get("count", 1))),
            "grantTo": str(reward.get("grantTo", GRANT_TO_PLAYER)).strip() or GRANT_TO_PLAYER,
        }
    if kind == "learn_recipe":
        out = {
            "kind": "learn_recipe",
            "recipeItemId": str(reward.get("recipeItemId", reward.get("itemId", ""))).strip(),
            "grantTo": str(reward.get("grantTo", GRANT_TO_PLAYER)).strip() or GRANT_TO_PLAYER,
        }
        return out
    if kind in ("currency", "unlock"):
        out = {"kind": kind, "grantTo": str(reward.get("grantTo", GRANT_TO_PLAYER)).strip() or GRANT_TO_PLAYER}
        for key in ("amount", "count", "currencyId", "unlockId", "target"):
            if key in reward and reward[key] not in (None, ""):
                out[key] = reward[key]
        return out
    return copy.deepcopy(reward)


def reward_primary_label(kind: str) -> str:
    if kind == "reputation":
        return "amount"
    if kind == "learn_recipe":
        return "recipeItemId"
    return "itemId"


def reward_secondary_label(kind: str) -> str:
    if kind == "reputation":
        return "npcRoleId"
    if kind == "item":
        return "count"
    return ""


def reward_primary_value(reward: dict) -> str:
    kind = str(reward.get("kind", "item"))
    if kind == "reputation":
        return str(reward.get("amount", 1))
    if kind == "learn_recipe":
        return str(reward.get("recipeItemId", ""))
    return str(reward.get("itemId", ""))


def reward_secondary_value(reward: dict) -> str:
    kind = str(reward.get("kind", "item"))
    if kind == "reputation":
        return str(reward.get("npcRoleId", ""))
    if kind == "item":
        return str(reward.get("count", 1))
    return str(reward.get("count", reward.get("amount", "")))


def apply_primary_value(reward: dict, text: str) -> None:
    kind = str(reward.get("kind", "item"))
    if kind == "reputation":
        try:
            reward["amount"] = max(1, int(text))
        except ValueError:
            reward["amount"] = 1
        reward.pop("itemId", None)
        reward.pop("count", None)
        reward.pop("recipeItemId", None)
    elif kind == "learn_recipe":
        reward["recipeItemId"] = text
        reward.pop("itemId", None)
        reward.pop("count", None)
        reward.pop("amount", None)
    else:
        reward["itemId"] = text
        reward.pop("amount", None)
        reward.pop("recipeItemId", None)


def apply_secondary_value(reward: dict, text: str) -> None:
    kind = str(reward.get("kind", "item"))
    if kind == "reputation":
        if text.strip():
            reward["npcRoleId"] = text.strip()
        else:
            reward.pop("npcRoleId", None)
        reward.pop("count", None)
    elif kind == "item":
        try:
            reward["count"] = max(1, int(text))
        except ValueError:
            reward["count"] = 1
        reward.pop("amount", None)
        reward.pop("npcRoleId", None)


def apply_kind_change(reward: dict, new_kind: str) -> dict:
    grant_to = str(reward.get("grantTo", GRANT_TO_PLAYER))
    if new_kind == "reputation":
        return {
            "kind": "reputation",
            "amount": 5,
            "npcRoleId": "",
            "grantTo": grant_to if grant_to.endswith("_npc") else GRANT_TO_QUEST_GIVER,
        }
    if new_kind == "item":
        return {
            "kind": "item",
            "itemId": "Aetherhaven_Gold_Coin",
            "count": 8,
            "grantTo": GRANT_TO_PLAYER,
        }
    if new_kind == "learn_recipe":
        return {"kind": "learn_recipe", "recipeItemId": "", "grantTo": GRANT_TO_PLAYER}
    return {"kind": new_kind, "grantTo": grant_to}


def default_item_reward() -> dict:
    return normalize_reward(
        {"kind": "item", "itemId": "Aetherhaven_Gold_Coin", "count": 8, "grantTo": GRANT_TO_PLAYER}
    )


def default_reputation_reward(npc_role_id: str = "") -> dict:
    reward = {
        "kind": "reputation",
        "amount": 5,
        "grantTo": GRANT_TO_QUEST_GIVER,
    }
    if npc_role_id.strip():
        reward["npcRoleId"] = npc_role_id.strip()
    return normalize_reward(reward)
