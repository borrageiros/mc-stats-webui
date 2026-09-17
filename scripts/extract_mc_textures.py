import argparse
import json
import os
import shutil
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATIC = ROOT / "web" / "static" / "mc"
ICONS = ROOT / "web" / "src" / "lib" / "mc" / "icons.json"
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"


def gradle_home() -> Path:
	env = os.environ.get("GRADLE_USER_HOME")
	if env:
		return Path(env)
	return Path.home() / ".gradle"


def default_jar(version: str) -> Path:
	return gradle_home() / "caches" / "fabric-loom" / version / "minecraft-client.jar"


def download_client_jar(version: str, dest: Path) -> None:
	print(f"Downloading Minecraft {version} client jar…")
	with urllib.request.urlopen(MANIFEST_URL) as response:
		manifest = json.load(response)
	entry = next((item for item in manifest["versions"] if item["id"] == version), None)
	if not entry:
		raise SystemExit(f"Minecraft version not found in manifest: {version}")
	with urllib.request.urlopen(entry["url"]) as response:
		meta = json.load(response)
	client = meta.get("downloads", {}).get("client")
	if not client or "url" not in client:
		raise SystemExit(f"No client download for Minecraft {version}")
	dest.parent.mkdir(parents=True, exist_ok=True)
	tmp = dest.with_suffix(".jar.tmp")
	urllib.request.urlretrieve(client["url"], tmp)
	tmp.replace(dest)
	print(f"Saved {dest}")


def resolve_jar(version: str, jar_arg: str | None) -> Path:
	jar = Path(jar_arg) if jar_arg else default_jar(version)
	if jar.is_file():
		return jar
	download_client_jar(version, jar)
	if not jar.is_file():
		raise SystemExit(f"Missing Minecraft client jar: {jar}")
	return jar


def tex_url(tex: str) -> str | None:
	value = tex.replace("minecraft:", "")
	if value.startswith("#"):
		return None
	if value.startswith("item/"):
		return "/mc/item/" + value[5:] + ".png"
	if value.startswith("block/"):
		return "/mc/block/" + value[6:] + ".png"
	if "/" not in value:
		return "/mc/item/" + value + ".png"
	return "/mc/" + value + ".png"


def load_json(jar: zipfile.ZipFile, path: str):
	try:
		return json.loads(jar.read(path))
	except KeyError:
		return None


def first_string_model(node) -> str | None:
	if isinstance(node, dict):
		kind = node.get("type")
		model = node.get("model")
		if kind in ("minecraft:model", "model") and isinstance(model, str):
			return model
		for value in node.values():
			found = first_string_model(value)
			if found:
				return found
	elif isinstance(node, list):
		for value in node:
			found = first_string_model(value)
			if found:
				return found
	return None


def model_path(model_id: str) -> str:
	value = model_id.replace("minecraft:", "")
	if not value.startswith("block/") and not value.startswith("item/"):
		value = "item/" + value
	return "assets/minecraft/models/" + value + ".json"


def textures_from_model(jar: zipfile.ZipFile, model_id: str, seen: set[str]) -> dict:
	if not model_id or model_id in seen:
		return {}
	seen.add(model_id)
	data = load_json(jar, model_path(model_id))
	if not data:
		return {}
	merged = {}
	parent = data.get("parent")
	if isinstance(parent, str):
		merged.update(textures_from_model(jar, parent, seen))
	textures = data.get("textures")
	if isinstance(textures, dict):
		merged.update(textures)
	return merged


def pick_texture(textures: dict) -> str | None:
	for key in (
		"layer0",
		"all",
		"particle",
		"texture",
		"front",
		"side",
		"top",
		"end",
		"up",
		"north",
		"plant",
		"cross",
		"rail",
		"fan",
		"panicle",
		"crop",
		"wool",
		"fire",
		"lantern",
		"torch",
	):
		value = textures.get(key)
		if isinstance(value, str) and not value.startswith("#"):
			return value
	for value in textures.values():
		if isinstance(value, str) and not value.startswith("#"):
			return value
	return None


def resolve_item(jar: zipfile.ZipFile, item_id: str, data: dict) -> str | None:
	model = first_string_model(data)
	if not model:
		return None
	textures = textures_from_model(jar, model, set())
	picked = pick_texture(textures)
	if picked:
		url = tex_url(picked)
		if url and file_exists(url):
			return url
	name = item_id.replace("minecraft:", "")
	for candidate in (
		f"/mc/item/{name}.png",
		f"/mc/block/{name}.png",
		f"/mc/block/{name}_front.png",
		f"/mc/block/{name}_top.png",
		f"/mc/block/{name}_side.png",
		f"/mc/item/{name}_spawn_egg.png",
	):
		if file_exists(candidate):
			return candidate
	return None


def copy_pngs(jar: zipfile.ZipFile) -> None:
	if STATIC.exists():
		shutil.rmtree(STATIC)
	for name in jar.namelist():
		if not name.endswith(".png"):
			continue
		dest = None
		if name.startswith("assets/minecraft/textures/item/"):
			dest = STATIC / "item" / Path(name).name
		elif name.startswith("assets/minecraft/textures/block/"):
			dest = STATIC / "block" / Path(name).name
		elif name == "assets/minecraft/textures/entity/chest/normal.png":
			dest = STATIC / "entity" / "chest.png"
		elif name == "assets/minecraft/textures/entity/chest/ender.png":
			dest = STATIC / "entity" / "ender_chest.png"
		elif name == "assets/minecraft/textures/entity/chest/trapped.png":
			dest = STATIC / "entity" / "trapped_chest.png"
		elif name == "assets/minecraft/textures/entity/shield/shield_base_nopattern.png":
			dest = STATIC / "entity" / "shield.png"
		elif name == "assets/minecraft/textures/entity/bed/red.png":
			dest = STATIC / "entity" / "bed.png"
		elif name == "assets/minecraft/textures/entity/shulker/shulker.png":
			dest = STATIC / "entity" / "shulker.png"
		if dest is None:
			continue
		dest.parent.mkdir(parents=True, exist_ok=True)
		dest.write_bytes(jar.read(name))
	player_head = ROOT / "web" / "src" / "lib" / "assets" / "mc" / "player_head.png"
	if player_head.is_file():
		item_dir = STATIC / "item"
		item_dir.mkdir(parents=True, exist_ok=True)
		shutil.copyfile(player_head, item_dir / "player_head.png")


def fill_missing(icons: dict[str, str], item_id: str) -> None:
	if item_id in icons:
		return
	name = item_id.replace("minecraft:", "")
	candidates = []
	swaps = (
		("_wall_sign", "_sign"),
		("_wall_hanging_sign", "_hanging_sign"),
		("_wall_fan", "_fan"),
		("_wall_coral", ""),
	)
	for suffix, replacement in swaps:
		if name.endswith(suffix):
			renamed = name[: -len(suffix)] + replacement
			candidates.append(f"/mc/item/{renamed}.png")
			candidates.append(f"/mc/block/{renamed}.png")
	if name.startswith("wall_"):
		rest = name[5:]
		candidates.append(f"/mc/item/{rest}.png")
		candidates.append(f"/mc/block/{rest}.png")
	crops = {
		"potatoes": "potato",
		"carrots": "carrot",
		"beetroots": "beetroot",
		"sweet_berry_bush": "sweet_berries",
		"melon_stem": "melon_seeds",
		"attached_melon_stem": "melon_seeds",
		"pumpkin_stem": "pumpkin_seeds",
		"attached_pumpkin_stem": "pumpkin_seeds",
		"pitcher_crop": "pitcher_pod",
		"torchflower_crop": "torchflower",
		"cocoa": "cocoa_beans",
		"nether_wart": "nether_wart",
		"wheat": "wheat",
		"decorated_pot": "flower_pot",
	}
	if name in crops:
		candidates.append(f"/mc/item/{crops[name]}.png")
		candidates.append(f"/mc/block/{crops[name]}.png")
	candidates.extend(
		(
			f"/mc/item/{name}.png",
			f"/mc/block/{name}.png",
			f"/mc/block/{name}_stage3.png",
			f"/mc/block/{name}_stage7.png",
			f"/mc/block/{name}_0.png",
		)
	)
	for url in candidates:
		if file_exists(url):
			icons[item_id] = url
			return


def file_exists(url: str) -> bool:
	relative = url[4:] if url.startswith("/mc/") else url
	return (STATIC / relative).is_file()


def main() -> None:
	parser = argparse.ArgumentParser()
	parser.add_argument("--version", default=os.environ.get("MC_VERSION", "26.3"))
	parser.add_argument("--jar")
	args = parser.parse_args()
	jar_path = resolve_jar(args.version, args.jar)
	with zipfile.ZipFile(jar_path) as jar:
		copy_pngs(jar)
		icons: dict[str, str] = {}
		for entry in jar.namelist():
			if not entry.startswith("assets/minecraft/items/") or not entry.endswith(".json"):
				continue
			item_id = "minecraft:" + Path(entry).stem
			data = load_json(jar, entry)
			if not data:
				continue
			url = resolve_item(jar, item_id, data)
			if url and file_exists(url):
				icons[item_id] = url
		specials = {
			"minecraft:chest": "/mc/entity/chest.png",
			"minecraft:ender_chest": "/mc/entity/ender_chest.png",
			"minecraft:trapped_chest": "/mc/entity/trapped_chest.png",
			"minecraft:copper_chest": "/mc/entity/chest.png",
			"minecraft:shield": "/mc/entity/shield.png",
			"minecraft:white_banner": "/mc/block/white_wool.png",
			"minecraft:water": "/mc/block/water_still.png",
			"minecraft:lava": "/mc/block/lava_still.png",
			"minecraft:fire": "/mc/block/fire_0.png",
			"minecraft:soul_fire": "/mc/block/soul_fire_0.png",
			"minecraft:air": "/mc/item/barrier.png",
			"minecraft:cave_air": "/mc/item/barrier.png",
			"minecraft:void_air": "/mc/item/barrier.png",
			"minecraft:player": "/mc/item/player_head.png",
			"minecraft:zombie_head": "/mc/item/zombie_spawn_egg.png",
			"minecraft:skeleton_skull": "/mc/item/skeleton_spawn_egg.png",
			"minecraft:creeper_head": "/mc/item/creeper_spawn_egg.png",
			"minecraft:dragon_head": "/mc/item/ender_dragon_spawn_egg.png",
			"minecraft:piglin_head": "/mc/item/piglin_spawn_egg.png",
			"minecraft:wither_skeleton_skull": "/mc/item/wither_skeleton_spawn_egg.png",
			"minecraft:zombie_wall_head": "/mc/item/zombie_spawn_egg.png",
			"minecraft:creeper_wall_head": "/mc/item/creeper_spawn_egg.png",
			"minecraft:dragon_wall_head": "/mc/item/ender_dragon_spawn_egg.png",
			"minecraft:piglin_wall_head": "/mc/item/piglin_spawn_egg.png",
			"minecraft:player_wall_head": "/mc/item/player_head.png",
			"minecraft:skeleton_wall_skull": "/mc/item/skeleton_spawn_egg.png",
			"minecraft:wither_skeleton_wall_skull": "/mc/item/wither_skeleton_spawn_egg.png",
			"minecraft:red_bed": "/mc/entity/bed.png",
			"minecraft:shulker_box": "/mc/entity/shulker.png",
			"minecraft:decorated_pot": "/mc/item/flower_pot.png",
			"minecraft:conduit": "/mc/block/conduit.png",
			"minecraft:player_head": "/mc/item/player_head.png",
			"minecraft:potatoes": "/mc/item/potato.png",
			"minecraft:carrots": "/mc/item/carrot.png",
			"minecraft:beetroots": "/mc/item/beetroot.png",
		}
		for item_id, url in specials.items():
			if file_exists(url):
				icons[item_id] = url
		for png in (STATIC / "item").glob("*_spawn_egg.png"):
			entity = "minecraft:" + png.stem.removesuffix("_spawn_egg")
			icons.setdefault(entity, "/mc/item/" + png.name)
		for png in (STATIC / "block").glob("*.png"):
			stem = png.stem
			if stem.endswith("_top") or stem.endswith("_side") or stem.endswith("_front") or stem.endswith("_bottom"):
				continue
			icons.setdefault("minecraft:" + stem, "/mc/block/" + png.name)
		for entry in jar.namelist():
			if not entry.endswith(".json"):
				continue
			if entry.startswith("assets/minecraft/items/") or entry.startswith("assets/minecraft/models/block/"):
				fill_missing(icons, "minecraft:" + Path(entry).stem)
		ICONS.parent.mkdir(parents=True, exist_ok=True)
		ICONS.write_text(json.dumps(icons, indent="\t", sort_keys=True) + "\n", encoding="utf-8")
		print(f"Wrote {len(icons)} icons and textures to {STATIC}")


if __name__ == "__main__":
	main()
