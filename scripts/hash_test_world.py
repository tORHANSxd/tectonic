#!/usr/bin/env python3
"""Create and compare canonical Minecraft 1.20.1 terrain snapshots."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import struct
import sys
import time
import zlib
from collections.abc import Iterable
from pathlib import Path

from analyze_ore_distribution import (
    DATA_VERSION_1_20_1,
    FormatError,
    RegionFile,
    region_path,
    unpack_palette_indices,
)


SCHEMA = "tectonic-terrain-snapshot-v1"
CHUNK_MAGIC = b"tectonic-terrain-chunk\x00\x01"
WORLD_MAGIC = b"tectonic-terrain-world\x00\x01"
INCLUDES = ("block_states", "biomes")
EXCLUDES = (
    "LastUpdate",
    "InhabitedTime",
    "block_ticks",
    "fluid_ticks",
    "lighting",
    "Heightmaps",
    "PostProcessing",
    "structures_metadata",
    "block_entities",
    "entities",
    "all_other_chunk_fields",
)


def _int(value, label: str) -> int:
    if type(value) is not int:
        raise FormatError(f"{label} 必须是整数")
    return value


def _text(value, label: str) -> bytes:
    if not isinstance(value, str):
        raise FormatError(f"{label} 必须是字符串")
    return value.encode("utf-8")


def _sized(value: bytes) -> bytes:
    return struct.pack(">I", len(value)) + value


def _sha256_text(value, label: str) -> str:
    if (
        not isinstance(value, str)
        or len(value) != 64
        or any(character not in "0123456789abcdef" for character in value)
    ):
        raise FormatError(f"{label} 必须是小写 SHA-256")
    return value


def _block_state(entry, label: str) -> bytes:
    if not isinstance(entry, dict):
        raise FormatError(f"{label} 必须是 compound")
    unexpected = set(entry) - {"Name", "Properties"}
    if unexpected:
        raise FormatError(f"{label} 包含未知字段: {sorted(unexpected)}")

    encoded = bytearray(b"B")
    encoded.extend(_sized(_text(entry.get("Name"), f"{label}.Name")))
    properties = entry.get("Properties", {})
    if not isinstance(properties, dict):
        raise FormatError(f"{label}.Properties 必须是 compound")
    items = []
    for key, value in properties.items():
        key_bytes = _text(key, f"{label}.Properties key")
        value_bytes = _text(value, f"{label}.Properties[{key!r}]")
        items.append((key_bytes, value_bytes))
    items.sort(key=lambda item: item[0])
    encoded.extend(struct.pack(">H", len(items)))
    for key_bytes, value_bytes in items:
        encoded.extend(_sized(key_bytes))
        encoded.extend(_sized(value_bytes))
    return bytes(encoded)


def _biome(entry, label: str) -> bytes:
    return b"M" + _sized(_text(entry, label))


def _paletted_container(
    container,
    *,
    label: str,
    value_count: int,
    min_bits: int,
    entry_encoder,
) -> bytes:
    if not isinstance(container, dict):
        raise FormatError(f"{label} 必须是 compound")
    unexpected = set(container) - {"palette", "data"}
    if unexpected:
        raise FormatError(f"{label} 包含未知字段: {sorted(unexpected)}")
    palette = container.get("palette")
    if not isinstance(palette, list):
        raise FormatError(f"{label}.palette 必须是 list")

    encoded_entries = [
        entry_encoder(entry, f"{label}.palette[{index}]")
        for index, entry in enumerate(palette)
    ]
    indices = unpack_palette_indices(
        len(encoded_entries),
        container.get("data"),
        value_count=value_count,
        min_bits=min_bits,
    )
    referenced_entries = sorted({encoded_entries[index] for index in indices})
    canonical_index = {
        encoded_entry: index for index, encoded_entry in enumerate(referenced_entries)
    }
    canonical_indices = [
        canonical_index[encoded_entries[index]] for index in indices
    ]

    encoded = bytearray()
    encoded.extend(struct.pack(">H", len(referenced_entries)))
    for encoded_entry in referenced_entries:
        encoded.extend(_sized(encoded_entry))
    if len(referenced_entries) == 1:
        encoded.append(0)
    elif len(referenced_entries) <= 256:
        encoded.append(1)
        encoded.extend(bytes(canonical_indices))
    else:
        encoded.append(2)
        for index in canonical_indices:
            encoded.extend(struct.pack(">H", index))
    return bytes(encoded)


def canonicalize_chunk(
    root,
    *,
    chunk_x: int,
    chunk_z: int,
    expected_min_y: int,
    expected_max_y: int,
    expected_data_version: int = DATA_VERSION_1_20_1,
) -> bytes:
    if not isinstance(root, dict):
        raise FormatError(f"区块 ({chunk_x}, {chunk_z}) 根 tag 必须是 compound")
    if expected_min_y % 16 or expected_max_y % 16:
        raise FormatError("expected min/max Y 必须是 16 的倍数")
    if expected_min_y >= expected_max_y:
        raise FormatError("expected min Y 必须小于 max Y")

    actual_x = _int(root.get("xPos"), f"区块 ({chunk_x}, {chunk_z}) xPos")
    actual_z = _int(root.get("zPos"), f"区块 ({chunk_x}, {chunk_z}) zPos")
    if actual_x != chunk_x or actual_z != chunk_z:
        raise FormatError(
            f"区块坐标不匹配: 请求 ({chunk_x}, {chunk_z})，NBT=({actual_x}, {actual_z})"
        )
    if root.get("Status") != "minecraft:full":
        raise FormatError(
            f"区块 ({chunk_x}, {chunk_z}) Status={root.get('Status')!r}，不是 full"
        )
    data_version = _int(
        root.get("DataVersion"), f"区块 ({chunk_x}, {chunk_z}) DataVersion"
    )
    if data_version != expected_data_version:
        raise FormatError(
            f"区块 ({chunk_x}, {chunk_z}) DataVersion={data_version}，"
            f"预期 {expected_data_version}"
        )
    expected_y_pos = expected_min_y // 16
    y_pos = _int(root.get("yPos"), f"区块 ({chunk_x}, {chunk_z}) yPos")
    if y_pos != expected_y_pos:
        raise FormatError(
            f"区块 ({chunk_x}, {chunk_z}) yPos={y_pos}，预期 {expected_y_pos}"
        )

    sections = root.get("sections")
    if not isinstance(sections, list):
        raise FormatError(f"区块 ({chunk_x}, {chunk_z}) 缺少 sections")
    expected_sections = set(range(expected_y_pos, expected_max_y // 16))
    canonical_sections = {}
    seen_sections = set()
    for section in sections:
        if not isinstance(section, dict):
            raise FormatError(f"区块 ({chunk_x}, {chunk_z}) section 必须是 compound")
        section_y = _int(
            section.get("Y"), f"区块 ({chunk_x}, {chunk_z}) section.Y"
        )
        if section_y in seen_sections:
            raise FormatError(
                f"区块 ({chunk_x}, {chunk_z}) section Y={section_y} 重复"
            )
        seen_sections.add(section_y)
        if section_y not in expected_sections:
            if set(section) <= {"Y", "BlockLight", "SkyLight"}:
                continue
            raise FormatError(
                f"区块 ({chunk_x}, {chunk_z}) 出现范围外 section Y={section_y}"
            )
        block_states = _paletted_container(
            section.get("block_states"),
            label=f"区块 ({chunk_x}, {chunk_z}) section Y={section_y} block_states",
            value_count=4096,
            min_bits=4,
            entry_encoder=_block_state,
        )
        biomes = _paletted_container(
            section.get("biomes"),
            label=f"区块 ({chunk_x}, {chunk_z}) section Y={section_y} biomes",
            value_count=64,
            min_bits=1,
            entry_encoder=_biome,
        )
        canonical_sections[section_y] = (block_states, biomes)

    if set(canonical_sections) != expected_sections:
        missing = sorted(expected_sections - set(canonical_sections))
        raise FormatError(
            f"区块 ({chunk_x}, {chunk_z}) section 范围不完整，缺少 {missing}"
        )

    encoded = bytearray(CHUNK_MAGIC)
    encoded.extend(
        struct.pack(
            ">iiiiiI",
            data_version,
            chunk_x,
            chunk_z,
            expected_min_y,
            expected_max_y,
            len(canonical_sections),
        )
    )
    for section_y in sorted(canonical_sections):
        block_states, biomes = canonical_sections[section_y]
        encoded.extend(struct.pack(">i", section_y))
        encoded.extend(_sized(block_states))
        encoded.extend(_sized(biomes))
    return bytes(encoded)


def _hash_chunk_digests(
    chunks: Iterable[tuple[int, int, str]],
) -> tuple[str, list[dict]]:
    ordered = sorted(chunks, key=lambda item: (item[1], item[0]))
    seen = set()
    digest = hashlib.sha256()
    digest.update(WORLD_MAGIC)
    digest.update(struct.pack(">I", len(ordered)))
    details = []
    for chunk_x, chunk_z, chunk_hash in ordered:
        coordinate = (chunk_x, chunk_z)
        if coordinate in seen:
            raise FormatError(f"重复区块: ({chunk_x}, {chunk_z})")
        seen.add(coordinate)
        chunk_hash = _sha256_text(chunk_hash, f"区块 {coordinate} terrain_sha256")
        chunk_digest = bytes.fromhex(chunk_hash)
        digest.update(struct.pack(">ii", chunk_x, chunk_z))
        digest.update(chunk_digest)
        details.append(
            {"x": chunk_x, "z": chunk_z, "terrain_sha256": chunk_hash}
        )
    return digest.hexdigest(), details


def hash_chunks(chunks: Iterable[tuple[int, int, bytes]]) -> tuple[str, list[dict]]:
    chunk_digests = []
    for chunk_x, chunk_z, canonical in chunks:
        if not isinstance(canonical, bytes):
            raise TypeError("canonical chunk 必须是 bytes")
        chunk_digests.append(
            (chunk_x, chunk_z, hashlib.sha256(canonical).hexdigest())
        )
    return _hash_chunk_digests(chunk_digests)


def scan_world(
    world: Path,
    bounds: tuple[int, int, int, int],
    *,
    expected_min_y: int,
    expected_max_y: int,
    expected_data_version: int = DATA_VERSION_1_20_1,
) -> dict:
    world = world.resolve()
    region_root = world / "region"
    if not region_root.is_dir():
        raise FormatError(f"找不到主世界 region 目录: {region_root}")
    min_x, min_z, max_x, max_z = bounds
    if min_x > max_x or min_z > max_z:
        raise FormatError("chunks 必须按 minX minZ maxX maxZ 给出闭区间")

    readers: dict[Path, RegionFile] = {}
    chunk_digests = []
    try:
        for chunk_z in range(min_z, max_z + 1):
            for chunk_x in range(min_x, max_x + 1):
                path = region_path(region_root, chunk_x, chunk_z)
                if path not in readers:
                    if not path.is_file():
                        raise FormatError(f"找不到 region 文件: {path}")
                    readers[path] = RegionFile(path)
                root = readers[path].read_chunk(chunk_x, chunk_z)
                canonical = canonicalize_chunk(
                    root,
                    chunk_x=chunk_x,
                    chunk_z=chunk_z,
                    expected_min_y=expected_min_y,
                    expected_max_y=expected_max_y,
                    expected_data_version=expected_data_version,
                )
                chunk_digests.append(
                    (
                        chunk_x,
                        chunk_z,
                        hashlib.sha256(canonical).hexdigest(),
                    )
                )
    finally:
        for reader in readers.values():
            reader.close()

    terrain_sha256, chunk_hashes = _hash_chunk_digests(chunk_digests)
    return {
        "schema": SCHEMA,
        "world": str(world),
        "chunk_bounds_inclusive": list(bounds),
        "chunks": len(chunk_digests),
        "data_version": expected_data_version,
        "min_y": expected_min_y,
        "max_y": expected_max_y,
        "includes": list(INCLUDES),
        "excludes": list(EXCLUDES),
        "terrain_sha256": terrain_sha256,
        "chunk_hashes": chunk_hashes,
    }


def _load_snapshot(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise FormatError(f"快照根对象必须是 object: {path}")
    return value


def _validated_snapshot_chunks(snapshot: dict, label: str) -> dict[tuple[int, int], str]:
    if snapshot.get("schema") != SCHEMA:
        raise FormatError(f"{label} 只支持 {SCHEMA}")
    if snapshot.get("includes") != list(INCLUDES):
        raise FormatError(f"{label}.includes 与 {SCHEMA} 不匹配")
    if snapshot.get("excludes") != list(EXCLUDES):
        raise FormatError(f"{label}.excludes 与 {SCHEMA} 不匹配")
    bounds = snapshot.get("chunk_bounds_inclusive")
    if (
        not isinstance(bounds, list)
        or len(bounds) != 4
        or any(type(value) is not int for value in bounds)
    ):
        raise FormatError(f"{label}.chunk_bounds_inclusive 非法")
    min_x, min_z, max_x, max_z = bounds
    if min_x > max_x or min_z > max_z:
        raise FormatError(f"{label}.chunk_bounds_inclusive 不是有效闭区间")
    data_version = _int(snapshot.get("data_version"), f"{label}.data_version")
    if data_version < 1:
        raise FormatError(f"{label}.data_version 必须为正数")
    min_y = _int(snapshot.get("min_y"), f"{label}.min_y")
    max_y = _int(snapshot.get("max_y"), f"{label}.max_y")
    if min_y % 16 or max_y % 16 or min_y >= max_y:
        raise FormatError(f"{label} 的 min_y/max_y 非法")
    expected_count = (max_x - min_x + 1) * (max_z - min_z + 1)
    if _int(snapshot.get("chunks"), f"{label}.chunks") != expected_count:
        raise FormatError(f"{label}.chunks 与闭区间面积不匹配")

    rows = snapshot.get("chunk_hashes")
    if not isinstance(rows, list):
        raise FormatError(f"{label}.chunk_hashes 必须是 list")
    result = {}
    digests = []
    for row in rows:
        if not isinstance(row, dict):
            raise FormatError(f"{label}.chunk_hashes 条目必须是 object")
        coordinate = (
            _int(row.get("x"), f"{label}.x"),
            _int(row.get("z"), f"{label}.z"),
        )
        if not (min_x <= coordinate[0] <= max_x and min_z <= coordinate[1] <= max_z):
            raise FormatError(f"{label} 包含闭区间外区块 {coordinate}")
        terrain_hash = _sha256_text(
            row.get("terrain_sha256"), f"{label} 区块 {coordinate} terrain_sha256"
        )
        if coordinate in result:
            raise FormatError(f"{label} 包含重复区块 {coordinate}")
        result[coordinate] = terrain_hash
        digests.append((coordinate[0], coordinate[1], terrain_hash))
    if len(result) != expected_count:
        for chunk_z in range(min_z, max_z + 1):
            for chunk_x in range(min_x, max_x + 1):
                if (chunk_x, chunk_z) not in result:
                    raise FormatError(f"{label} 缺少区块 ({chunk_x}, {chunk_z})")

    calculated_hash, _ = _hash_chunk_digests(digests)
    declared_hash = _sha256_text(
        snapshot.get("terrain_sha256"), f"{label}.terrain_sha256"
    )
    if declared_hash != calculated_hash:
        raise FormatError(
            f"{label}.terrain_sha256 与逐区块哈希不一致: "
            f"{declared_hash} != {calculated_hash}"
        )
    return result


def compare_snapshots(left: dict, right: dict) -> list[dict]:
    left_chunks = _validated_snapshot_chunks(left, "left")
    right_chunks = _validated_snapshot_chunks(right, "right")
    metadata = (
        "schema",
        "chunk_bounds_inclusive",
        "chunks",
        "data_version",
        "min_y",
        "max_y",
        "includes",
        "excludes",
    )
    for field in metadata:
        if left.get(field) != right.get(field):
            raise FormatError(
                f"快照元数据 {field} 不匹配: {left.get(field)!r} != {right.get(field)!r}"
            )

    left_world_hash = left["terrain_sha256"]
    right_world_hash = right["terrain_sha256"]
    differences = []
    for coordinate in sorted(
        set(left_chunks) | set(right_chunks), key=lambda item: (item[1], item[0])
    ):
        left_chunk_hash = left_chunks.get(coordinate)
        right_chunk_hash = right_chunks.get(coordinate)
        if left_chunk_hash != right_chunk_hash:
            differences.append(
                {
                    "x": coordinate[0],
                    "z": coordinate[1],
                    "left": left_chunk_hash,
                    "right": right_chunk_hash,
                }
            )
    hash_equal = left_world_hash == right_world_hash
    if hash_equal != (not differences):
        raise FormatError("总 terrain_sha256 与逐区块比较结果矛盾")
    return differences


def scan_command(args: argparse.Namespace) -> int:
    started = time.perf_counter()
    snapshot = scan_world(
        args.world,
        tuple(args.chunks),
        expected_min_y=args.expected_min_y,
        expected_max_y=args.expected_max_y,
        expected_data_version=args.expected_data_version,
    )
    snapshot["elapsed_seconds"] = round(time.perf_counter() - started, 3)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(
        f"{snapshot['chunks']} chunks, terrain_sha256={snapshot['terrain_sha256']}, "
        f"snapshot={args.output}"
    )
    return 0


def compare_command(args: argparse.Namespace) -> int:
    differences = compare_snapshots(
        _load_snapshot(args.left), _load_snapshot(args.right)
    )
    if not differences:
        print("terrain snapshots are identical")
        return 0
    print(f"terrain snapshots differ in {len(differences)} chunk(s)", file=sys.stderr)
    for difference in differences[: args.max_differences]:
        print(
            f"  ({difference['x']}, {difference['z']}): "
            f"{difference['left']} != {difference['right']}",
            file=sys.stderr,
        )
    return 1


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)

    scan = commands.add_parser("scan", help="哈希一个已停止的 1.20.1 Overworld")
    scan.add_argument("world", type=Path)
    scan.add_argument(
        "--chunks",
        type=int,
        nargs=4,
        metavar=("MIN_X", "MIN_Z", "MAX_X", "MAX_Z"),
        required=True,
    )
    scan.add_argument("--expected-min-y", type=int, required=True)
    scan.add_argument("--expected-max-y", type=int, default=320)
    scan.add_argument(
        "--expected-data-version", type=int, default=DATA_VERSION_1_20_1
    )
    scan.add_argument("--output", type=Path, required=True)
    scan.set_defaults(func=scan_command)

    compare = commands.add_parser("compare", help="严格比较两个 terrain snapshot")
    compare.add_argument("left", type=Path)
    compare.add_argument("right", type=Path)
    compare.add_argument("--max-differences", type=int, default=20)
    compare.set_defaults(func=compare_command)
    return root


def main() -> int:
    try:
        args = parser().parse_args()
        if getattr(args, "max_differences", 1) < 1:
            raise FormatError("max-differences 必须大于零")
        return args.func(args)
    except (
        FormatError,
        OSError,
        ValueError,
        TypeError,
        UnicodeError,
        struct.error,
        zlib.error,
        gzip.BadGzipFile,
    ) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
