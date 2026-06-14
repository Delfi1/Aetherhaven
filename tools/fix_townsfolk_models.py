#!/usr/bin/env python3
"""Repair Server/Models/Townsfolk/*.json exports (duplicate hair, body mesh, ears, stubble)."""
from __future__ import annotations

import json
from pathlib import Path

PLAYER_BODY = "Characters/Player.blockymodel"
GENERIC_HAIR = {
    "Characters/Haircuts/GenericShort.blockymodel",
    "Characters/Haircuts/GenericMedium.blockymodel",
    "Characters/Haircuts/GenericLong.blockymodel",
}
EAR_NPC_GREYSCALE = "Characters/Body_Attachments/Ears/Ears1_Textures/Ears1_Greyscale_Texture.png"
LEGACY_EAR = "Characters/Body_Attachments/Ears/Ears.png"
FACE_STUBBLE = "Characters/Body_Attachments/Faces/Faces_Detached_Textures/Face_Stubble.png"
FACE_NEUTRAL = "Characters/Body_Attachments/Faces/Faces_Detached_Textures/Face.png"
BEARD_PREFIX = "Characters/Body_Attachments/Beards/"

TOWNSFOLK_MODELS = Path(__file__).resolve().parents[1] / "src/main/resources/Server/Models/Townsfolk"
VILLAGER_MODELS = Path(__file__).resolve().parents[1] / "src/main/resources/Server/Models/Villager"


def is_styled_hair(att: dict) -> bool:
    model = att.get("Model", "")
    return model.startswith("Characters/Haircuts/") and model not in GENERIC_HAIR


def fix_model(data: dict) -> bool:
    changed = False
    attachments: list[dict] = list(data.get("DefaultAttachments") or [])

    if attachments and attachments[0].get("Model") == PLAYER_BODY:
        body = attachments.pop(0)
        if data.get("Model") != body.get("Model"):
            data["Model"] = body["Model"]
            changed = True
        if data.get("Texture") != body.get("Texture"):
            data["Texture"] = body["Texture"]
            changed = True
        for key in ("GradientSet", "GradientId"):
            if body.get(key) and data.get(key) != body[key]:
                data[key] = body[key]
                changed = True
        changed = True

    has_beard = any((a.get("Model") or "").startswith(BEARD_PREFIX) for a in attachments)
    has_styled = any(is_styled_hair(a) for a in attachments)

    new_attachments: list[dict] = []
    for att in attachments:
        model = att.get("Model", "")
        texture = att.get("Texture", "")

        if has_styled and model in GENERIC_HAIR:
            changed = True
            continue

        if texture == LEGACY_EAR:
            att = dict(att)
            att["Texture"] = EAR_NPC_GREYSCALE
            changed = True

        if has_beard and texture == FACE_STUBBLE:
            att = dict(att)
            att["Texture"] = FACE_NEUTRAL
            changed = True

        new_attachments.append(att)

    if new_attachments != attachments:
        data["DefaultAttachments"] = new_attachments
        changed = True

    return changed


def fix_directory(directory: Path) -> int:
    if not directory.is_dir():
        print(f"Missing directory: {directory}")
        return 0
    fixed = 0
    for path in sorted(directory.glob("*.json")):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        if fix_model(data):
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2)
                f.write("\n")
            print(f"fixed {directory.name}/{path.name}")
            fixed += 1
    return fixed


def main() -> int:
    total = fix_directory(TOWNSFOLK_MODELS) + fix_directory(VILLAGER_MODELS)
    print(f"Done: {total} files updated")
    return 0 if total >= 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
