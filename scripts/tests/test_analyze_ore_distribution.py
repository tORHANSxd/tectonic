import hashlib
import json
import struct
import sys
import tempfile
import unittest
from collections import Counter
from pathlib import Path
from unittest.mock import patch


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analyze_ore_distribution import (
    DATA_VERSION_1_20_1,
    FormatError,
    RegionFile,
    load_scan_summary,
    parse_nbt,
    scan_world,
    unpack_palette_frequencies,
    unpack_palette_indices,
)


def pack_indices(indices, palette_size, padding=0):
    bits = max(4, (palette_size - 1).bit_length())
    values_per_long = 64 // bits
    words = []
    for start in range(0, len(indices), values_per_long):
        word = 0
        values = indices[start : start + values_per_long]
        for offset, value in enumerate(values):
            word |= value << (offset * bits)
        if len(values) == values_per_long and padding:
            word |= padding << (values_per_long * bits)
        words.append(word if word < 1 << 63 else word - (1 << 64))
    return words


class PalettePackingTest(unittest.TestCase):
    def test_single_palette_without_data(self):
        self.assertEqual([4096], unpack_palette_frequencies(1, None))

    def test_four_bit_palette_and_signed_longs(self):
        indices = [0, 1] * 2048
        frequencies = unpack_palette_frequencies(2, pack_indices(indices, 2))
        self.assertEqual([2048, 2048], frequencies)
        self.assertTrue(any(word < 0 for word in pack_indices([15] * 4096, 16)))

    def test_five_bit_values_do_not_cross_long_boundary(self):
        indices = [index % 17 for index in range(4096)]
        packed = pack_indices(indices, 17)
        self.assertEqual(342, len(packed))
        self.assertEqual(Counter(indices), Counter(dict(enumerate(unpack_palette_frequencies(17, packed)))))

    def test_padding_is_ignored(self):
        indices = [1] * 4096
        packed = pack_indices(indices, 17, padding=15)
        self.assertEqual(15, (packed[0] & ((1 << 64) - 1)) >> 60)
        frequencies = unpack_palette_frequencies(17, packed)
        self.assertEqual([0, 4096] + [0] * 15, frequencies)

    def test_two_bit_biome_vector_uses_lsb_first_order(self):
        packed = [0x2424242424242424, 0x2424242424242424]
        self.assertEqual(
            [0, 1, 2, 0] * 16,
            unpack_palette_indices(3, packed, value_count=64, min_bits=1),
        )

    def test_invalid_packed_length_fails(self):
        with self.assertRaises(FormatError):
            unpack_palette_frequencies(2, [0])


class NbtReaderTest(unittest.TestCase):
    def test_negative_section_y_byte(self):
        payload = b"\x0a\x00\x00\x01\x00\x01Y" + struct.pack(">b", -8) + b"\x00"
        self.assertEqual(-8, parse_nbt(payload)["Y"])

    def test_truncated_gzip_reports_chunk_coordinates(self):
        header = bytearray(8192)
        header[0:4] = ((2 << 8) | 1).to_bytes(4, "big")
        compressed = b"\x1f\x8b\x08"
        payload = bytes(header) + (len(compressed) + 1).to_bytes(4, "big") + b"\x01" + compressed
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.0.0.mca"
            path.write_bytes(payload)
            reader = RegionFile(path)
            try:
                with self.assertRaisesRegex(FormatError, r"区块 \(0, 0\) 解压失败"):
                    reader.read_chunk(0, 0)
            finally:
                reader.close()

    def test_scan_summary_binds_csv_to_exact_chunk_window(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            csv_path = root / "ore-counts.csv"
            csv_path.write_text("fixture\n", encoding="utf-8")
            summary_path = root / "ore-summary.json"
            summary = {
                "csv_sha256": hashlib.sha256(csv_path.read_bytes()).hexdigest(),
                "chunk_bounds_inclusive": [64, 64, 79, 79],
                "chunks": 256,
                "data_versions": [DATA_VERSION_1_20_1],
            }
            summary_path.write_text(json.dumps(summary), encoding="utf-8")

            actual_path, _, bounds = load_scan_summary(csv_path)
            self.assertEqual(summary_path, actual_path)
            self.assertEqual((64, 64, 79, 79), bounds)

            summary["chunks"] = 255
            summary_path.write_text(json.dumps(summary), encoding="utf-8")
            with self.assertRaisesRegex(FormatError, "闭区间面积"):
                load_scan_summary(csv_path)

    def test_ore_scan_ignores_lighting_only_section_outside_height(self):
        class FakeRegionFile:
            outside_section = {"Y": -5, "BlockLight": bytes(2048)}

            def __init__(self, _path):
                pass

            def read_chunk(self, _chunk_x, _chunk_z):
                sections = [
                    {
                        "Y": section_y,
                        "block_states": {"palette": [{"Name": "minecraft:air"}]},
                    }
                    for section_y in range(-4, 20)
                ]
                sections.append(dict(self.outside_section))
                return {
                    "DataVersion": DATA_VERSION_1_20_1,
                    "xPos": 0,
                    "zPos": 0,
                    "Status": "minecraft:full",
                    "sections": sections,
                }

            def close(self):
                pass

        with tempfile.TemporaryDirectory() as directory:
            world = Path(directory) / "world"
            region = world / "region"
            region.mkdir(parents=True)
            (region / "r.0.0.mca").touch()
            args = type(
                "Args",
                (),
                {
                    "world": world,
                    "expected_min_y": -64,
                    "expected_max_y": 320,
                    "expected_data_version": DATA_VERSION_1_20_1,
                    "chunks": (0, 0, 0, 0),
                    "include_materials": False,
                    "case_id": "lighting-only",
                    "scenario": "test",
                    "seed": 0,
                    "mod_enabled": "true",
                    "ore_fix": "false",
                    "csv": Path(directory) / "ore-counts.csv",
                    "summary": Path(directory) / "ore-summary.json",
                },
            )()
            with patch("analyze_ore_distribution.RegionFile", FakeRegionFile):
                self.assertEqual(0, scan_world(args))

            FakeRegionFile.outside_section = {
                "Y": -5,
                "block_states": {"palette": [{"Name": "minecraft:air"}]},
            }
            with patch("analyze_ore_distribution.RegionFile", FakeRegionFile):
                with self.assertRaisesRegex(FormatError, "范围外"):
                    scan_world(args)


if __name__ == "__main__":
    unittest.main()
