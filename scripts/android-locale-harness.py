#!/usr/bin/env python3
"""Prepare and install deterministic Android locale drafts for AI translation."""

from __future__ import annotations

import argparse
import importlib.util
import json
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_SRC = ROOT / "app" / "src"
STATUS = ROOT / "docs" / "localization-status.json"
_checker_spec = importlib.util.spec_from_file_location(
    "android_locale_checker", ROOT / "scripts" / "check-android-locales.py"
)
checker = importlib.util.module_from_spec(_checker_spec)
sys.modules[_checker_spec.name] = checker
_checker_spec.loader.exec_module(checker)


def qualifier(tag: str) -> str:
    parts = tag.split("-")
    return f"values-{tag}" if len(parts) == 1 else "values-b+" + "+".join(parts)


def canonical_files() -> tuple[dict[str, list[Path]], dict[str, str]]:
    errors: list[str] = []
    files = {}
    for source_set in sorted(APP_SRC.iterdir()):
        if not source_set.is_dir():
            continue
        _, origins = checker.load_resource_directory(source_set / "res" / "values", errors)
        if origins:
            files[source_set.name] = sorted(set(origins.values()))
    hashes = checker.source_catalog_hashes(errors, APP_SRC)
    if errors:
        raise ValueError("invalid canonical catalogs: " + "; ".join(errors))
    if not files:
        raise ValueError("no canonical Android catalogs found")
    return files, hashes


def validate_pair(source: Path, translated: Path) -> None:
    """Validate a full source-set directory, independent of resource filenames."""
    errors: list[str] = []
    canonical, _ = checker.load_resource_directory(source, errors)
    candidate, _ = checker.load_resource_directory(translated, errors)
    if canonical.keys() != candidate.keys():
        missing = sorted(canonical.keys() - candidate.keys())
        extra = sorted(candidate.keys() - canonical.keys())
        raise ValueError(f"catalog keys differ; missing={missing[:5]} extra={extra[:5]}")
    for key, expected in canonical.items():
        checker.compare_entry(str(source), str(translated), expected, candidate[key], errors)
    if errors:
        raise ValueError("; ".join(errors))


def prepare(args: argparse.Namespace) -> None:
    files, hashes = canonical_files()
    target = ROOT / "build" / "i18n" / args.tag
    if target.exists() and not args.force:
        raise FileExistsError(f"draft already exists: {target}")
    target.mkdir(parents=True, exist_ok=True)
    for source_set, paths in files.items():
        for source in paths:
            destination = target / source_set / source.name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    manifest = {
        "schema_version": 2,
        "tag": args.tag,
        "native_name": args.native_name,
        "verification": "ai-translated",
        "canonical_sha256": hashes,
    }
    (target / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(target)


def install(args: argparse.Namespace) -> None:
    draft = ROOT / "build" / "i18n" / args.tag
    manifest = json.loads((draft / "manifest.json").read_text(encoding="utf-8"))
    if manifest.get("tag") != args.tag:
        raise ValueError("draft manifest tag mismatch")
    if manifest.get("schema_version") != 2:
        raise ValueError("draft predates complete catalog coverage; regenerate the draft")
    files, hashes = canonical_files()
    if manifest.get("canonical_sha256") != hashes:
        raise ValueError("English catalogs changed; regenerate the draft")
    for source_set in files:
        validate_pair(APP_SRC / source_set / "res" / "values", draft / source_set)
    destination_name = qualifier(args.tag)
    copies = [
        (source, APP_SRC / source_set / "res" / destination_name / source.name)
        for source_set in files
        for source in sorted((draft / source_set).glob("*.xml"))
    ]
    for _, destination in copies:
        if destination.exists() and not args.force:
            raise FileExistsError(f"locale already installed: {destination}")
    for source, destination in copies:
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
    status = json.loads(STATUS.read_text(encoding="utf-8"))
    status["locales"][args.tag] = {
        "native_name": manifest["native_name"],
        "verification": "ai-translated",
        "review_refs": [],
        "source_sha256": hashes,
        "surfaces": {
            "android": "complete",
            "readme": "english-fallback",
            "user_docs": "english-fallback",
        },
    }
    status["locales"] = dict(sorted(status["locales"].items()))
    STATUS.write_text(json.dumps(status, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"installed {args.tag} as {destination_name}")


def main() -> int:
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("prepare", help="create a non-shipping draft from canonical English")
    create.add_argument("tag")
    create.add_argument("native_name")
    create.add_argument("--force", action="store_true")
    create.set_defaults(handler=prepare)
    add = commands.add_parser("install", help="validate and install a completed draft")
    add.add_argument("tag")
    add.add_argument("--force", action="store_true")
    add.set_defaults(handler=install)
    args = parser.parse_args()
    try:
        args.handler(args)
    except (FileNotFoundError, FileExistsError, ValueError, ET.ParseError, KeyError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
