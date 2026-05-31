import json
from pathlib import Path

bb = json.loads(Path(r"c:\Users\gchou\OneDrive\Documents\Hytale-Modding\ArtAssets\prowl_hytale.bbmodel").read_text(encoding="utf-8"))
bm = json.loads(
    Path(r"c:\Users\gchou\OneDrive\Documents\Hytale-Modding\Aetherhaven\src\main\resources\Common\NPC\Prowl\prowl_hytale.blockymodel").read_text(
        encoding="utf-8"
    )
)

print("bb resolution:", bb.get("resolution"))
print("bb format:", bb.get("meta", {}).get("model_format"))
textures = bb.get("textures") or []
print("bb textures:", len(textures))
if textures:
    t0 = textures[0]
    if isinstance(t0, dict):
        print("bb tex0 keys:", sorted(t0.keys()))
        for k in ("width", "height", "name", "path", "relative_path", "folder"):
            if k in t0:
                v = t0[k]
                if isinstance(v, str) and len(v) > 120:
                    v = v[:120] + "..."
                print(f"  {k}: {v}")

mx = my = 0
for el in bb.get("elements", []):
    for data in (el.get("faces") or {}).values():
        uv = data.get("uv") or []
        if len(uv) == 4:
            mx = max(mx, uv[0], uv[2])
            my = max(my, uv[1], uv[3])
print("bb max uv:", mx, my)

bm_boxes = []

def walk_bm(nodes):
    for n in nodes:
        sh = n.get("shape") or {}
        if sh.get("type") == "box":
            tl = sh.get("textureLayout") or {}
            sz = (sh.get("settings") or {}).get("size") or {}
            bm_boxes.append((n.get("name"), sz, tl, sh.get("visible")))
        walk_bm(n.get("children") or [])

walk_bm(bm.get("nodes") or [])
print("bm boxes:", len(bm_boxes))
print("bm format:", bm.get("format"))

for name in ("Pelvis", "Belly", "belly", "Neck", "chest"):
    for el in bb.get("elements", []):
        if el.get("name") == name:
            print(f"\nbb {name}: from={el['from']} to={el['to']}")
            for face, fd in (el.get("faces") or {}).items():
                print(f"  {face}: {fd.get('uv')}")

    for bname, sz, tl, vis in bm_boxes:
        if bname.lower().startswith(name.lower()) or name.lower() in bname.lower():
            print(f"bm {bname}: size={sz} visible={vis}")
            for face, fd in tl.items():
                off = fd.get("offset", {})
                print(f"  {face}: offset=({off.get('x')},{off.get('y')}) mirror={fd.get('mirror')}")
