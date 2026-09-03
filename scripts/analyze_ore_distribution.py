#!/usr/bin/env python3
"""Strict Minecraft 1.20.1 Anvil ore counter and CSV/SVG report generator."""

from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import json
import math
import statistics
import struct
import sys
import time
import zlib
from collections import Counter, defaultdict
from pathlib import Path
from xml.sax.saxutils import escape


DATA_VERSION_1_20_1 = 3465
MASK_64 = (1 << 64) - 1
ORE_BLOCKS = {
    "coal": ("minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
    "iron": ("minecraft:iron_ore", "minecraft:deepslate_iron_ore"),
    "copper": ("minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
    "gold": ("minecraft:gold_ore", "minecraft:deepslate_gold_ore"),
    "redstone": ("minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
    "lapis": ("minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"),
    "diamond": ("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
    "emerald": ("minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
}
MATERIAL_BLOCKS = {
    "gravel": ("minecraft:gravel",),
    "tuff": ("minecraft:tuff",),
}
CSV_FIELDS = (
    "case_id",
    "scenario",
    "seed",
    "min_y",
    "max_y",
    "mod_enabled",
    "ore_fix",
    "data_version",
    "chunks",
    "scope",
    "family",
    "block",
    "y_min",
    "y_max_exclusive",
    "count",
    "count_per_chunk",
)


class FormatError(RuntimeError):
    pass


class NbtReader:
    def __init__(self, data: bytes):
        self.data = data
        self.offset = 0

    def read(self, size: int) -> bytes:
        end = self.offset + size
        if size < 0 or end > len(self.data):
            raise FormatError("NBT 数据被截断")
        value = self.data[self.offset:end]
        self.offset = end
        return value

    def unpack(self, pattern: str):
        size = struct.calcsize(pattern)
        return struct.unpack(pattern, self.read(size))[0]

    def string(self) -> str:
        size = self.unpack(">H")
        return self.read(size).decode("utf-8", errors="replace")

    def length(self, kind: str) -> int:
        value = self.unpack(">i")
        if value < 0:
            raise FormatError(f"NBT {kind} 长度为负数: {value}")
        return value

    def payload(self, tag_type: int):
        if tag_type == 1:
            return self.unpack(">b")
        if tag_type == 2:
            return self.unpack(">h")
        if tag_type == 3:
            return self.unpack(">i")
        if tag_type == 4:
            return self.unpack(">q")
        if tag_type == 5:
            return self.unpack(">f")
        if tag_type == 6:
            return self.unpack(">d")
        if tag_type == 7:
            return self.read(self.length("byte array"))
        if tag_type == 8:
            return self.string()
        if tag_type == 9:
            item_type = self.unpack(">B")
            return [self.payload(item_type) for _ in range(self.length("list"))]
        if tag_type == 10:
            value = {}
            while True:
                child_type = self.unpack(">B")
                if child_type == 0:
                    return value
                child_name = self.string()
                value[child_name] = self.payload(child_type)
        if tag_type == 11:
            size = self.length("int array")
            return list(struct.unpack(f">{size}i", self.read(size * 4))) if size else []
        if tag_type == 12:
            size = self.length("long array")
            return list(struct.unpack(f">{size}q", self.read(size * 8))) if size else []
        raise FormatError(f"不支持的 NBT tag 类型: {tag_type}")


def parse_nbt(data: bytes) -> dict:
    reader = NbtReader(data)
    root_type = reader.unpack(">B")
    if root_type != 10:
        raise FormatError(f"NBT 根 tag 不是 compound: {root_type}")
    reader.string()
    root = reader.payload(root_type)
    if reader.offset != len(data):
        raise FormatError(f"NBT 根 tag 后有 {len(data) - reader.offset} 个多余字节")
    return root


class RegionFile:
    def __init__(self, path: Path):
        self.path = path
        self.stream = path.open("rb")
        self.header = self.stream.read(4096)
        if len(self.header) != 4096:
            self.close()
            raise FormatError(f"Region 头部被截断: {path}")

    def close(self) -> None:
        self.stream.close()

    def read_chunk(self, chunk_x: int, chunk_z: int) -> dict:
        slot = (chunk_x & 31) + (chunk_z & 31) * 32
        location = int.from_bytes(self.header[slot * 4 : slot * 4 + 4], "big")
        sector_offset = location >> 8
        sector_count = location & 0xFF
        if sector_offset == 0 or sector_count == 0:
            raise FormatError(f"缺少区块 ({chunk_x}, {chunk_z})，region={self.path.name}")

        self.stream.seek(sector_offset * 4096)
        length_bytes = self.stream.read(4)
        if len(length_bytes) != 4:
            raise FormatError(f"区块 ({chunk_x}, {chunk_z}) 长度字段被截断")
        length = int.from_bytes(length_bytes, "big")
        if length < 1 or length > sector_count * 4096 - 4:
            raise FormatError(
                f"区块 ({chunk_x}, {chunk_z}) 长度非法: {length}, sectors={sector_count}"
            )
        compression_raw = self.stream.read(1)
        payload = self.stream.read(length - 1)
        if len(compression_raw) != 1 or len(payload) != length - 1:
            raise FormatError(f"区块 ({chunk_x}, {chunk_z}) 数据被截断")

        compression = compression_raw[0]
        if compression & 0x80:
            raise FormatError(f"暂不支持外置 .mcc 区块 ({chunk_x}, {chunk_z})")
        compression &= 0x7F
        if compression == 1:
            unpacked = gzip.decompress(payload)
        elif compression == 2:
            unpacked = zlib.decompress(payload)
        elif compression == 3:
            unpacked = payload
        else:
            raise FormatError(f"区块 ({chunk_x}, {chunk_z}) 使用未知压缩类型 {compression}")
        return parse_nbt(unpacked)


def unpack_palette_frequencies(palette_size: int, packed: list[int] | None) -> list[int]:
    if palette_size < 1:
        raise FormatError("block_states.palette 为空")
    if palette_size == 1:
        if packed:
            raise FormatError("单值 palette 不应包含 data")
        return [4096]
    if not packed:
        raise FormatError("多值 palette 缺少 data")

    bits = max(4, (palette_size - 1).bit_length())
    values_per_long = 64 // bits
    required_longs = math.ceil(4096 / values_per_long)
    if len(packed) != required_longs:
        raise FormatError(
            f"palette={palette_size} 需要 {required_longs} 个 long，实际 {len(packed)}"
        )

    mask = (1 << bits) - 1
    frequencies = [0] * palette_size
    remaining = 4096
    for signed_word in packed:
        word = signed_word & MASK_64
        entries = min(values_per_long, remaining)
        for _ in range(entries):
            palette_index = word & mask
            if palette_index >= palette_size:
                raise FormatError(
                    f"palette 索引 {palette_index} 超出 palette={palette_size}"
                )
            frequencies[palette_index] += 1
            word >>= bits
        remaining -= entries
    if remaining != 0:
        raise FormatError(f"palette data 少解码 {remaining} 个方块")
    return frequencies


def bool_text(value: str) -> str:
    lowered = value.lower()
    if lowered not in {"true", "false"}:
        raise argparse.ArgumentTypeError("必须是 true 或 false")
    return lowered


def region_path(region_root: Path, chunk_x: int, chunk_z: int) -> Path:
    return region_root / f"r.{chunk_x // 32}.{chunk_z // 32}.mca"


def write_csv(path: Path, rows: list[dict], fields=CSV_FIELDS) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def scan_world(args: argparse.Namespace) -> int:
    started = time.perf_counter()
    world = args.world.resolve()
    region_root = world / "region"
    if not region_root.is_dir():
        raise FormatError(f"找不到主世界 region 目录: {region_root}")
    if args.expected_min_y % 16 or args.expected_max_y % 16:
        raise FormatError("expected min/max Y 必须是 16 的倍数")
    if args.expected_min_y >= args.expected_max_y:
        raise FormatError("expected min Y 必须小于 max Y")

    min_x, min_z, max_x, max_z = args.chunks
    if min_x > max_x or min_z > max_z:
        raise FormatError("chunks 必须按 minX minZ maxX maxZ 给出闭区间")
    expected_sections = set(range(args.expected_min_y // 16, args.expected_max_y // 16))
    families = dict(ORE_BLOCKS)
    if args.include_materials:
        families.update(MATERIAL_BLOCKS)
    block_to_family = {
        block: family for family, blocks in families.items() for block in blocks
    }
    counts: Counter[tuple[str, str, int]] = Counter()
    versions: set[int] = set()
    readers: dict[Path, RegionFile] = {}
    chunks = 0

    try:
        for chunk_z in range(min_z, max_z + 1):
            for chunk_x in range(min_x, max_x + 1):
                path = region_path(region_root, chunk_x, chunk_z)
                if path not in readers:
                    if not path.is_file():
                        raise FormatError(f"找不到 region 文件: {path}")
                    readers[path] = RegionFile(path)
                root = readers[path].read_chunk(chunk_x, chunk_z)
                if root.get("xPos") != chunk_x or root.get("zPos") != chunk_z:
                    raise FormatError(
                        f"区块坐标不匹配: 请求 ({chunk_x}, {chunk_z})，"
                        f"NBT=({root.get('xPos')}, {root.get('zPos')})"
                    )
                if root.get("Status") != "minecraft:full":
                    raise FormatError(
                        f"区块 ({chunk_x}, {chunk_z}) Status={root.get('Status')!r}，不是 full"
                    )
                version = root.get("DataVersion")
                if version != args.expected_data_version:
                    raise FormatError(
                        f"区块 ({chunk_x}, {chunk_z}) DataVersion={version}，"
                        f"预期 {args.expected_data_version}"
                    )
                versions.add(version)

                sections = root.get("sections")
                if not isinstance(sections, list):
                    raise FormatError(f"区块 ({chunk_x}, {chunk_z}) 缺少 sections")
                seen_sections = set()
                for section in sections:
                    section_y = section.get("Y")
                    if not isinstance(section_y, int):
                        raise FormatError(f"区块 ({chunk_x}, {chunk_z}) section 缺少 Y")
                    if section_y in seen_sections:
                        raise FormatError(
                            f"区块 ({chunk_x}, {chunk_z}) section Y={section_y} 重复"
                        )
                    seen_sections.add(section_y)
                    if section_y not in expected_sections:
                        raise FormatError(
                            f"区块 ({chunk_x}, {chunk_z}) 出现范围外 section Y={section_y}"
                        )
                    block_states = section.get("block_states")
                    if not isinstance(block_states, dict):
                        raise FormatError(
                            f"区块 ({chunk_x}, {chunk_z}) section Y={section_y} 缺少 block_states"
                        )
                    palette = block_states.get("palette")
                    if not isinstance(palette, list):
                        raise FormatError(
                            f"区块 ({chunk_x}, {chunk_z}) section Y={section_y} 缺少 palette"
                        )
                    names = [entry.get("Name") if isinstance(entry, dict) else None for entry in palette]
                    interesting = any(name in block_to_family for name in names)
                    if not interesting:
                        continue
                    frequencies = unpack_palette_frequencies(
                        len(palette), block_states.get("data")
                    )
                    bucket_y = section_y * 16
                    for index, block in enumerate(names):
                        family = block_to_family.get(block)
                        if family is not None:
                            counts[(family, block, bucket_y)] += frequencies[index]
                if seen_sections != expected_sections:
                    missing = sorted(expected_sections - seen_sections)
                    raise FormatError(
                        f"区块 ({chunk_x}, {chunk_z}) section 范围不完整，缺少 {missing}"
                    )
                chunks += 1
    finally:
        for reader in readers.values():
            reader.close()

    expected_chunks = (max_x - min_x + 1) * (max_z - min_z + 1)
    if chunks != expected_chunks:
        raise FormatError(f"统计了 {chunks} 个区块，预期 {expected_chunks}")

    common = {
        "case_id": args.case_id,
        "scenario": args.scenario,
        "seed": str(args.seed),
        "min_y": args.expected_min_y,
        "max_y": args.expected_max_y,
        "mod_enabled": args.mod_enabled,
        "ore_fix": args.ore_fix,
        "data_version": next(iter(versions)),
        "chunks": chunks,
    }
    rows = []
    for family, blocks in families.items():
        for y_min in range(args.expected_min_y, args.expected_max_y, 16):
            family_count = sum(counts[(family, block, y_min)] for block in blocks)
            rows.append(
                {
                    **common,
                    "scope": "family",
                    "family": family,
                    "block": "",
                    "y_min": y_min,
                    "y_max_exclusive": y_min + 16,
                    "count": family_count,
                    "count_per_chunk": f"{family_count / chunks:.9f}",
                }
            )
            for block in blocks:
                block_count = counts[(family, block, y_min)]
                rows.append(
                    {
                        **common,
                        "scope": "block",
                        "family": family,
                        "block": block,
                        "y_min": y_min,
                        "y_max_exclusive": y_min + 16,
                        "count": block_count,
                        "count_per_chunk": f"{block_count / chunks:.9f}",
                    }
                )
    write_csv(args.csv, rows)

    totals = {
        family: sum(counts[(family, block, y)] for block in blocks for y in range(args.expected_min_y, args.expected_max_y, 16))
        for family, blocks in families.items()
    }
    below_minus_64 = {
        family: sum(
            counts[(family, block, y)]
            for block in blocks
            for y in range(args.expected_min_y, min(-64, args.expected_max_y), 16)
        )
        for family, blocks in families.items()
    }
    summary_path = args.summary or args.csv.with_suffix(".json")
    summary = {
        "case_id": args.case_id,
        "scenario": args.scenario,
        "seed": str(args.seed),
        "world": str(world),
        "chunk_bounds_inclusive": [min_x, min_z, max_x, max_z],
        "chunks": chunks,
        "data_versions": sorted(versions),
        "min_y": args.expected_min_y,
        "max_y": args.expected_max_y,
        "mod_enabled": args.mod_enabled,
        "ore_fix": args.ore_fix,
        "totals": totals,
        "below_minus_64": below_minus_64,
        "csv": str(args.csv.resolve()),
        "csv_sha256": sha256(args.csv),
        "elapsed_seconds": round(time.perf_counter() - started, 3),
    }
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")
    print(
        f"{args.case_id}: {chunks} chunks, Y={args.expected_min_y}..{args.expected_max_y - 1}, "
        f"CSV={args.csv}"
    )
    return 0


def discover_csv(inputs: list[Path]) -> list[Path]:
    found = set()
    for item in inputs:
        if item.is_file():
            found.add(item.resolve())
        elif item.is_dir():
            found.update(path.resolve() for path in item.rglob("ore-counts.csv"))
        else:
            raise FormatError(f"输入不存在: {item}")
    if not found:
        raise FormatError("没有找到 ore-counts.csv")
    return sorted(found)


def mean(values: list[float]) -> float:
    return sum(values) / len(values)


def svg_chart(path: Path, title: str, series: dict[str, list[tuple[int, float]]]) -> None:
    width, height = 960, 540
    left, right, top, bottom = 78, 24, 52, 66
    plot_w, plot_h = width - left - right, height - top - bottom
    all_points = [point for points in series.values() for point in points]
    if not all_points:
        return
    xs = [point[0] for point in all_points]
    ys = [point[1] for point in all_points]
    x_min, x_max = min(xs), max(xs)
    y_max = max(ys) or 1.0
    if x_min == x_max:
        x_max += 1
    colors = ("#2563eb", "#dc2626", "#16a34a", "#9333ea", "#ea580c", "#0891b2")

    def sx(value: float) -> float:
        return left + (value - x_min) * plot_w / (x_max - x_min)

    def sy(value: float) -> float:
        return top + plot_h - value * plot_h / y_max

    lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">',
        '<rect width="100%" height="100%" fill="white"/>',
        f'<text x="{width / 2}" y="28" text-anchor="middle" font-family="sans-serif" font-size="18">{escape(title)}</text>',
    ]
    for tick in range(6):
        value = y_max * tick / 5
        y = sy(value)
        lines.append(f'<line x1="{left}" y1="{y:.2f}" x2="{width-right}" y2="{y:.2f}" stroke="#e5e7eb"/>')
        lines.append(f'<text x="{left-8}" y="{y+4:.2f}" text-anchor="end" font-family="sans-serif" font-size="11">{value:.2f}</text>')
    x_step = max(1, math.ceil((x_max - x_min) / 8 / 16)) * 16
    first_tick = math.ceil(x_min / x_step) * x_step
    for value in range(first_tick, x_max + 1, x_step):
        x = sx(value)
        lines.append(f'<line x1="{x:.2f}" y1="{top}" x2="{x:.2f}" y2="{top+plot_h}" stroke="#f3f4f6"/>')
        lines.append(f'<text x="{x:.2f}" y="{top+plot_h+20}" text-anchor="middle" font-family="sans-serif" font-size="11">{value}</text>')
    lines.extend(
        [
            f'<line x1="{left}" y1="{top+plot_h}" x2="{width-right}" y2="{top+plot_h}" stroke="#111827"/>',
            f'<line x1="{left}" y1="{top}" x2="{left}" y2="{top+plot_h}" stroke="#111827"/>',
            f'<text x="{left+plot_w/2}" y="{height-18}" text-anchor="middle" font-family="sans-serif" font-size="12">Y bucket minimum</text>',
            f'<text x="18" y="{top+plot_h/2}" text-anchor="middle" transform="rotate(-90 18 {top+plot_h/2})" font-family="sans-serif" font-size="12">mean blocks per chunk</text>',
        ]
    )
    for index, (label, points) in enumerate(sorted(series.items())):
        color = colors[index % len(colors)]
        coordinates = " ".join(f"{sx(x):.2f},{sy(y):.2f}" for x, y in sorted(points))
        lines.append(f'<polyline points="{coordinates}" fill="none" stroke="{color}" stroke-width="2"/>')
        legend_y = top + 16 + index * 18
        lines.append(f'<line x1="{left+12}" y1="{legend_y}" x2="{left+36}" y2="{legend_y}" stroke="{color}" stroke-width="3"/>')
        lines.append(f'<text x="{left+42}" y="{legend_y+4}" font-family="sans-serif" font-size="11">{escape(label)}</text>')
    lines.append("</svg>")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def aggregate(args: argparse.Namespace) -> int:
    csv_paths = discover_csv(args.input)
    rows = []
    for path in csv_paths:
        with path.open("r", encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream)
            if tuple(reader.fieldnames or ()) != CSV_FIELDS:
                raise FormatError(f"CSV 字段不匹配: {path}")
            rows.extend(reader)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    write_csv(output / "ore-distribution-raw.csv", rows)

    family_rows = [row for row in rows if row["scope"] == "family"]
    distribution_groups: dict[tuple[str, int, str, int], list[float]] = defaultdict(list)
    per_case_totals: dict[tuple[str, str, int, str], float] = defaultdict(float)
    per_case_below: dict[tuple[str, str, int, str], float] = defaultdict(float)
    case_vectors: dict[
        tuple[str, str, int, str, str, str, str], list[tuple[int, int]]
    ] = defaultdict(list)
    for row in family_rows:
        scenario = row["scenario"]
        min_y = int(row["min_y"])
        family = row["family"]
        y_min = int(row["y_min"])
        count = int(row["count"])
        chunks = int(row["chunks"])
        distribution_groups[(scenario, min_y, family, y_min)].append(count / chunks)
        case_key = (row["case_id"], scenario, min_y, family)
        per_case_totals[case_key] += count / chunks
        if y_min < -64:
            per_case_below[case_key] += count / chunks
        vector_key = (
            scenario,
            row["seed"],
            min_y,
            row["mod_enabled"],
            row["ore_fix"],
            row["case_id"],
            family,
        )
        case_vectors[vector_key].append((y_min, count))

    distribution_fields = (
        "scenario", "min_y", "family", "y_min", "y_max_exclusive",
        "samples", "mean_count_per_chunk", "min_count_per_chunk", "max_count_per_chunk",
    )
    distribution_rows = []
    for (scenario, min_y, family, y_min), values in sorted(distribution_groups.items()):
        distribution_rows.append(
            {
                "scenario": scenario,
                "min_y": min_y,
                "family": family,
                "y_min": y_min,
                "y_max_exclusive": y_min + 16,
                "samples": len(values),
                "mean_count_per_chunk": f"{mean(values):.9f}",
                "min_count_per_chunk": f"{min(values):.9f}",
                "max_count_per_chunk": f"{max(values):.9f}",
            }
        )
    write_csv(output / "ore-distribution-mean.csv", distribution_rows, distribution_fields)

    total_groups: dict[tuple[str, int, str], list[float]] = defaultdict(list)
    below_groups: dict[tuple[str, int, str], list[float]] = defaultdict(list)
    for (_, scenario, min_y, family), value in per_case_totals.items():
        total_groups[(scenario, min_y, family)].append(value)
    for (_, scenario, min_y, family), value in per_case_below.items():
        below_groups[(scenario, min_y, family)].append(value)
    total_fields = (
        "scenario", "min_y", "family", "samples", "mean_total_per_chunk",
        "stdev_total_per_chunk", "mean_below_minus_64_per_chunk",
    )
    total_rows = []
    for key, values in sorted(total_groups.items()):
        below_values = below_groups.get(key, [0.0] * len(values))
        total_rows.append(
            {
                "scenario": key[0],
                "min_y": key[1],
                "family": key[2],
                "samples": len(values),
                "mean_total_per_chunk": f"{mean(values):.9f}",
                "stdev_total_per_chunk": f"{statistics.stdev(values):.9f}" if len(values) > 1 else "0.000000000",
                "mean_below_minus_64_per_chunk": f"{mean(below_values):.9f}",
            }
        )
    write_csv(output / "ore-totals.csv", total_rows, total_fields)

    chart_root = output / "charts"
    index_lines = ["# Ore distribution charts", ""]
    chart_data: dict[tuple[int, str], dict[str, list[tuple[int, float]]]] = defaultdict(lambda: defaultdict(list))
    for row in distribution_rows:
        chart_data[(int(row["min_y"]), row["family"])][row["scenario"]].append(
            (int(row["y_min"]), float(row["mean_count_per_chunk"]))
        )
    for (min_y, family), series in sorted(chart_data.items()):
        suffix = f"neg{abs(min_y)}" if min_y < 0 else str(min_y)
        filename = f"{family}-min-y-{suffix}.svg"
        svg_chart(chart_root / filename, f"{family} distribution, minY={min_y}", series)
        index_lines.extend((f"## {family}, minY={min_y}", "", f"![{family} minY {min_y}]({filename})", ""))
    (chart_root / "index.md").write_text("\n".join(index_lines), encoding="utf-8")

    deterministic_checks = []
    raw_case_signatures: dict[tuple[str, str, int, str, str], dict[str, dict[str, tuple[int, ...]]]] = defaultdict(lambda: defaultdict(dict))
    for vector_key, vector in case_vectors.items():
        signature = vector_key[:5]
        case_id = vector_key[5]
        family = vector_key[6]
        raw_case_signatures[signature][case_id][family] = tuple(
            count for _, count in sorted(vector)
        )
    for signature, cases in sorted(raw_case_signatures.items()):
        if len(cases) < 2:
            continue
        values = list(cases.values())
        deterministic_checks.append(
            {
                "scenario": signature[0],
                "seed": signature[1],
                "min_y": signature[2],
                "mod_enabled": signature[3],
                "ore_fix": signature[4],
                "case_ids": sorted(cases),
                "equal": all(value == values[0] for value in values[1:]),
            }
        )

    acceptance = {
        "source_csv_files": [
            {"case_id": path.parent.name, "sha256": sha256(path)} for path in csv_paths
        ],
        "case_count": len({row["case_id"] for row in family_rows}),
        "scenarios": sorted({row["scenario"] for row in family_rows}),
        "seeds": sorted({row["seed"] for row in family_rows}, key=int),
        "min_y_values": sorted({int(row["min_y"]) for row in family_rows}),
        "determinism_checks": deterministic_checks,
        "determinism_pass": (
            all(check["equal"] for check in deterministic_checks)
            if deterministic_checks
            else None
        ),
        "note": "数量级膨胀、-64 以下连续零桶与 #438 结论需结合配对场景在 TEST_REPORT.md 中解释。",
    }
    (output / "acceptance.json").write_text(json.dumps(acceptance, indent=2) + "\n", encoding="utf-8")
    print(f"aggregated {len(csv_paths)} CSV files into {output}")
    return 0


def parser() -> argparse.ArgumentParser:
    root = argparse.ArgumentParser(description=__doc__)
    commands = root.add_subparsers(dest="command", required=True)

    scan = commands.add_parser("scan", help="统计一个已停止的 1.20.1 Overworld")
    scan.add_argument("world", type=Path)
    scan.add_argument("--chunks", type=int, nargs=4, metavar=("MIN_X", "MIN_Z", "MAX_X", "MAX_Z"), required=True)
    scan.add_argument("--case-id", required=True)
    scan.add_argument("--scenario", required=True)
    scan.add_argument("--seed", type=int, required=True)
    scan.add_argument("--expected-min-y", type=int, required=True)
    scan.add_argument("--expected-max-y", type=int, default=320)
    scan.add_argument("--expected-data-version", type=int, default=DATA_VERSION_1_20_1)
    scan.add_argument("--mod-enabled", type=bool_text, required=True)
    scan.add_argument("--ore-fix", choices=("true", "false", "unsupported"), required=True)
    scan.add_argument("--include-materials", action="store_true")
    scan.add_argument("--csv", type=Path, required=True)
    scan.add_argument("--summary", type=Path)
    scan.set_defaults(func=scan_world)

    report = commands.add_parser("aggregate", help="合并扫描 CSV 并生成均值 CSV/SVG")
    report.add_argument("--input", type=Path, action="append", required=True)
    report.add_argument("--output", type=Path, required=True)
    report.set_defaults(func=aggregate)
    return root


def main() -> int:
    try:
        args = parser().parse_args()
        return args.func(args)
    except (FormatError, OSError, ValueError, zlib.error, gzip.BadGzipFile) as error:
        print(f"error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
