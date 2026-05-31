import json
from pathlib import Path

bb = json.loads(Path(r"c:\Users\gchou\OneDrive\Documents\Hytale-Modding\ArtAssets\prowl_hytale.bbmodel").read_text(encoding="utf-8"))
bm = json.loads(
    Path(r"c:\Users\gchou\OneDrive\Documents\Hytale-Modding\Aetherhaven\src\main\resources\Common\NPC\Prowl\prowl_hytale.blockymodel").read_text(
        encoding="utf-8"
    )
)

tex = bb["textures"][0]
print("texture width/height:", tex.get("width"), tex.get("height"))
print("texture uv_width/uv_height:", tex.get("uv_width"), tex.get("uv_height"))
print("resolution:", bb.get("resolution"))

bb_map = {el["name"]: el for el in bb["elements"]}

bm_boxes = []


def walk(nodes):
    for n in nodes:
        sh = n.get("shape") or {}
        if sh.get("type") == "box":
            bm_boxes.append(n)
        walk(n.get("children") or [])


walk(bm.get("nodes") or [])

face_map = {"north": "back", "south": "front", "east": "right", "west": "left", "up": "top", "down": "bottom"}

print("\nUV span vs box size mismatches:")
for n in bm_boxes:
    base_name = n["name"].split("--")[0]
    bb_el = bb_map.get(base_name)
    if not bb_el:
        continue
    sz = (n["shape"]["settings"] or {}).get("size") or {}
    sx, sy, sz_z = sz.get("x"), sz.get("y"), sz.get("z")
    faces_bb = bb_el.get("faces") or {}
    for bb_face, bm_face in face_map.items():
        uv = (faces_bb.get(bb_face) or {}).get("uv") or []
        if len(uv) != 4:
            continue
        uw = abs(uv[2] - uv[0])
        uh = abs(uv[3] - uv[1])
        if bb_face in ("north", "south"):
            exp_w, exp_h = sx, sy
        elif bb_face in ("east", "west"):
            exp_w, exp_h = sz_z, sy
        else:
            exp_w, exp_h = sx, sz_z
        if abs(uw - exp_w) > 0.01 or abs(uh - exp_h) > 0.01:
            tl = (n["shape"].get("textureLayout") or {}).get(bm_face) or {}
            off = tl.get("offset") or {}
            print(
                f"{base_name}/{bm_face}: bb_uv=({uw:.1f}x{uh:.1f}) "
                f"expected=({exp_w}x{exp_h}) bm_size=({sx},{sy},{sz_z}) "
                f"offset=({off.get('x')},{off.get('y')})"
            )

print("\nZero / missing UV faces in bbmodel:")
for el in bb["elements"]:
    for face, fd in (el.get("faces") or {}).items():
        uv = fd.get("uv") or []
        if len(uv) == 4 and uv[2] - uv[0] == 0 and uv[3] - uv[1] == 0:
            print(f"  {el['name']}/{face}: {uv}")
