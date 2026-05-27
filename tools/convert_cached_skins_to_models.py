#!/usr/bin/env python3
"""
Convert Hytale CachedPlayerSkins JSON (main-menu avatar designer) into Server/Models/*.json
(Parent: Player + DefaultAttachments), matching Aetherhaven PlayerSkinModelExporter / CosmeticRegistry rules.

Usage:
  python tools/convert_cached_skins_to_models.py
  python tools/convert_cached_skins_to_models.py --input "%AppData%\\Hytale\\UserData\\CachedPlayerSkins" \\
      --output "path/to/mod/Server/Models" --manifest tools/skin_names.csv

Optional manifest CSV (no header): uuid_or_filename_stem,OutputModelName
  bfb47258-84ff-4d82-a75c-0d97731750b7,Villager_Merchant
"""
from __future__ import annotations

import argparse
import csv
import json
import os
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

SKIN_GRADIENT_SET_ID = "Skin"
HAIR_GRADIENT_SET_ID = "Hair"
PARENT_PLAYER = "Player"

DEFAULT_ASSETS = Path(r"c:\Users\gchou\OneDrive\Documents\Hytale-Modding\HytaleSourceCode\Assets")
DEFAULT_INPUT = Path(os.environ.get("APPDATA", "")) / "Hytale" / "UserData" / "CachedPlayerSkins"
DEFAULT_OUTPUT = (
    Path(os.environ.get("APPDATA", ""))
    / "Hytale"
    / "UserData"
    / "Mods"
    / "Hexvane.AetherhavenTestPack"
    / "Server"
    / "Models"
)

SLOT_ORDER: list[tuple[str, str, str]] = [
    ("bodyCharacteristic", "body", "BodyCharacteristics.json"),
    ("underwear", "underwear", "Underwear.json"),
    ("skinFeature", "skin feature", "SkinFeatures.json"),
    ("face", "face", "Faces.json"),
    ("ears", "ears", "Ears.json"),
    ("mouth", "mouth", "Mouths.json"),
    ("eyes", "eyes", "Eyes.json"),
    ("eyebrows", "eyebrows", "Eyebrows.json"),
    ("facialHair", "facial hair", "FacialHair.json"),
    ("haircut", "haircut", "Haircuts.json"),
    ("pants", "pants", "Pants.json"),
    ("overpants", "overpants", "Overpants.json"),
    ("undertop", "undertop", "Undertops.json"),
    ("overtop", "overtop", "Overtops.json"),
    ("shoes", "shoes", "Shoes.json"),
    ("gloves", "gloves", "Gloves.json"),
    ("headAccessory", "head accessory", "HeadAccessory.json"),
    ("faceAccessory", "face accessory", "FaceAccessory.json"),
    ("earAccessory", "ear accessory", "EarAccessory.json"),
    ("cape", "cape", "Capes.json"),
]


@dataclass
class ModelAttachment:
    model: str
    texture: str
    gradient_set: str | None = None
    gradient_id: str | None = None

    def to_json(self) -> dict[str, str]:
        out: dict[str, str] = {"Model": self.model, "Texture": self.texture}
        if self.gradient_set:
            out["GradientSet"] = self.gradient_set
            out["GradientId"] = self.gradient_id or ""
        return out


class CosmeticRegistry:
    def __init__(self, assets_root: Path):
        cc = assets_root / "Cosmetics" / "CharacterCreator"
        self.gradient_sets: dict[str, dict[str, Any]] = {}
        with open(cc / "GradientSets.json", encoding="utf-8") as f:
            for gs in json.load(f):
                self.gradient_sets[gs["Id"]] = gs["Gradients"]

        self.maps: dict[str, dict[str, dict[str, Any]]] = {}
        for _key, _label, filename in SLOT_ORDER:
            with open(cc / filename, encoding="utf-8") as f:
                items = json.load(f)
            self.maps[filename] = {item["Id"]: item for item in items}

    def part_map(self, filename: str) -> dict[str, dict[str, Any]]:
        return self.maps[filename]


def texture_path(entry: Any) -> str | None:
    if entry is None:
        return None
    if isinstance(entry, str):
        return entry
    if isinstance(entry, dict):
        return entry.get("Texture") or entry.get("GreyscaleTexture")
    return None


def part_textures(part: dict[str, Any]) -> dict[str, Any] | None:
    if "Textures" in part:
        return part["Textures"]
    return None


def part_variants(part: dict[str, Any]) -> dict[str, Any] | None:
    if "Variants" in part:
        return part["Variants"]
    return None


def inherit_skin_gradient_selector(skin: dict[str, Any], registry: CosmeticRegistry) -> str | None:
    skin_gradients = registry.gradient_sets.get(SKIN_GRADIENT_SET_ID)
    if not skin_gradients:
        return None
    for key in ("bodyCharacteristic", "underwear"):
        raw = skin.get(key)
        if not raw:
            continue
        parts = raw.split(".")
        if len(parts) >= 2 and parts[1] and parts[1] in skin_gradients:
            return parts[1]
    return None


def inherit_hair_gradient_selector(skin: dict[str, Any], registry: CosmeticRegistry) -> str | None:
    hair_gradients = registry.gradient_sets.get(HAIR_GRADIENT_SET_ID)
    if not hair_gradients:
        return None
    for key in ("eyebrows", "facialHair", "haircut"):
        raw = skin.get(key)
        if not raw:
            continue
        parts = raw.split(".")
        if len(parts) >= 2 and parts[1] and parts[1] in hair_gradients:
            return parts[1]
    return None


def effective_haircut_id(skin: dict[str, Any], registry: CosmeticRegistry) -> str | None:
    haircut_id = skin.get("haircut")
    if not haircut_id:
        return None
    head_accessory_id = skin.get("headAccessory")
    if not head_accessory_id:
        return haircut_id

    haircuts = registry.part_map("Haircuts.json")
    head_accessories = registry.part_map("HeadAccessory.json")

    haircut_parts = haircut_id.split(".")
    haircut_asset_id = haircut_parts[0]
    haircut_texture_id = haircut_parts[1] if len(haircut_parts) > 1 and haircut_parts[1] else None

    acc_parts = head_accessory_id.split(".")
    head_part = head_accessories.get(acc_parts[0])
    if not head_part:
        return haircut_id

    if head_part.get("HeadAccessoryType") != "HalfCovering":
        return haircut_id

    haircut_part = haircuts.get(haircut_asset_id)
    if not haircut_part:
        return haircut_id

    if not haircut_part.get("RequiresGenericHaircut"):
        return haircut_id

    hair_type = haircut_part.get("HairType")
    if not hair_type:
        return haircut_id

    base_id = "Generic" + hair_type
    base_part = haircuts.get(base_id)
    if not base_part:
        return haircut_id

    tone = haircut_texture_id or inherit_hair_gradient_selector(skin, registry)
    if tone:
        return f"{base_id}.{tone}"
    return haircut_id


def resolve_slot(
    slot_label: str,
    part_id: str,
    part_map: dict[str, dict[str, Any]],
    registry: CosmeticRegistry,
    skin: dict[str, Any],
) -> ModelAttachment:
    id_parts = part_id.split(".")
    part = part_map.get(id_parts[0])
    if part is None:
        raise ValueError(f"Unknown {slot_label} asset id: {id_parts[0]} (full id: {part_id})")

    variants = part_variants(part)
    variant_id: str | None = None
    if variants is not None:
        if len(id_parts) <= 2 or not id_parts[2]:
            raise ValueError(f"{slot_label} requires assetId.selector.variantId: {part_id}")
        variant_id = id_parts[2]
        if variant_id not in variants:
            raise ValueError(f"{slot_label} unknown variant '{variant_id}' for id: {part_id}")
    else:
        variant_id = None

    if len(id_parts) >= 2 and id_parts[1]:
        selector = id_parts[1]
        if not selector:
            raise ValueError(f"{slot_label} empty selector in id: {part_id}")
    elif part.get("GradientSet") == SKIN_GRADIENT_SET_ID:
        selector = inherit_skin_gradient_selector(skin, registry)
        if not selector:
            raise ValueError(
                f"{slot_label} id has no skin tone ({part_id}). "
                "Set body/underwear with a Skin gradient (e.g. Muscular.01) or use AssetId.tone on the part."
            )
    elif part.get("GradientSet") == HAIR_GRADIENT_SET_ID:
        selector = inherit_hair_gradient_selector(skin, registry)
        if not selector:
            raise ValueError(
                f"{slot_label} id has no hair color ({part_id}). "
                "Set haircut/eyebrows with a Hair gradient or use AssetId.color."
            )
    else:
        raise ValueError(
            f"{slot_label} id must include a selector after the asset id (e.g. BodyId.gradientOrTexture): {part_id}"
        )

    if variant_id is not None:
        variant = variants[variant_id]
        model_path = variant.get("Model")
        greyscale = variant.get("GreyscaleTexture")
        texture_map = variant.get("Textures")
    else:
        model_path = part.get("Model")
        greyscale = part.get("GreyscaleTexture")
        texture_map = part_textures(part)

    if not model_path:
        raise ValueError(f"{slot_label} missing model path for id: {part_id}")

    gradient_set_id = part.get("GradientSet")
    gradient_match = False
    if gradient_set_id:
        gs = registry.gradient_sets.get(gradient_set_id)
        if gs and selector in gs:
            gradient_match = True

    if gradient_match:
        if not greyscale:
            raise ValueError(f"{slot_label} gradient part missing GreyscaleTexture for id: {part_id}")
        return ModelAttachment(model_path, greyscale, gradient_set_id, selector)

    if not texture_map or selector not in texture_map:
        raise ValueError(f"{slot_label} unknown texture/gradient key '{selector}' for id: {part_id}")

    tex_path = texture_path(texture_map[selector])
    if not tex_path:
        raise ValueError(f"{slot_label} empty texture path for id: {part_id}")
    return ModelAttachment(model_path, tex_path)


def skin_to_attachments(skin: dict[str, Any], registry: CosmeticRegistry) -> list[ModelAttachment]:
    attachments: list[ModelAttachment] = []
    for skin_key, slot_label, filename in SLOT_ORDER:
        if skin_key == "haircut":
            raw = effective_haircut_id(skin, registry)
        else:
            raw = skin.get(skin_key)
        if raw is None or raw == "":
            continue
        part_map = registry.part_map(filename)
        att = resolve_slot(slot_label, raw, part_map, registry, skin)
        attachments.append(att)
    return attachments


def to_model_json(skin: dict[str, Any], registry: CosmeticRegistry) -> dict[str, Any]:
    return {
        "Parent": PARENT_PLAYER,
        "DefaultAttachments": [a.to_json() for a in skin_to_attachments(skin, registry)],
    }


def common_asset_exists(common_root: Path, relative_path: str) -> bool:
    return (common_root / relative_path.replace("/", os.sep)).is_file()


def validate_model(model: dict[str, Any], common_root: Path) -> list[str]:
    errors: list[str] = []
    for i, att in enumerate(model.get("DefaultAttachments", [])):
        for key in ("Model", "Texture"):
            path = att.get(key)
            if path and not common_asset_exists(common_root, path):
                errors.append(f"attachment[{i}].{key}: missing Common asset '{path}'")
    return errors


def load_manifest(path: Path) -> dict[str, str]:
    mapping: dict[str, str] = {}
    with open(path, encoding="utf-8", newline="") as f:
        for row in csv.reader(f):
            if len(row) < 2 or not row[0].strip() or row[0].strip().startswith("#"):
                continue
            src, dest = row[0].strip(), row[1].strip()
            mapping[src] = dest
    return mapping


def output_name_for(stem: str, manifest: dict[str, str]) -> str:
    if stem in manifest:
        return manifest[stem]
    for key, name in manifest.items():
        if key.lower() == stem.lower():
            return name
    return stem


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert CachedPlayerSkins JSON files to Hytale Server/Models JSON (Player + DefaultAttachments)."
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=DEFAULT_INPUT,
        help="Directory containing cached skin JSON files (default: %%APPDATA%%/Hytale/UserData/CachedPlayerSkins)",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Directory for output model JSON files (default: Aetherhaven test pack Server/Models)",
    )
    parser.add_argument(
        "--assets",
        type=Path,
        default=DEFAULT_ASSETS,
        help="Hytale Assets root (contains Cosmetics/CharacterCreator and Common/)",
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        help="Optional CSV: source_stem_or_uuid,ModelAssetName (e.g. uuid,Villager_Merchant)",
    )
    parser.add_argument(
        "--no-validate",
        action="store_true",
        help="Skip checking that model/texture paths exist under Assets/Common",
    )
    args = parser.parse_args()

    if not args.assets.is_dir():
        print(f"Assets path not found: {args.assets}", file=sys.stderr)
        return 1
    if not args.input.is_dir():
        print(f"Input directory not found: {args.input}", file=sys.stderr)
        return 1

    common_root = args.assets / "Common"
    registry = CosmeticRegistry(args.assets)
    manifest = load_manifest(args.manifest) if args.manifest else {}

    skin_files = sorted(args.input.glob("*.json"))
    if not skin_files:
        print(f"No JSON files in {args.input}")
        return 0

    args.output.mkdir(parents=True, exist_ok=True)
    ok = 0
    failed = 0

    for skin_path in skin_files:
        stem = skin_path.stem
        out_name = output_name_for(stem, manifest)
        try:
            with open(skin_path, encoding="utf-8") as f:
                skin = json.load(f)
            model = to_model_json(skin, registry)
            if not args.no_validate:
                errs = validate_model(model, common_root)
                if errs:
                    raise ValueError("; ".join(errs))
            out_path = args.output / f"{out_name}.json"
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(model, f, indent=2)
                f.write("\n")
            print(f"OK  {skin_path.name} -> {out_path.name} ({len(model['DefaultAttachments'])} attachments)")
            ok += 1
        except Exception as e:
            print(f"FAIL {skin_path.name}: {e}", file=sys.stderr)
            failed += 1

    print(f"Done: {ok} converted, {failed} failed, output -> {args.output}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
