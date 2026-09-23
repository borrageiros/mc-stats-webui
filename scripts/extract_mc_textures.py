import argparse
import json
import os
import shutil
import struct
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
import zlib
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATIC = ROOT / "web" / "static" / "mc"
ICONS = ROOT / "web" / "src" / "lib" / "mc" / "icons.json"
ADVANCEMENT_LANG = ROOT / "web" / "src" / "lib" / "mc" / "advancementLang.json"
INV_CACHE = ROOT / "scripts" / ".cache" / "invicons"
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
ASSET_CDN = "https://resources.download.minecraft.net"
WIKI_FILE = "https://minecraft.wiki/w/Special:FilePath/"
USER_AGENT = "mc-stats-webui texture extract"
CUBE_HINTS = (
	"block/cube",
	"block/cube_all",
	"block/cube_column",
	"block/cube_column_horizontal",
	"block/cube_bottom_top",
	"block/cube_top",
	"block/cube_mirrored",
	"block/orientable",
	"block/leaves",
)
LANG_FILES = {
	"en": "assets/minecraft/lang/en_us.json",
	"es": "assets/minecraft/lang/es_es.json",
	"fr": "assets/minecraft/lang/fr_fr.json",
}
_VERSION_META: dict[str, dict] = {}


def gradle_home() -> Path:
	env = os.environ.get("GRADLE_USER_HOME")
	if env:
		return Path(env)
	return Path.home() / ".gradle"


def default_jar(version: str) -> Path:
	return gradle_home() / "caches" / "fabric-loom" / version / "minecraft-client.jar"


def fetch_json(url: str):
	request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
	with urllib.request.urlopen(request) as response:
		return json.load(response)


def version_meta(version: str) -> dict:
	cached = _VERSION_META.get(version)
	if cached:
		return cached
	manifest = fetch_json(MANIFEST_URL)
	entry = next((item for item in manifest["versions"] if item["id"] == version), None)
	if not entry:
		raise SystemExit(f"Minecraft version not found in manifest: {version}")
	meta = fetch_json(entry["url"])
	_VERSION_META[version] = meta
	return meta


def download_client_jar(version: str, dest: Path) -> None:
	print(f"Downloading Minecraft {version} client jar…")
	meta = version_meta(version)
	client = meta.get("downloads", {}).get("client")
	if not client or "url" not in client:
		raise SystemExit(f"No client download for Minecraft {version}")
	dest.parent.mkdir(parents=True, exist_ok=True)
	tmp = dest.with_suffix(".jar.tmp")
	urllib.request.urlretrieve(client["url"], tmp)
	tmp.replace(dest)
	print(f"Saved {dest}")


def download_lang_asset(version: str, filename: str) -> dict | None:
	cache = default_jar(version).parent / "lang" / filename
	if cache.is_file():
		try:
			data = json.loads(cache.read_text(encoding="utf-8"))
			if isinstance(data, dict) and data:
				return data
		except json.JSONDecodeError:
			pass
	meta = version_meta(version)
	index = meta.get("assetIndex")
	if not isinstance(index, dict) or "url" not in index:
		return None
	assets = fetch_json(index["url"])
	obj = assets.get("objects", {}).get(f"minecraft/lang/{filename}")
	if not isinstance(obj, dict) or "hash" not in obj:
		return None
	digest = str(obj["hash"])
	url = f"{ASSET_CDN}/{digest[:2]}/{digest}"
	print(f"Downloading {filename}…")
	data = fetch_json(url)
	if not isinstance(data, dict):
		return None
	cache.parent.mkdir(parents=True, exist_ok=True)
	cache.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
	return data


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
	models = collect_model_ids(node)
	return models[0] if models else None


def collect_model_ids(node) -> list[str]:
	found: list[str] = []
	if isinstance(node, dict):
		kind = str(node.get("type", "")).replace("minecraft:", "")
		model = node.get("model")
		if kind == "model" and isinstance(model, str):
			found.append(model)
		for value in node.values():
			found.extend(collect_model_ids(value))
	elif isinstance(node, list):
		for value in node:
			found.extend(collect_model_ids(value))
	return found


def node_has_type(node, *types: str) -> bool:
	wanted = {item.replace("minecraft:", "") for item in types}
	if isinstance(node, dict):
		kind = str(node.get("type", "")).replace("minecraft:", "")
		if kind in wanted:
			return True
		return any(node_has_type(value, *types) for value in node.values())
	if isinstance(node, list):
		return any(node_has_type(value, *types) for value in node)
	return False


def model_is_cube(jar: zipfile.ZipFile, model_id: str) -> bool:
	seen: set[str] = set()
	current = model_id
	while current and current not in seen:
		seen.add(current)
		path = current.replace("minecraft:", "")
		if any(hint in path for hint in CUBE_HINTS):
			return True
		data = load_json(jar, model_path(current))
		if not data:
			return False
		parent = data.get("parent")
		if not isinstance(parent, str):
			return False
		current = parent
	return False


def item_models_are_cube(jar: zipfile.ZipFile, data: dict) -> bool:
	if node_has_type(data, "composite", "special"):
		return False
	models = collect_model_ids(data)
	return bool(models) and all(model_is_cube(jar, model) for model in models)


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


def resolve_refs(textures: dict) -> dict[str, str]:
	resolved = {key: value for key, value in textures.items() if isinstance(value, str)}
	changed = True
	while changed:
		changed = False
		for key, value in list(resolved.items()):
			if value.startswith("#"):
				ref = resolved.get(value[1:])
				if isinstance(ref, str) and not ref.startswith("#"):
					resolved[key] = ref
					changed = True
	return {key: value for key, value in resolved.items() if not value.startswith("#")}


def first_tex(textures: dict[str, str], *keys: str) -> str | None:
	for key in keys:
		value = textures.get(key)
		if value:
			return value
	return None


def cube_faces(textures: dict) -> tuple[str, str, str] | None:
	resolved = resolve_refs(textures)
	if "layer0" in resolved or "cross" in resolved or "plant" in resolved:
		return None
	fill = first_tex(resolved, "all", "particle", "texture")
	up = first_tex(resolved, "up", "top", "end") or fill
	left = first_tex(resolved, "south", "front", "north", "side") or fill
	right = first_tex(resolved, "east", "west", "side") or fill
	if up and left and right:
		return up, left, right
	return None


def static_path(url: str) -> Path:
	relative = url[4:] if url.startswith("/mc/") else url
	return STATIC / relative


def read_png(path: Path) -> list[list[tuple[int, int, int, int]]] | None:
	try:
		data = path.read_bytes()
	except OSError:
		return None
	if data[:8] != b"\x89PNG\r\n\x1a\n":
		return None
	pos = 8
	width = height = None
	raw = b""
	palette = []
	alpha = None
	color_type = 6
	while pos + 8 <= len(data):
		size, kind = struct.unpack(">I4s", data[pos : pos + 8])
		chunk = data[pos + 8 : pos + 8 + size]
		pos += 12 + size
		if kind == b"IHDR":
			width, height, bit, color_type = struct.unpack(">IIBB", chunk[:10])
			if bit not in (1, 2, 4, 8):
				return None
		elif kind == b"PLTE":
			palette = [tuple(chunk[i : i + 3]) for i in range(0, len(chunk), 3)]
		elif kind == b"tRNS":
			alpha = chunk
		elif kind == b"IDAT":
			raw += chunk
		elif kind == b"IEND":
			break
	if width is None or height is None:
		return None
	try:
		decoded = zlib.decompress(raw)
	except zlib.error:
		return None
	channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}.get(color_type)
	if channels is None:
		return None
	bpp = max(1, (channels * bit + 7) // 8)
	stride = (width * channels * bit + 7) // 8
	rows = []
	src = 0
	prev = bytearray(stride)
	for _ in range(height):
		if src + 1 + stride > len(decoded):
			return None
		filter_type = decoded[src]
		scan = bytearray(decoded[src + 1 : src + 1 + stride])
		src += 1 + stride
		for i, value in enumerate(scan):
			left = scan[i - bpp] if i >= bpp else 0
			up = prev[i]
			up_left = prev[i - bpp] if i >= bpp else 0
			if filter_type == 1:
				scan[i] = (value + left) & 255
			elif filter_type == 2:
				scan[i] = (value + up) & 255
			elif filter_type == 3:
				scan[i] = (value + ((left + up) >> 1)) & 255
			elif filter_type == 4:
				p = left + up - up_left
				pa, pb, pc = abs(p - left), abs(p - up), abs(p - up_left)
				pr = left if pa <= pb and pa <= pc else up if pb <= pc else up_left
				scan[i] = (value + pr) & 255
			elif filter_type != 0:
				return None
		prev = scan
		row = []
		for x in range(width):
			if color_type == 3:
				index = sample_bits(scan, x, bit)
				rgb = palette[index] if index < len(palette) else (0, 0, 0)
				a = alpha[index] if alpha and index < len(alpha) else 255
				row.append((rgb[0], rgb[1], rgb[2], a))
			elif color_type == 0:
				value = sample_bits(scan, x, bit)
				if bit < 8:
					value = value * (255 // ((1 << bit) - 1))
				row.append((value, value, value, 255))
			else:
				i = x * channels
				if color_type == 6:
					row.append((scan[i], scan[i + 1], scan[i + 2], scan[i + 3]))
				elif color_type == 2:
					row.append((scan[i], scan[i + 1], scan[i + 2], 255))
				else:
					row.append((scan[i], scan[i], scan[i], scan[i + 1]))
		rows.append(row)
	return rows


def sample_bits(scan: bytearray, index: int, bit: int) -> int:
	if bit == 8:
		return scan[index]
	ppb = 8 // bit
	byte = scan[index // ppb]
	shift = 8 - bit * (index % ppb + 1)
	return (byte >> shift) & ((1 << bit) - 1)


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
	height = len(pixels)
	width = len(pixels[0]) if height else 0
	raw = b"".join(b"\x00" + bytes(channel for pixel in row for channel in pixel) for row in pixels)

	def chunk(tag: bytes, body: bytes) -> bytes:
		return struct.pack(">I", len(body)) + tag + body + struct.pack(">I", zlib.crc32(tag + body) & 0xFFFFFFFF)

	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_bytes(
		b"\x89PNG\r\n\x1a\n"
		+ chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
		+ chunk(b"IDAT", zlib.compress(raw, 9))
		+ chunk(b"IEND", b"")
	)


def scale_nn(
	pixels: list[list[tuple[int, int, int, int]]], width: int, height: int
) -> list[list[tuple[int, int, int, int]]]:
	source_h = len(pixels)
	source_w = len(pixels[0]) if source_h else 0
	if source_w == width and source_h == height:
		return [row[:] for row in pixels]
	if source_w == 0 or source_h == 0:
		return [[(0, 0, 0, 0)] * width for _ in range(height)]
	return [
		[pixels[int(y * source_h / height)][int(x * source_w / width)] for x in range(width)]
		for y in range(height)
	]


def crop_px(
	pixels: list[list[tuple[int, int, int, int]]], x: int, y: int, width: int, height: int
) -> list[list[tuple[int, int, int, int]]]:
	return [row[x : x + width] for row in pixels[y : y + height]]


def shade(pixel: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
	return (min(255, int(pixel[0] * factor)), min(255, int(pixel[1] * factor)), min(255, int(pixel[2] * factor)), pixel[3])


def put_pixel(
	canvas: list[list[tuple[int, int, int, int]]], x: int, y: int, pixel: tuple[int, int, int, int]
) -> None:
	if pixel[3] == 0 or y < 0 or x < 0 or y >= len(canvas) or x >= len(canvas[0]):
		return
	if pixel[3] == 255:
		canvas[y][x] = pixel
		return
	dst = canvas[y][x]
	a = pixel[3] / 255
	canvas[y][x] = (
		int(pixel[0] * a + dst[0] * (1 - a)),
		int(pixel[1] * a + dst[1] * (1 - a)),
		int(pixel[2] * a + dst[2] * (1 - a)),
		min(255, pixel[3] + dst[3]),
	)


def render_iso(
	top: list[list[tuple[int, int, int, int]]],
	left: list[list[tuple[int, int, int, int]]],
	right: list[list[tuple[int, int, int, int]]],
) -> list[list[tuple[int, int, int, int]]]:
	top = scale_nn(top, 16, 16)
	left = scale_nn(left, 16, 16)
	right = scale_nn(right, 16, 16)
	canvas = [[(0, 0, 0, 0) for _ in range(32)] for _ in range(32)]
	for y in range(16):
		for x in range(16):
			put_pixel(canvas, x, (x + 15) // 2 + y, shade(left[y][x], 0.8))
			put_pixel(canvas, 15 + x, (30 - x) // 2 + y, shade(right[y][x], 0.55))
	for y in range(16):
		for x in range(16):
			put_pixel(canvas, 15 + x - y, (x + y) // 2, shade(top[y][x], 1.0))
	return canvas


def load_tex(url: str) -> list[list[tuple[int, int, int, int]]] | None:
	path = static_path(url)
	if not path.is_file():
		return None
	return read_png(path)


def write_iso_icon(name: str, top_url: str, left_url: str, right_url: str) -> str | None:
	top = load_tex(top_url)
	left = load_tex(left_url)
	right = load_tex(right_url)
	if not top or not left or not right:
		return None
	url = f"/mc/iso/{name}.png"
	write_png(static_path(url), render_iso(top, left, right))
	return url


def stack_v(
	top: list[list[tuple[int, int, int, int]]], bottom: list[list[tuple[int, int, int, int]]]
) -> list[list[tuple[int, int, int, int]]]:
	return top + bottom


def render_chest_icon(name: str, sheet_url: str) -> str | None:
	sheet = load_tex(sheet_url)
	if not sheet or len(sheet) < 43 or len(sheet[0]) < 42:
		return None
	top = scale_nn(crop_px(sheet, 14, 0, 14, 14), 16, 16)
	front = scale_nn(stack_v(crop_px(sheet, 14, 14, 14, 5), crop_px(sheet, 14, 33, 14, 10)), 16, 16)
	side = scale_nn(stack_v(crop_px(sheet, 0, 14, 14, 5), crop_px(sheet, 0, 33, 14, 10)), 16, 16)
	url = f"/mc/iso/{name}.png"
	write_png(static_path(url), render_iso(top, front, side))
	return url


def render_shield_icon() -> str | None:
	sheet = load_tex("/mc/entity/shield.png")
	if not sheet or len(sheet) < 22 or len(sheet[0]) < 12:
		return None
	wood = scale_nn(crop_px(sheet, 2, 2, 10, 20), 20, 28)
	canvas = [[(0, 0, 0, 0) for _ in range(32)] for _ in range(32)]
	for y in range(28):
		if y < 18:
			half = 10
		else:
			half = max(1, int(10 * (1 - (y - 18) / 10)))
		for x in range(10 - half, 10 + half):
			if 0 <= x < 20:
				put_pixel(canvas, 6 + x, 2 + y, wood[y][x])
	url = "/mc/iso/shield.png"
	write_png(static_path(url), canvas)
	return url


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


def load_en_us(jar: zipfile.ZipFile, version: str) -> dict[str, str]:
	data = load_json(jar, "assets/minecraft/lang/en_us.json")
	if not isinstance(data, dict) or not data:
		data = download_lang_asset(version, "en_us.json")
	if not isinstance(data, dict):
		return {}
	return {key: value for key, value in data.items() if isinstance(key, str) and isinstance(value, str)}


def invicon_titles(item_id: str, lang: dict[str, str]) -> list[str]:
	name = item_id.replace("minecraft:", "")
	titles: list[str] = []
	for key in (f"item.minecraft.{name}", f"block.minecraft.{name}"):
		value = lang.get(key)
		if value and value not in titles:
			titles.append(value)
	pretty = name.replace("_", " ").title()
	if pretty not in titles:
		titles.append(pretty)
	return titles


def fetch_png(url: str) -> bytes | None:
	for attempt in range(5):
		request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
		try:
			with urllib.request.urlopen(request, timeout=25) as response:
				data = response.read()
			if data[:8] == b"\x89PNG\r\n\x1a\n":
				return data
		except (urllib.error.URLError, TimeoutError, OSError):
			pass
		time.sleep(0.6 * (attempt + 1))
	return None


def install_cached_invicon(name: str) -> str | None:
	url = f"/mc/inv/{name}.png"
	dest = static_path(url)
	if dest.is_file():
		return url
	cache = INV_CACHE / f"{name}.png"
	if cache.is_file():
		dest.parent.mkdir(parents=True, exist_ok=True)
		dest.write_bytes(cache.read_bytes())
		return url
	return None


def download_invicon(item_id: str, lang: dict[str, str]) -> str | None:
	name = item_id.replace("minecraft:", "")
	existing = install_cached_invicon(name)
	if existing:
		return existing
	for title in invicon_titles(item_id, lang):
		filename = "Invicon_" + title.replace(" ", "_") + ".png"
		data = fetch_png(WIKI_FILE + urllib.parse.quote(filename))
		if not data:
			continue
		cache = INV_CACHE / f"{name}.png"
		cache.parent.mkdir(parents=True, exist_ok=True)
		cache.write_bytes(data)
		return install_cached_invicon(name)
	return None


def download_invicons(item_ids: list[str], lang: dict[str, str]) -> dict[str, str]:
	found: dict[str, str] = {}
	pending: list[str] = []
	for item_id in item_ids:
		name = item_id.replace("minecraft:", "")
		cached = install_cached_invicon(name)
		if cached:
			found[item_id] = cached
		else:
			pending.append(item_id)
	if not pending:
		return found
	print(f"Downloading {len(pending)} inventory icons from minecraft.wiki…")
	with ThreadPoolExecutor(max_workers=4) as pool:
		futures = {pool.submit(download_invicon, item_id, lang): item_id for item_id in pending}
		for future in as_completed(futures):
			item_id = futures[future]
			try:
				url = future.result()
			except Exception:
				url = None
			if url:
				found[item_id] = url
	print(f"Got {len(found)} inventory icons")
	return found


def resolve_item(jar: zipfile.ZipFile, item_id: str, data: dict) -> str | None:
	name = item_id.replace("minecraft:", "")
	if file_exists(f"/mc/item/{name}.png"):
		return f"/mc/item/{name}.png"
	model = first_string_model(data)
	if model:
		textures = textures_from_model(jar, model, set())
		resolved = resolve_refs(textures)
		if "layer0" in resolved:
			url = tex_url(resolved["layer0"])
			if url and file_exists(url):
				return url
		if item_models_are_cube(jar, data):
			faces = cube_faces(textures)
			if faces:
				urls = [tex_url(face) for face in faces]
				if all(url and file_exists(url) for url in urls):
					iso = write_iso_icon(name, urls[0], urls[1], urls[2])
					if iso:
						return iso
	if file_exists(f"/mc/item/{name}_spawn_egg.png"):
		return f"/mc/item/{name}_spawn_egg.png"
	return None


def special_icon(item_id: str) -> str | None:
	if item_id == "minecraft:shield":
		return render_shield_icon()
	chests = {
		"minecraft:chest": "/mc/entity/chest.png",
		"minecraft:trapped_chest": "/mc/entity/trapped_chest.png",
		"minecraft:ender_chest": "/mc/entity/ender_chest.png",
		"minecraft:copper_chest": "/mc/entity/copper_chest.png",
	}
	sheet = chests.get(item_id)
	if sheet:
		return render_chest_icon(item_id.replace("minecraft:", ""), sheet)
	if item_id == "minecraft:shulker_box" and file_exists("/mc/block/shulker_box.png"):
		return write_iso_icon(
			"shulker_box",
			"/mc/block/shulker_box.png",
			"/mc/block/shulker_box.png",
			"/mc/block/shulker_box.png",
		)
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
		elif name == "assets/minecraft/textures/entity/chest/copper.png":
			dest = STATIC / "entity" / "copper_chest.png"
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
			f"/mc/inv/{name}.png",
			f"/mc/iso/{name}.png",
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
	parser.add_argument("--lang-only", action="store_true")
	args = parser.parse_args()
	jar_path = resolve_jar(args.version, args.jar)
	with zipfile.ZipFile(jar_path) as jar:
		if args.lang_only:
			write_advancement_lang(jar, args.version)
			return
		copy_pngs(jar)
		en_us = load_en_us(jar, args.version)
		icons: dict[str, str] = {}
		pending: list[str] = []
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
			else:
				pending.append(item_id)
		icons.update(download_invicons(pending, en_us))
		specials = {
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
			"minecraft:decorated_pot": "/mc/item/flower_pot.png",
			"minecraft:player_head": "/mc/item/player_head.png",
			"minecraft:potatoes": "/mc/item/potato.png",
			"minecraft:carrots": "/mc/item/carrot.png",
			"minecraft:beetroots": "/mc/item/beetroot.png",
		}
		for item_id in (
			"minecraft:shield",
			"minecraft:chest",
			"minecraft:trapped_chest",
			"minecraft:ender_chest",
			"minecraft:copper_chest",
			"minecraft:shulker_box",
		):
			if item_id in icons:
				continue
			url = special_icon(item_id)
			if url:
				icons[item_id] = url
		for png in (STATIC / "block").glob("*_shulker_box.png"):
			item_id = "minecraft:" + png.stem
			if item_id in icons:
				continue
			url = write_iso_icon(
				png.stem,
				f"/mc/block/{png.name}",
				f"/mc/block/{png.name}",
				f"/mc/block/{png.name}",
			)
			if url:
				icons[item_id] = url
		conduit = write_iso_icon(
			"conduit",
			"/mc/block/conduit.png",
			"/mc/block/conduit.png",
			"/mc/block/conduit.png",
		)
		if conduit:
			specials["minecraft:conduit"] = conduit
		for item_id, url in specials.items():
			if item_id not in icons and file_exists(url):
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
		write_advancement_lang(jar, args.version)
		print(f"Wrote {len(icons)} icons and textures to {STATIC}")


def lang_entries(data) -> dict[str, str]:
	if not isinstance(data, dict):
		return {}
	return {
		key: value
		for key, value in data.items()
		if isinstance(key, str) and key.startswith("advancements.") and isinstance(value, str)
	}


def write_advancement_lang(jar: zipfile.ZipFile, version: str) -> None:
	body: dict[str, dict[str, str]] = {}
	for locale, path in LANG_FILES.items():
		data = load_json(jar, path)
		entries = lang_entries(data)
		if not entries:
			entries = lang_entries(download_lang_asset(version, Path(path).name))
		body[locale] = entries
	ADVANCEMENT_LANG.parent.mkdir(parents=True, exist_ok=True)
	ADVANCEMENT_LANG.write_text(json.dumps(body, indent="\t", ensure_ascii=False, sort_keys=True) + "\n", encoding="utf-8")
	print(f"Wrote advancement lang to {ADVANCEMENT_LANG}")


if __name__ == "__main__":
	main()
