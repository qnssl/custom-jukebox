#!/usr/bin/env python3
"""
generate_tracks.py

Reads tools/album_map.json (the artist/album/disc-range grouping) and an
optional tools/titles.csv (disc_id,title - real song names, one per line,
UTF-8), then:

  1. Expands every {discStart..discEnd} range into individual disc_XX ids.
  2. Copies matching audio from source_tracks/{id}.ogg into
     src/main/resources/assets/customjukebox/sounds/tracks/{id}.ogg
  3. Copies matching cover art from source_tracks/{id}.png into
     src/main/resources/assets/customjukebox/textures/covers/{id}.png
     (falls back to a generated flat-color placeholder if missing, so the
     game never shows a broken/missing texture).
  4. Writes assets/customjukebox/tracks.json (ordered, one object per
     album_map.json entry - groups are NEVER merged, even duplicates).
  5. Writes/merges assets/customjukebox/sounds.json (one "track.<id>"
     sound event per track, category "record", stream=true - required
     for anything longer than a couple seconds).

Run this every time you add new songs, THEN run `gradlew build`.

Usage:
    python tools/generate_tracks.py

Requirements: only the Python standard library (no pip installs needed).
"""
import json
import shutil
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ALBUM_MAP = ROOT / "tools" / "album_map.json"
TITLES_CSV = ROOT / "tools" / "titles.csv"
SOURCE_DIR = ROOT / "source_tracks"

RES_ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "customjukebox"
SOUNDS_DIR = RES_ASSETS / "sounds" / "tracks"
COVERS_DIR = RES_ASSETS / "textures" / "covers"
TRACKS_JSON = RES_ASSETS / "tracks.json"
SOUNDS_JSON = RES_ASSETS / "sounds.json"


def disc_id(n: int) -> str:
    # Matches your existing file naming: disc_1.ogg ... disc_145.ogg (no zero-padding)
    return f"disc_{n}"


def load_titles() -> dict:
    titles = {}
    if TITLES_CSV.exists():
        with TITLES_CSV.open("r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                if "," not in line:
                    continue
                tid, title = line.split(",", 1)
                titles[tid.strip()] = title.strip()
    return titles


def make_placeholder_png(path: Path, size: int = 32, rgb=(60, 60, 66)):
    """Writes a minimal flat-color PNG with no external dependencies."""
    width = height = size
    raw = b""
    for _ in range(height):
        raw += b"\x00" + bytes(rgb) * width

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data +
                struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff))

    sig = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)
    idat = zlib.compress(raw, 9)
    png = sig + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")
    path.write_bytes(png)


def find_source(id_: str, exts):
    for ext in exts:
        p = SOURCE_DIR / f"{id_}{ext}"
        if p.exists():
            return p
    return None


def main():
    if not ALBUM_MAP.exists():
        raise SystemExit(f"Missing {ALBUM_MAP}")

    SOUNDS_DIR.mkdir(parents=True, exist_ok=True)
    COVERS_DIR.mkdir(parents=True, exist_ok=True)
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)

    album_map = json.loads(ALBUM_MAP.read_text(encoding="utf-8"))
    titles = load_titles()

    tracks_json = []
    sounds_json = {}

    missing_audio = []
    missing_cover = []

    for group in album_map:
        artist = group["artist"]
        raw_album = group.get("album")
        is_single = group.get("single", False)

        if raw_album:
            album_title = raw_album
        elif is_single:
            album_title = "Сингл"
        else:
            album_title = "Без альбома"

        start = group["discStart"]
        end = group["discEnd"]

        track_entries = []
        for n in range(start, end + 1):
            tid = disc_id(n)

            # --- audio ---
            audio_src = find_source(tid, [".ogg"])
            audio_dst = SOUNDS_DIR / f"{tid}.ogg"
            if audio_src:
                shutil.copyfile(audio_src, audio_dst)
            elif not audio_dst.exists():
                missing_audio.append(tid)

            # --- cover ---
            cover_src = find_source(tid, [".png"])
            cover_dst = COVERS_DIR / f"{tid}.png"
            if cover_src:
                shutil.copyfile(cover_src, cover_dst)
            elif not cover_dst.exists():
                # try non-png source that needs conversion first
                alt = find_source(tid, [".jpg", ".jpeg", ".webp"])
                if alt:
                    missing_cover.append(f"{tid} (found {alt.name} - convert to PNG first, e.g. `ffmpeg -i {alt.name} {tid}.png`)")
                else:
                    missing_cover.append(tid)
                make_placeholder_png(cover_dst)

            title = titles.get(tid, f"{tid}")

            track_entries.append({"id": tid, "title": title})
            sounds_json[f"track.{tid}"] = {
                "category": "record",
                "sounds": [
                    {"name": f"customjukebox:tracks/{tid}", "stream": True}
                ]
            }

        tracks_json.append({
            "artist": artist,
            "album": album_title,
            "tracks": track_entries,
        })

    TRACKS_JSON.write_text(json.dumps(tracks_json, ensure_ascii=False, indent=2), encoding="utf-8")
    SOUNDS_JSON.write_text(json.dumps(sounds_json, ensure_ascii=False, indent=2), encoding="utf-8")

    total_tracks = sum(len(g["tracks"]) for g in tracks_json)
    print(f"OK: {len(tracks_json)} album groups, {total_tracks} tracks written to tracks.json / sounds.json")

    if missing_audio:
        print(f"\n[!] {len(missing_audio)} track(s) have NO .ogg file in source_tracks/:")
        print("    " + ", ".join(missing_audio))
        print("    They are silent until you add source_tracks/<id>.ogg and re-run this script.")

    if missing_cover:
        print(f"\n[!] {len(missing_cover)} track(s) got a placeholder grey cover:")
        for m in missing_cover:
            print(f"    {m}")


if __name__ == "__main__":
    main()
