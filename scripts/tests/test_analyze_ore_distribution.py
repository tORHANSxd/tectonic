import struct
import sys
import unittest
from collections import Counter
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from analyze_ore_distribution import FormatError, parse_nbt, unpack_palette_frequencies


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
        frequencies = unpack_palette_frequencies(2, pack_indices(indices, 2, padding=15))
        self.assertEqual([0, 4096], frequencies)

    def test_invalid_packed_length_fails(self):
        with self.assertRaises(FormatError):
            unpack_palette_frequencies(2, [0])


class NbtReaderTest(unittest.TestCase):
    def test_negative_section_y_byte(self):
        payload = b"\x0a\x00\x00\x01\x00\x01Y" + struct.pack(">b", -8) + b"\x00"
        self.assertEqual(-8, parse_nbt(payload)["Y"])


if __name__ == "__main__":
    unittest.main()
