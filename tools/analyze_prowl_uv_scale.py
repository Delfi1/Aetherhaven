import json
from pathlib import Path

bb = json.loads(Path(r"c:\Users\gchou\OneDrive\Documents\Hytale-Modding\ArtAssets\prowl_hytale.bbmodel").read_text(encoding="utf-8"))
tex = bb["textures"][0]
uv_factor = tex["width"] / tex["uv_width"]
print(f"Texture: {tex['width']}x{tex['height']}, UV canvas: {tex['uv_width']}x{tex['uv_height']}")
print(f"uvFactor (texture/uv canvas): {uv_factor}")
print(f"Project resolution: {bb.get('resolution')}")
print()

face_map = {"north": "back", "south": "front", "east": "right", "west": "left", "up": "top", "down": "bottom"}
mismatches = []

for el in bb["elements"]:
    dims = [el["to"][i] - el["from"][i] for i in range(3)]
    for bb_face, bm_face in face_map.items():
        fd = (el.get("faces") or {}).get(bb_face) or {}
        uv = fd.get("uv") or []
        if len(uv) != 4:
            continue
        uw, uh = abs(uv[2] - uv[0]), abs(uv[3] - uv[1])
        if bb_face in ("north", "south"):
            exp_w, exp_h = dims[0], dims[1]
        elif bb_face in ("east", "west"):
            exp_w, exp_h = dims[2], dims[1]
        else:
            exp_w, exp_h = dims[0], dims[2]
        if abs(uw - exp_w) > 0.01 or abs(uh - exp_h) > 0.01:
            mismatches.append((el["name"], bm_face, exp_w, exp_h, uw, uh, uv))

print(f"Faces where UV span != box face size: {len(mismatches)}")
for row in mismatches[:15]:
    name, face, ew, eh, uw, uh, uv = row
    print(f"  {name}/{face}: box {ew}x{eh}, uv span {uw}x{uh}, uv={uv}")
if len(mismatches) > 15:
    print(f"  ... and {len(mismatches) - 15} more")

print()
print("Sample offsets if scaled by uvFactor for 512px texture:")
for name in ("Pelvis", "belly", "L-Arm"):
    el = next(e for e in bb["elements"] if e["name"] == name)
    uv = el["faces"]["north"]["uv"]
    ox, oy = min(uv[0], uv[2]), min(uv[1], uv[3])
    print(f"  {name} back: bb offset ({ox}, {oy}) -> scaled ({ox * uv_factor}, {oy * uv_factor})")
