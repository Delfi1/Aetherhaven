from __future__ import annotations

import copy
from dataclasses import dataclass
from typing import Any, Dict, List, Optional, Tuple

from .lang_keys import (
    json_key_to_lang_key,
    quest_description_lang_key,
    quest_title_lang_key,
    target_label_lang_key,
)
from .rewards_util import GRANT_TO_QUEST_BENEFICIARY, GRANT_TO_QUEST_GIVER

QUEST_TYPES = ("fetch", "hunt", "raid")
ENTRIES_KEY = {
    "fetch": "fetchEntries",
    "hunt": "huntEntries",
    "raid": "raidEntries",
}


@dataclass
class QuestRef:
    villager_id: str
    quest_type: str
    index: int
    entry: dict

    @property
    def quest_id(self) -> str:
        return str(self.entry.get("id", ""))

    @property
    def rank(self) -> str:
        return str(self.entry.get("rank", ""))


@dataclass
class QuestBoardDocument:
    data: dict

    @property
    def ranks(self) -> List[str]:
        raw = self.data.get("ranks") or []
        out: List[str] = []
        for r in raw:
            if isinstance(r, dict) and r.get("id"):
                out.append(str(r["id"]))
        return out

    @property
    def villagers(self) -> Dict[str, dict]:
        v = self.data.get("villagers")
        return v if isinstance(v, dict) else {}

    def villager_ids(self) -> List[str]:
        return sorted(self.villagers.keys())

    def flatten(self) -> List[QuestRef]:
        refs: List[QuestRef] = []
        for villager_id, villager in self.villagers.items():
            if not isinstance(villager, dict):
                continue
            for quest_type in QUEST_TYPES:
                key = ENTRIES_KEY[quest_type]
                entries = villager.get(key)
                if not isinstance(entries, list):
                    continue
                for i, entry in enumerate(entries):
                    if isinstance(entry, dict):
                        refs.append(
                            QuestRef(
                                villager_id=villager_id,
                                quest_type=quest_type,
                                index=i,
                                entry=entry,
                            )
                        )
        return refs

    def ensure_villager(self, villager_id: str) -> dict:
        villagers = self.data.setdefault("villagers", {})
        if not isinstance(villagers, dict):
            villagers = {}
            self.data["villagers"] = villagers
        entry = villagers.get(villager_id)
        if not isinstance(entry, dict):
            entry = {}
            villagers[villager_id] = entry
        return entry

    def entries_list(self, villager_id: str, quest_type: str) -> List[dict]:
        villager = self.ensure_villager(villager_id)
        key = ENTRIES_KEY[quest_type]
        entries = villager.get(key)
        if not isinstance(entries, list):
            entries = []
            villager[key] = entries
        return entries

    def insert_quest(
        self, villager_id: str, quest_type: str, entry: dict, index: Optional[int] = None
    ) -> QuestRef:
        entries = self.entries_list(villager_id, quest_type)
        if index is None or index < 0 or index > len(entries):
            entries.append(entry)
            idx = len(entries) - 1
        else:
            entries.insert(index, entry)
            idx = index
        return QuestRef(villager_id, quest_type, idx, entry)

    def remove_quest(self, ref: QuestRef) -> None:
        entries = self.entries_list(ref.villager_id, ref.quest_type)
        if 0 <= ref.index < len(entries):
            entries.pop(ref.index)

    def move_quest(
        self, ref: QuestRef, new_villager_id: str, new_quest_type: str
    ) -> QuestRef:
        if ref.villager_id == new_villager_id and ref.quest_type == new_quest_type:
            return ref
        entry = copy.deepcopy(ref.entry)
        self.remove_quest(ref)
        return self.insert_quest(new_villager_id, new_quest_type, entry)

    def rank_index(self, rank_id: str) -> int:
        try:
            return self.ranks.index(rank_id)
        except ValueError:
            return -1


def villager_short_label(villager_id: str) -> str:
    if villager_id.startswith("Aetherhaven_"):
        return villager_id[len("Aetherhaven_") :]
    return villager_id


def resolve_title(lang_getter, ref: QuestRef) -> str:
    key = ref.entry.get("titleLangKey")
    if not isinstance(key, str) or not key:
        return ref.quest_id
    return lang_getter(json_key_to_lang_key(key), ref.quest_id)


@dataclass
class QuestFilter:
    villager_id: Optional[str] = None
    quest_type: Optional[str] = None
    rank: Optional[str] = None
    search: str = ""

    def matches(self, ref: QuestRef, lang_getter) -> bool:
        if self.villager_id and ref.villager_id != self.villager_id:
            return False
        if self.quest_type and ref.quest_type != self.quest_type:
            return False
        if self.rank and ref.rank != self.rank:
            return False
        if self.search.strip():
            q = self.search.strip().lower()
            title = resolve_title(lang_getter, ref).lower()
            if q not in ref.quest_id.lower() and q not in title:
                return False
        return True


def filter_quests(
    refs: List[QuestRef], filt: QuestFilter, lang_getter
) -> List[QuestRef]:
    return [r for r in refs if filt.matches(r, lang_getter)]


def make_fetch_template(villager_id: str, quest_id: str = "new_quest") -> dict:
    return {
        "id": quest_id,
        "rank": "E",
        "minRank": "E",
        "maxRank": "C",
        "weight": 10,
        "daysLimit": 3,
        "titleLangKey": quest_title_lang_key(villager_id, quest_id),
        "descriptionLangKey": quest_description_lang_key(villager_id, quest_id),
        "itemSets": [{"weight": 1, "items": [{"itemId": "Rock_Stone", "count": 1}]}],
        "rewards": [
            {"kind": "item", "itemId": "Aetherhaven_Gold_Coin", "count": 8, "grantTo": "player"}
        ],
    }


def make_hunt_template(villager_id: str, quest_id: str = "new_quest") -> dict:
    return {
        "id": quest_id,
        "rank": "E",
        "minRank": "E",
        "maxRank": "C",
        "weight": 10,
        "daysLimit": 4,
        "titleLangKey": quest_title_lang_key(villager_id, quest_id),
        "descriptionLangKey": quest_description_lang_key(villager_id, quest_id),
        "targetLabelLangKey": target_label_lang_key("vermin"),
        "killSets": [{"weight": 1, "killCount": 5, "entityTagsAny": ["Vermin"]}],
        "rewards": [
            {"kind": "item", "itemId": "Aetherhaven_Gold_Coin", "count": 8, "grantTo": "player"}
        ],
    }


def make_raid_template(villager_id: str, quest_id: str = "new_quest") -> dict:
    return {
        "id": quest_id,
        "rank": "D",
        "minRank": "D",
        "maxRank": "B",
        "weight": 8,
        "daysLimit": 5,
        "titleLangKey": quest_title_lang_key(villager_id, quest_id),
        "descriptionLangKey": quest_description_lang_key(villager_id, quest_id),
        "targetLabelLangKey": target_label_lang_key("vermin"),
        "raidSets": [
            {
                "weight": 1,
                "mobCountsByRank": {"D": 6, "C": 8, "B": 10},
                "mobPool": [{"roleId": "Goblin_Scrapper", "weight": 3}],
            }
        ],
        "rewards": [
            {"kind": "item", "itemId": "Aetherhaven_Gold_Coin", "count": 18, "grantTo": "player"}
        ],
    }


def make_template(villager_id: str, quest_type: str, quest_id: str = "new_quest") -> dict:
    if quest_type == "fetch":
        return make_fetch_template(villager_id, quest_id)
    if quest_type == "hunt":
        return make_hunt_template(villager_id, quest_id)
    if quest_type == "raid":
        return make_raid_template(villager_id, quest_id)
    raise ValueError(f"Unknown quest type: {quest_type}")


def migrate_entry_type(entry: dict, new_type: str, villager_id: str) -> dict:
    base = {
        "id": entry.get("id", "new_quest"),
        "rank": entry.get("rank", "E"),
        "minRank": entry.get("minRank", "E"),
        "maxRank": entry.get("maxRank", "C"),
        "weight": entry.get("weight", 10),
        "daysLimit": entry.get("daysLimit", 3),
        "titleLangKey": entry.get("titleLangKey")
        or quest_title_lang_key(villager_id, str(entry.get("id", "new_quest"))),
        "descriptionLangKey": entry.get("descriptionLangKey")
        or quest_description_lang_key(villager_id, str(entry.get("id", "new_quest"))),
        "rewards": copy.deepcopy(entry.get("rewards") or []),
    }
    if entry.get("rankXpReward"):
        base["rankXpReward"] = entry["rankXpReward"]
    tmpl = make_template(villager_id, new_type, str(base["id"]))
    for k, v in tmpl.items():
        if k not in base or base[k] in (None, [], {}):
            base[k] = v
    if new_type in ("hunt", "raid") and "targetLabelLangKey" not in base:
        base["targetLabelLangKey"] = tmpl.get("targetLabelLangKey")
    return base


def validate_document(doc: QuestBoardDocument, lang_getter) -> List[str]:
    errors: List[str] = []
    ranks = set(doc.ranks)
    seen_local: set[Tuple[str, str, str]] = set()
    seen_global: dict[str, List[str]] = {}

    for ref in doc.flatten():
        e = ref.entry
        qid = str(e.get("id", "")).strip()
        if not qid:
            errors.append(f"[{ref.villager_id}/{ref.quest_type}] missing id")
            continue

        local_key = (ref.villager_id, ref.quest_type, qid)
        if local_key in seen_local:
            errors.append(f"Duplicate id '{qid}' for {ref.villager_id} {ref.quest_type}")
        seen_local.add(local_key)
        seen_global.setdefault(qid, []).append(f"{ref.villager_id}/{ref.quest_type}")

        for field in ("rank", "minRank", "maxRank"):
            val = e.get(field)
            if not val:
                errors.append(f"[{qid}] missing {field}")
            elif val not in ranks:
                errors.append(f"[{qid}] unknown {field}: {val}")

        rank = e.get("rank")
        min_r = e.get("minRank")
        max_r = e.get("maxRank")
        if rank in ranks and min_r in ranks and max_r in ranks:
            ri, mini, maxi = doc.rank_index(str(rank)), doc.rank_index(str(min_r)), doc.rank_index(str(max_r))
            if mini > ri or ri > maxi:
                errors.append(f"[{qid}] rank {rank} not within [{min_r}, {max_r}]")

        weight = e.get("weight", 0)
        if not isinstance(weight, int) or weight < 1:
            errors.append(f"[{qid}] weight must be >= 1")

        days = e.get("daysLimit", 0)
        if not isinstance(days, int) or days < 1:
            errors.append(f"[{qid}] daysLimit must be >= 1")

        for lang_field in ("titleLangKey", "descriptionLangKey"):
            lk = e.get(lang_field)
            if not isinstance(lk, str) or not lk:
                errors.append(f"[{qid}] missing {lang_field}")
            else:
                lang_key = json_key_to_lang_key(lk)
                if not lang_getter(lang_key, "").strip():
                    errors.append(f"[{qid}] empty lang text for {lang_key}")

        if ref.quest_type == "fetch":
            sets = e.get("itemSets") or []
            if not sets:
                errors.append(f"[{qid}] fetch needs at least one itemSet")
            for s in sets:
                items = (s or {}).get("items") or []
                if not items:
                    errors.append(f"[{qid}] itemSet has no items")
        elif ref.quest_type == "hunt":
            sets = e.get("killSets") or []
            if not sets:
                errors.append(f"[{qid}] hunt needs at least one killSet")
            for s in sets:
                if (s or {}).get("killCount", 0) < 1:
                    errors.append(f"[{qid}] killSet killCount must be >= 1")
        elif ref.quest_type == "raid":
            sets = e.get("raidSets") or []
            if not sets:
                errors.append(f"[{qid}] raid needs at least one raidSet")
            for s in sets:
                if not (s or {}).get("mobPool"):
                    errors.append(f"[{qid}] raidSet needs mobPool")
                if not (s or {}).get("mobCountsByRank"):
                    errors.append(f"[{qid}] raidSet needs mobCountsByRank")

        for i, reward in enumerate(e.get("rewards") or []):
            if not isinstance(reward, dict):
                errors.append(f"[{qid}] reward #{i + 1} is not an object")
                continue
            kind = str(reward.get("kind", "")).strip()
            if kind == "item":
                if not str(reward.get("itemId", "")).strip():
                    errors.append(f"[{qid}] item reward #{i + 1} missing itemId")
            elif kind == "reputation":
                amount = reward.get("amount", 0)
                if not isinstance(amount, int) or amount < 1:
                    errors.append(f"[{qid}] reputation reward #{i + 1} amount must be >= 1")
                grant_to = str(reward.get("grantTo", "")).strip()
                if grant_to not in (GRANT_TO_QUEST_GIVER, GRANT_TO_QUEST_BENEFICIARY):
                    errors.append(
                        f"[{qid}] reputation reward #{i + 1} grantTo must be "
                        f"{GRANT_TO_QUEST_GIVER} or {GRANT_TO_QUEST_BENEFICIARY}"
                    )
            elif kind == "learn_recipe":
                if not str(reward.get("recipeItemId", "")).strip():
                    errors.append(f"[{qid}] learn_recipe reward #{i + 1} missing recipeItemId")

    for qid, locs in seen_global.items():
        if len(locs) > 1:
            errors.append(f"Global duplicate id '{qid}' in: {', '.join(locs)}")

    return errors


def regenerate_entry_lang_keys(
    entry: dict,
    villager_id: str,
    quest_id: str,
    quest_type: str,
    *,
    title_text: str,
    desc_text: str,
    target_text: str = "",
) -> Tuple[Dict[str, str], List[str]]:
    """Rewrite quest lang keys from villager + quest id; keep current UI text.

    Returns pending texts keyed by json lang key, and stale json keys to remove.
    """
    old_title = str(entry.get("titleLangKey", ""))
    old_desc = str(entry.get("descriptionLangKey", ""))

    entry["titleLangKey"] = quest_title_lang_key(villager_id, quest_id)
    entry["descriptionLangKey"] = quest_description_lang_key(villager_id, quest_id)
    if quest_type in ("hunt", "raid") and not entry.get("targetLabelLangKey"):
        entry["targetLabelLangKey"] = target_label_lang_key("vermin")

    pending: Dict[str, str] = {}
    title_key = str(entry.get("titleLangKey", ""))
    desc_key = str(entry.get("descriptionLangKey", ""))
    if title_key:
        pending[title_key] = title_text
    if desc_key:
        pending[desc_key] = desc_text
    target_key = str(entry.get("targetLabelLangKey", ""))
    if target_key:
        pending[target_key] = target_text

    new_keys = set(pending.keys())
    stale = [k for k in (old_title, old_desc) if k and k not in new_keys]
    return pending, stale


def sync_lang_from_quests(doc: QuestBoardDocument, lang_doc, texts: Dict[str, str]) -> None:
    """Upsert lang entries from edited text map keyed by json lang key."""
    for json_key, value in texts.items():
        lang_doc.set(json_key_to_lang_key(json_key), value)

    for ref in doc.flatten():
        e = ref.entry
        for field in ("titleLangKey", "descriptionLangKey", "targetLabelLangKey"):
            lk = e.get(field)
            if isinstance(lk, str) and lk and lk not in texts:
                lang_key = json_key_to_lang_key(lk)
                if not lang_doc.get(lang_key, "").strip():
                    lang_doc.set(lang_key, "")
