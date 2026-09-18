#!/usr/bin/env python3
"""Validate Android translation catalogs against each source set's English resources."""

from __future__ import annotations

import json
import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
APP_SRC = REPO_ROOT / "app" / "src"
STATUS_PATH = REPO_ROOT / "docs" / "localization-status.json"
LOCALE_DIR = re.compile(r"^values-(?:[a-z]{2,3}(?:-r[A-Z]{2})?|b\+[A-Za-z0-9+]+)$")
PRINTF = re.compile(r"%(?:(\d+)\$)?[-#+ 0,(<]*\d*(?:\.\d+)?([A-Za-z%])")
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
VERIFICATION_STATES = {"canonical", "ai-translated", "community-reviewed", "verified"}
# These catalogs replace previously hardcoded English. Existing locales retain
# that English fallback until translated; Korean ships complete coverage.
NEW_FALLBACK_CATALOGS = {
    "supervised_strings.xml",
    "korean_coverage_strings.xml",
    "korean_runtime_strings.xml",
    "runtime_notice_strings.xml",
    "ui_label_strings.xml",
}
COMPLETE_CATALOG_LOCALES = {"ko"}
PLURAL_QUANTITIES = {"zero", "one", "two", "few", "many", "other"}


@dataclass(frozen=True)
class Entry:
    kind: str
    name: str
    values: dict[str, str]
    formatted: bool


def fail(message: str, errors: list[str]) -> None:
    errors.append(message)


def text(node: ET.Element) -> str:
    return "".join(node.itertext())


def path_label(path: Path) -> str:
    try:
        return str(path.relative_to(REPO_ROOT))
    except ValueError:
        return str(path)


def load_catalog(path: Path, errors: list[str]) -> dict[tuple[str, str], Entry]:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        fail(f"{path_label(path)}: cannot parse XML: {exc}", errors)
        return {}

    if root.tag != "resources":
        fail(f"{path_label(path)}: expected <resources> root", errors)
        return {}
    catalog: dict[tuple[str, str], Entry] = {}
    seen: Counter[tuple[str, str]] = Counter()
    for node in root:
        name = node.attrib.get("name")
        if not name or node.tag not in {"string", "plurals", "string-array"}:
            continue
        key = (node.tag, name)
        seen[key] += 1
        if node.attrib.get("translatable", "true").lower() == "false":
            continue
        formatted = node.attrib.get("formatted", "true").lower() != "false"
        if node.tag == "string":
            values = {"value": text(node)}
        else:
            values = {}
            for index, item in enumerate(node.findall("item")):
                variant = item.attrib.get("quantity", str(index))
                if variant in values:
                    fail(f"{path_label(path)}: {node.tag}/{name} has duplicate variant {variant!r}", errors)
                if node.tag == "plurals" and variant not in PLURAL_QUANTITIES:
                    fail(f"{path_label(path)}: plurals/{name} has invalid quantity {variant!r}", errors)
                values[variant] = text(item)
            if node.tag == "plurals" and "other" not in values:
                fail(f"{path_label(path)}: plurals/{name} is missing required 'other' quantity", errors)
        catalog[key] = Entry(node.tag, name, values, formatted)

    for key, count in seen.items():
        if count > 1:
            fail(
                f"{path_label(path)}: duplicate <{key[0]}> name={key[1]!r}",
                errors,
            )
    return catalog


def placeholders(value: str) -> list[tuple[int, str]]:
    result: list[tuple[int, str]] = []
    implicit_index = 1
    for match in PRINTF.finditer(value):
        conversion = match.group(2)
        if conversion == "%":
            continue
        index = int(match.group(1)) if match.group(1) else implicit_index
        if match.group(1) is None:
            implicit_index += 1
        result.append((index, conversion.lower()))
    return sorted(result)


def compare_entry(
    source_label: str,
    locale_label: str,
    canonical: Entry,
    translated: Entry,
    errors: list[str],
) -> None:
    if canonical.formatted != translated.formatted:
        fail(f"{locale_label}: {canonical.kind}/{canonical.name} changes formatted attribute", errors)
    if canonical.kind == "string-array" and set(canonical.values) != set(translated.values):
        fail(f"{locale_label}: string-array/{canonical.name} item count does not match {source_label}", errors)
    if not canonical.formatted:
        return
    for variant, translated_value in translated.values.items():
        canonical_value = canonical.values.get(variant)
        if canonical_value is None and canonical.kind == "plurals":
            canonical_value = canonical.values.get("other")
        if canonical_value is None:
            fail(
                f"{locale_label}: {canonical.kind}/{canonical.name} has unsupported variant {variant!r}",
                errors,
            )
            continue
        expected = placeholders(canonical_value)
        actual = placeholders(translated_value)
        if expected != actual:
            fail(
                f"{locale_label}: {canonical.kind}/{canonical.name}[{variant}] placeholders "
                f"{actual} do not match {source_label} {expected}",
                errors,
            )


def load_resource_directory(
    directory: Path, errors: list[str]
) -> tuple[dict[tuple[str, str], Entry], dict[tuple[str, str], Path]]:
    """Android merges resource keys across every values XML file."""
    catalog: dict[tuple[str, str], Entry] = {}
    origins: dict[tuple[str, str], Path] = {}
    for path in sorted(directory.glob("*.xml")):
        entries = load_catalog(path, errors)
        for key, entry in entries.items():
            if key in catalog:
                fail(
                    f"{path_label(path)}: duplicate <{key[0]}> name={key[1]!r}; "
                    f"also defined in {path_label(origins[key])}",
                    errors,
                )
            catalog[key] = entry
            origins[key] = path
    return catalog, origins


def locale_directories(resource_root: Path) -> list[Path]:
    if not resource_root.is_dir():
        return []
    return sorted(
        path for path in resource_root.iterdir()
        if path.is_dir() and LOCALE_DIR.fullmatch(path.name)
        and any(path.glob("*.xml"))
    )


def validate_source_set(source_set: Path, errors: list[str]) -> int:
    resource_root = source_set / "res"
    canonical, origins = load_resource_directory(resource_root / "values", errors)
    if not canonical:
        return 0

    locale_paths = locale_directories(resource_root)
    # Flavor resources can otherwise silently fall back if an entire Korean
    # directory is removed, rather than just one key inside it.
    existing_tags = {qualifier_to_tag(path.name) for path in locale_paths}
    for main_locale in locale_directories(source_set.parent / "main" / "res"):
        tag = qualifier_to_tag(main_locale.name)
        if tag in COMPLETE_CATALOG_LOCALES and tag not in existing_tags:
            locale_paths.append(resource_root / main_locale.name)
    for locale_path in locale_paths:
        translated, _ = load_resource_directory(locale_path, errors)
        required = {
            key for key in canonical
            if qualifier_to_tag(locale_path.name) in COMPLETE_CATALOG_LOCALES
            or origins[key].name not in NEW_FALLBACK_CATALOGS
        }
        missing = sorted(required - set(translated))
        extra = sorted(set(translated) - set(canonical))
        for kind, name in missing:
            fail(f"{source_set.name}/{locale_path.name}: missing {kind}/{name}", errors)
        for kind, name in extra:
            fail(f"{source_set.name}/{locale_path.name}: extra {kind}/{name}", errors)
        for key in sorted(set(canonical) & set(translated)):
            compare_entry(
                f"{source_set.name}/values",
                f"{source_set.name}/{locale_path.name}",
                canonical[key],
                translated[key],
                errors,
            )
    return len(locale_paths)


def source_catalog_hashes(errors: list[str], app_src: Path | None = None) -> dict[str, str]:
    """Fingerprint every translatable canonical file, including its filename."""
    hashes = {}
    for source_set in sorted((app_src or APP_SRC).iterdir()):
        if not source_set.is_dir():
            continue
        _, origins = load_resource_directory(source_set / "res" / "values", errors)
        paths = sorted(set(origins.values()))
        if not paths:
            continue
        digest = hashlib.sha256()
        for path in paths:
            digest.update(path.name.encode("utf-8") + b"\0")
            digest.update(path.read_text(encoding="utf-8").replace("\r\n", "\n").encode("utf-8"))
            digest.update(b"\0")
        hashes[source_set.name] = digest.hexdigest()
    return hashes


def qualifier_to_tag(qualifier: str) -> str:
    value = qualifier.removeprefix("values-")
    if value.startswith("b+"):
        return value.removeprefix("b+").replace("+", "-")
    parts = value.split("-")
    # Only strip the region "r" prefix (e.g. values-en-rUS -> en-US); a plain
    # two-letter language code (e.g. values-ru) must be kept as-is.
    return "-".join(
        part[1:] if len(part) > 2 and part.startswith("r") else part
        for part in parts
    )


def validate_locale_config(errors: list[str]) -> None:
    config_path = APP_SRC / "main" / "res" / "xml" / "locales_config.xml"
    try:
        config = ET.parse(config_path).getroot()
    except (ET.ParseError, OSError) as exc:
        fail(f"{config_path.relative_to(REPO_ROOT)}: cannot parse XML: {exc}", errors)
        return
    configured = {
        node.attrib[f"{ANDROID_NS}name"]
        for node in config.findall("locale")
        if f"{ANDROID_NS}name" in node.attrib
    }
    discovered = {
        qualifier_to_tag(path.name)
        for path in locale_directories(APP_SRC / "main" / "res")
    }
    expected = {"en", *discovered}
    if configured != expected:
        fail(
            "locale_config.xml entries "
            f"{sorted(configured)} do not match Android catalogs {sorted(expected)}",
            errors,
        )


def validate_status_registry(errors: list[str]) -> None:
    try:
        data = json.loads(STATUS_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"{STATUS_PATH.relative_to(REPO_ROOT)}: cannot read status registry: {exc}", errors)
        return

    if data.get("schema_version") != 3:
        fail("localization status schema_version must be 3", errors)
    if data.get("canonical_locale") != "en":
        fail("localization status canonical_locale must be 'en'", errors)

    locales = data.get("locales")
    if not isinstance(locales, dict):
        fail("localization status locales must be an object", errors)
        return

    discovered = {
        qualifier_to_tag(path.name)
        for path in locale_directories(APP_SRC / "main" / "res")
    }
    expected = {"en", *discovered}
    if set(locales) != expected:
        fail(
            f"localization status locales {sorted(locales)} do not match shipped locales {sorted(expected)}",
            errors,
        )

    source_hashes = source_catalog_hashes(errors)

    for tag, entry in locales.items():
        if not isinstance(entry, dict):
            fail(f"localization status {tag!r} must be an object", errors)
            continue
        verification = entry.get("verification")
        if verification not in VERIFICATION_STATES:
            fail(f"localization status {tag!r} has invalid verification {verification!r}", errors)
        if tag == "en" and verification != "canonical":
            fail("English localization status must be canonical", errors)
        if tag != "en" and verification == "canonical":
            fail(f"non-English locale {tag!r} cannot be canonical", errors)
        if not isinstance(entry.get("native_name"), str) or not entry["native_name"].strip():
            fail(f"localization status {tag!r} requires native_name", errors)
        review_refs = entry.get("review_refs")
        if not isinstance(review_refs, list) or any(not isinstance(ref, str) for ref in review_refs):
            fail(f"localization status {tag!r} review_refs must be a string array", errors)
        elif verification in {"community-reviewed", "verified"} and not review_refs:
            fail(f"localization status {tag!r} requires review_refs for {verification}", errors)
        elif any(not ref.startswith("https://github.com/") for ref in review_refs):
            fail(f"localization status {tag!r} review_refs must be GitHub URLs", errors)
        surfaces = entry.get("surfaces")
        if not isinstance(surfaces, dict) or surfaces.get("android") not in {"canonical", "complete"}:
            fail(f"localization status {tag!r} must track the Android surface", errors)
        if tag != "en" and entry.get("source_sha256") != source_hashes:
            fail(
                f"localization status {tag!r} is stale; translate the current English catalogs "
                "and refresh source_sha256",
                errors,
            )


def main() -> int:
    errors: list[str] = []
    locale_count = sum(
        validate_source_set(source_set, errors)
        for source_set in sorted(APP_SRC.iterdir())
        if source_set.is_dir()
    )
    validate_locale_config(errors)
    validate_status_registry(errors)
    if locale_count == 0:
        errors.append("No Android locale catalogs were discovered")
    if errors:
        print("Android locale validation failed:", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        return 1
    print(f"Android locale validation passed ({locale_count} catalog(s))")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
