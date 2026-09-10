#!/usr/bin/env python3
"""
Re-encodes the shipped wiki art to WebP at the size it is actually drawn at.

The wiki hands out full-size renders — enemy characters arrive as 1024x1024 PNGs of ~570 KB each,
while the templates never draw them larger than a couple of hundred pixels. Photoreal art with an
alpha channel is a poor fit for PNG: at 512 px an enemy render still costs ~245 KB as PNG but only
~41 KB as WebP (q82), with no visible difference at the sizes we draw.

This script resizes every managed asset group to a fixed maximum edge, writes the result next to
the original as `<name>.webp`, and removes the superseded file. Every `iconUrl` in the JSON data
points at the `.webp` path, so run this after scraping (scrape_wiki.py calls it automatically) and
keep MAX_EDGE in sync with
src/test/java/dev/hendrikhoemberg/witchfirerandomizer/assets/ImageAssetBudgetTest.java.
"""

import os
import sys

from PIL import Image

IMAGES_DIR = os.path.abspath("src/main/resources/static/images")

# group directory (or single file) -> maximum edge in pixels
MAX_EDGE = {
    "enemies": 512,
    "items": 256,
    "arcana": 192,
    "prophecies": 160,
    "texture-transparent.webp": 256,
    "wf-logo2.webp": 900,
    "wf-bg.webp": 3840,
}

WEBP_QUALITY = 82
SOURCE_EXTENSIONS = (".png", ".webp", ".jpg", ".jpeg")


def managed_assets():
    """Yields (path, max_edge) for every asset this script owns."""
    for name, edge in MAX_EDGE.items():
        path = os.path.join(IMAGES_DIR, name)
        if os.path.isdir(path):
            for entry in sorted(os.listdir(path)):
                if entry.lower().endswith(SOURCE_EXTENSIONS):
                    yield os.path.join(path, entry), edge
        elif os.path.isfile(path):
            yield path, edge


def webp_target(path):
    """enemy-foo.png -> enemy-foo.webp, keeping already-WebP names unchanged."""
    stem, _ = os.path.splitext(path)
    return stem + ".webp"


def is_settled(path, max_edge):
    """Already WebP and already small enough: re-encoding would only lose quality."""
    if not path.lower().endswith(".webp"):
        return False
    with Image.open(path) as img:
        return max(img.size) <= max_edge


def convert(path, max_edge):
    """Returns (before_bytes, after_bytes, original_size, new_size, destination)."""
    before = os.path.getsize(path)
    destination = webp_target(path)

    with Image.open(path) as img:
        original = img.size
        resized = img.convert("RGBA")
        if max(original) > max_edge:
            scale = max_edge / max(original)
            resized = resized.resize(
                (max(1, round(original[0] * scale)), max(1, round(original[1] * scale))),
                Image.LANCZOS,
            )
        new_size = resized.size

        # Replacing an already-WebP file in place must not truncate it mid-write.
        temporary = destination + ".tmp"
        resized.save(temporary, "WEBP", quality=WEBP_QUALITY, method=6)
        os.replace(temporary, destination)

    if os.path.abspath(path) != os.path.abspath(destination):
        os.remove(path)

    return before, os.path.getsize(destination), original, new_size, destination


def main():
    if not os.path.isdir(IMAGES_DIR):
        print(f"FAIL: {IMAGES_DIR} does not exist", file=sys.stderr)
        return 1

    total_before = 0
    total_after = 0
    converted = 0
    skipped = 0

    for path, max_edge in managed_assets():
        if is_settled(path, max_edge):
            skipped += 1
            continue
        before, after, original, new_size, destination = convert(path, max_edge)
        total_before += before
        total_after += after
        converted += 1
        print(f"  {os.path.relpath(destination, IMAGES_DIR):44s} "
              f"{original[0]}x{original[1]} -> {new_size[0]}x{new_size[1]}  "
              f"{before / 1024:.0f} KB -> {after / 1024:.0f} KB")

    if converted:
        print(f"\n{converted} assets converted to WebP. "
              f"{total_before / 1e6:.1f} MB -> {total_after / 1e6:.1f} MB")
    else:
        print(f"Nothing to do: {skipped} assets are already WebP and within their pixel budget.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
