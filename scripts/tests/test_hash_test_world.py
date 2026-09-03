import copy
import hashlib
import struct
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analyze_ore_distribution import DATA_VERSION_1_20_1, FormatError
from hash_test_world import (
    EXCLUDES,
    INCLUDES,
    SCHEMA,
    canonicalize_chunk,
    compare_snapshots,
    hash_chunks,
    scan_world,
)


def pack_indices(indices, palette_size, min_bits):
    bits = max(min_bits, (palette_size - 1).bit_length())
    values_per_long = 64 // bits
    words = []
    for start in range(0, len(indices), values_per_long):
        word = 0
        for offset, value in enumerate(indices[start : start + values_per_long]):
            word |= value << (offset * bits)
        words.append(word if word < 1 << 63 else word - (1 << 64))
    return words


def make_chunk(chunk_x=0, chunk_z=0):
    block_indices = [index % 2 for index in range(4096)]
    biome_indices = [index % 2 for index in range(64)]
    return {
        "DataVersion": DATA_VERSION_1_20_1,
        "xPos": chunk_x,
        "zPos": chunk_z,
        "yPos": -4,
        "Status": "minecraft:full",
        "LastUpdate": 1,
        "InhabitedTime": 2,
        "sections": [
            {
                "Y": -4,
                "block_states": {
                    "palette": [
                        {"Name": "minecraft:air"},
                        {
                            "Name": "minecraft:oak_log",
                            "Properties": {"axis": "y", "waterlogged": "false"},
                        },
                    ],
                    "data": pack_indices(block_indices, 2, 4),
                },
                "biomes": {
                    "palette": ["minecraft:plains", "minecraft:desert"],
                    "data": pack_indices(biome_indices, 2, 1),
                },
            },
            {
                "Y": -3,
                "block_states": {"palette": [{"Name": "minecraft:air"}]},
                "biomes": {"palette": ["minecraft:plains"]},
            },
        ],
    }


def canonical(root, chunk_x=0, chunk_z=0):
    return canonicalize_chunk(
        root,
        chunk_x=chunk_x,
        chunk_z=chunk_z,
        expected_min_y=-64,
        expected_max_y=-32,
    )


def snapshot(chunks):
    terrain_hash, details = hash_chunks(chunks)
    return {
        "schema": SCHEMA,
        "chunk_bounds_inclusive": [0, 0, 1, 0],
        "chunks": 2,
        "data_version": DATA_VERSION_1_20_1,
        "min_y": -64,
        "max_y": -32,
        "includes": list(INCLUDES),
        "excludes": list(EXCLUDES),
        "terrain_sha256": terrain_hash,
        "chunk_hashes": details,
    }


class TerrainSnapshotTest(unittest.TestCase):
    def test_snapshot_v1_golden_hash(self):
        payload = canonical(make_chunk())
        self.assertEqual(
            "95bf96285803e311e4c58304048dfdf56e3fb541d764bb351403d091d36c7196",
            hashlib.sha256(payload).hexdigest(),
        )
        self.assertEqual(
            "faebc3a0f07e97ca91e7a943475ade642687cb51b60ed4270f5da7bfc3f2ca6b",
            hash_chunks([(0, 0, payload)])[0],
        )

    def test_canonical_chunk_is_encoding_order_independent(self):
        left = make_chunk()
        right = copy.deepcopy(left)
        right["sections"].reverse()
        section = next(item for item in right["sections"] if item["Y"] == -4)
        section["block_states"]["palette"] = [
            {"Name": "minecraft:dirt"},
            {
                "Properties": {"waterlogged": "false", "axis": "y"},
                "Name": "minecraft:oak_log",
            },
            {"Name": "minecraft:air", "Properties": {}},
        ]
        section["block_states"]["data"] = pack_indices(
            [2 if index % 2 == 0 else 1 for index in range(4096)], 3, 4
        )
        section["biomes"]["palette"] = [
            "minecraft:desert",
            "minecraft:plains",
            "minecraft:forest",
        ]
        section["biomes"]["data"] = pack_indices(
            [1 if index % 2 == 0 else 0 for index in range(64)], 3, 1
        )

        self.assertEqual(canonical(left), canonical(right))
        section["block_states"]["data"] = pack_indices(
            [1] + [2 if index % 2 == 0 else 1 for index in range(1, 4096)], 3, 4
        )
        self.assertNotEqual(canonical(left), canonical(right))

    def test_world_hash_sorts_chunks_z_then_x(self):
        chunks = [(1, 0, b"one"), (0, 1, b"two"), (0, 0, b"zero")]
        forward_hash, forward_details = hash_chunks(chunks)
        reverse_hash, reverse_details = hash_chunks(reversed(chunks))
        self.assertEqual(forward_hash, reverse_hash)
        self.assertEqual(forward_details, reverse_details)
        self.assertEqual([(0, 0), (1, 0), (0, 1)], [
            (row["x"], row["z"]) for row in forward_details
        ])

    def test_volatile_fields_are_excluded_without_masking_content(self):
        left = make_chunk()
        right = copy.deepcopy(left)
        right.update(
            {
                "LastUpdate": 999,
                "InhabitedTime": 888,
                "block_ticks": [{"i": "minecraft:water", "t": 3}],
                "fluid_ticks": [{"i": "minecraft:water", "t": 4}],
                "Heightmaps": {"WORLD_SURFACE": [1, 2, 3]},
                "block_entities": [{"id": "minecraft:chest"}],
            }
        )
        self.assertEqual(canonical(left), canonical(right))

        section = next(item for item in right["sections"] if item["Y"] == -4)
        section["biomes"]["data"] = pack_indices([1] * 64, 2, 1)
        self.assertNotEqual(canonical(left), canonical(right))

    def test_lighting_only_section_outside_height_is_ignored(self):
        left = make_chunk()
        right = copy.deepcopy(left)
        right["sections"].append(
            {"Y": -5, "BlockLight": bytes(2048), "SkyLight": bytes(2048)}
        )
        self.assertEqual(canonical(left), canonical(right))

        right["sections"][-1]["block_states"] = {
            "palette": [{"Name": "minecraft:air"}]
        }
        with self.assertRaisesRegex(FormatError, "范围外"):
            canonical(right)

    def test_compare_validates_snapshot_and_finds_nonfinal_difference(self):
        left = snapshot([(0, 0, b"left"), (1, 0, b"same")])
        right = snapshot([(0, 0, b"right"), (1, 0, b"same")])
        differences = compare_snapshots(left, right)
        self.assertEqual([(0, 0)], [(row["x"], row["z"]) for row in differences])
        self.assertEqual([], compare_snapshots(left, copy.deepcopy(left)))

        damaged = copy.deepcopy(left)
        damaged["terrain_sha256"] = "0" * 64
        with self.assertRaisesRegex(FormatError, "逐区块哈希不一致"):
            compare_snapshots(left, damaged)

        damaged = copy.deepcopy(left)
        damaged["chunk_hashes"].pop()
        with self.assertRaisesRegex(FormatError, "缺少区块"):
            compare_snapshots(left, damaged)

    def test_missing_requested_chunk_fails(self):
        class FakeRegionFile:
            def __init__(self, _path):
                pass

            def read_chunk(self, chunk_x, chunk_z):
                if (chunk_x, chunk_z) == (0, 0):
                    return make_chunk()
                raise FormatError(f"缺少区块 ({chunk_x}, {chunk_z})")

            def close(self):
                pass

        with tempfile.TemporaryDirectory() as directory:
            world = Path(directory)
            region = world / "region"
            region.mkdir()
            (region / "r.0.0.mca").touch()
            with patch("hash_test_world.RegionFile", FakeRegionFile):
                with self.assertRaisesRegex(FormatError, r"缺少区块 \(1, 0\)"):
                    scan_world(
                        world,
                        (0, 0, 1, 0),
                        expected_min_y=-64,
                        expected_max_y=-32,
                    )

    def test_non_full_chunk_fails(self):
        root = make_chunk()
        root["Status"] = "minecraft:features"
        with self.assertRaisesRegex(FormatError, "minecraft:features"):
            canonical(root)


if __name__ == "__main__":
    unittest.main()
