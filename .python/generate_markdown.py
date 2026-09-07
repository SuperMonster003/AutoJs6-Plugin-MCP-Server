# -*- coding: utf-8 -*-
"""Generate localized documentation from JSON sources.

Source of truth:
  .readme/common.json                    language-neutral facts (URLs, IDs, defaults)
  .readme/lang_<code>.json               localized README copy (10 languages)
  .readme/template_readme.md             README skeleton with {{ placeholder }} slots
  .readme/template_plugin_instruction.md plugin-center instruction skeleton
  .changelog/lang_<code>.json            localized changelog labels and $data entries
  .changelog/template_changelog.md       changelog skeleton
  version.properties                     VERSION_NAME (must match the newest changelog entry)

Generated artifacts (36 in total, never edit them by hand):
  .readme/README-<code>.md                       x 10
  README.md                                      copy of the default language (zh-Hans)
  app/src/main/assets/doc/CHANGELOG-<name>.md    x 13 (zh-Hans/HK/TW expand to Android aliases)
  app/src/main/assets/doc/CHANGELOG.md           copy of the default language
  app/src/main/res/raw*/plugin_instruction.md    x 11 (10 locales plus the English default)

Usage:
  py .python/generate_markdown.py            regenerate all artifacts
  py .python/generate_markdown.py --check    verify artifacts match sources (CI gate, writes nothing)

Exit protocol: prints MARKDOWN_OK on success, MARKDOWN_ERROR <reason> on failure (exit code 1).
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from pathlib import Path
from typing import Any

LANGUAGE_CODES = [
    "zh-Hans",
    "zh-Hant-HK",
    "zh-Hant-TW",
    "en",
    "fr",
    "es",
    "ja",
    "ko",
    "ru",
    "ar",
]
LANGUAGE_CODE_DEFAULT = "zh-Hans"
ANDROID_CHANGELOG_ALIASES = {
    "zh-Hans": ["zh", "zh-Hans"],
    "zh-Hant-HK": ["zh-rHK", "zh-Hant-HK"],
    "zh-Hant-TW": ["zh-rTW", "zh-Hant-TW"],
}
ANDROID_INSTRUCTION_DIRECTORIES = {
    "zh-Hans": "raw-zh",
    "zh-Hant-HK": "raw-zh-rHK",
    "zh-Hant-TW": "raw-zh-rTW",
    "en": "raw-en",
    "fr": "raw-fr",
    "es": "raw-es",
    "ja": "raw-ja",
    "ko": "raw-ko",
    "ru": "raw-ru",
    "ar": "raw-ar",
}
ANDROID_DEFAULT_LANGUAGE = "en"

CHANGELOG_CATEGORIES = ["hint", "feature", "fix", "improvement", "dependency"]
CHANGELOG_LABEL_KEYS = [f"changelog_label_{category}" for category in CHANGELOG_CATEGORIES]
CHANGELOG_DATA_KEY = "$data"

README_LIST_KEYS = ["features", "usage_steps", "security_points"]

EXPECTED_ARTIFACT_COUNT = 36
README_LATEST_RELEASES = 3

PLACEHOLDER_MARKERS = (
    "TODO_TRANSLATION",
    "TRANSLATION_PENDING",
    "MACHINE_TRANSLATION_PLACEHOLDER",
)
TEMPLATE_PATTERN = re.compile(r"\{\{\s*([A-Za-z0-9_$.-]+)\s*\}\}")
RELEASED_DATE_PATTERN = re.compile(r"^\d{4}/\d{2}/\d{2}$")


class MarkdownGenerationError(Exception):
    """Raised when sources are inconsistent or artifacts cannot be produced."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise MarkdownGenerationError(message)


# ---------------------------------------------------------------------------
# Source loading and hygiene checks
# ---------------------------------------------------------------------------

def reject_duplicate_pairs(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        require(key not in result, f"Duplicate JSON key: {key!r}")
        result[key] = value
    return result


def validate_no_fullwidth_symbols(path: Path, text: str) -> None:
    for line_number, line in enumerate(text.splitlines(), start=1):
        for ch in line:
            if unicodedata.east_asian_width(ch) in ("F", "W") and unicodedata.category(ch)[0] in ("P", "S", "Z"):
                raise MarkdownGenerationError(
                    f"Fullwidth symbol {ch!r} (U+{ord(ch):04X}) in {path} at line {line_number}"
                )


def validate_no_placeholder_markers(path: Path, text: str) -> None:
    for marker in PLACEHOLDER_MARKERS:
        require(marker not in text, f"Translation placeholder {marker!r} left in {path}")


def load_text(path: Path) -> str:
    require(path.is_file(), f"Missing source file: {path}")
    require(not path.is_symlink(), f"Refusing to read symlink: {path}")
    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError as error:
        raise MarkdownGenerationError(f"Invalid UTF-8 in {path}: {error}") from None
    validate_no_fullwidth_symbols(path, text)
    validate_no_placeholder_markers(path, text)
    return text


def load_json(path: Path) -> dict[str, Any]:
    text = load_text(path)
    try:
        data = json.loads(text, object_pairs_hook=reject_duplicate_pairs)
    except MarkdownGenerationError as error:
        raise MarkdownGenerationError(f"{error} in {path}") from None
    except json.JSONDecodeError as error:
        raise MarkdownGenerationError(f"Invalid JSON in {path}: {error}") from None
    require(isinstance(data, dict), f"JSON root must be an object: {path}")
    return data


# ---------------------------------------------------------------------------
# Cross-language shape validation
# ---------------------------------------------------------------------------

def validate_key_parity(items: dict[str, dict[str, Any]], kind: str) -> None:
    reference = set(items[LANGUAGE_CODE_DEFAULT])
    for code, content in items.items():
        keys = set(content)
        missing = sorted(reference - keys)
        extra = sorted(keys - reference)
        require(
            not missing and not extra,
            f"{kind} key mismatch for {code!r}: missing={missing} extra={extra}",
        )


def shape_of(value: Any) -> Any:
    if isinstance(value, dict):
        return ("dict", tuple(sorted(value)))
    if isinstance(value, list):
        return ("list", len(value), tuple(shape_of(item) for item in value))
    return type(value).__name__


def validate_collection_shapes(items: dict[str, dict[str, Any]], kind: str) -> None:
    reference = items[LANGUAGE_CODE_DEFAULT]
    for code, content in items.items():
        for key, value in content.items():
            require(
                shape_of(value) == shape_of(reference[key]),
                f"{kind} field {key!r} for {code!r} does not match the "
                f"{LANGUAGE_CODE_DEFAULT!r} shape (type, list length, or object keys differ)",
            )


def validate_readme_language(code: str, content: dict[str, Any]) -> None:
    for key, value in content.items():
        if key in README_LIST_KEYS:
            continue
        require(isinstance(value, str) and value, f"README {code!r} field {key!r} must be non-empty text")
    for key in README_LIST_KEYS:
        require(key in content, f"README language {code!r} is missing the {key!r} list")
        require(isinstance(content[key], list) and content[key], f"README {code!r} field {key!r} must be a non-empty list")
        for item in content[key]:
            require(isinstance(item, str) and item, f"README {code!r} field {key!r} must contain non-empty strings only")
    synopsis = content["text_plugin_synopsis"]
    require(
        not synopsis.endswith((".", "!", "?")),
        f"README {code!r} text_plugin_synopsis must not end with terminal punctuation",
    )


def validate_changelog_language(code: str, content: dict[str, Any]) -> None:
    expected_keys = set(CHANGELOG_LABEL_KEYS) | {CHANGELOG_DATA_KEY}
    require(
        set(content) == expected_keys,
        f"Changelog {code!r} must contain exactly the label keys and {CHANGELOG_DATA_KEY!r}",
    )
    data = content[CHANGELOG_DATA_KEY]
    require(isinstance(data, dict) and data, f"Changelog {code!r} {CHANGELOG_DATA_KEY!r} must be a non-empty object")
    for version, entry in data.items():
        require(isinstance(entry, dict), f"Changelog {code!r} entry {version!r} must be an object")
        require("released_date" in entry, f"Changelog {code!r} entry {version!r} is missing released_date")
        require(
            RELEASED_DATE_PATTERN.match(str(entry["released_date"])) is not None,
            f"Changelog {code!r} entry {version!r} released_date must look like YYYY/MM/DD",
        )
        unknown = sorted(set(entry) - {"released_date"} - set(CHANGELOG_CATEGORIES))
        require(not unknown, f"Changelog {code!r} entry {version!r} has unknown fields: {unknown}")
        for category in CHANGELOG_CATEGORIES:
            if category in entry:
                require(
                    isinstance(entry[category], list) and entry[category],
                    f"Changelog {code!r} entry {version!r} category {category!r} must be a non-empty list",
                )


def validate_changelog_shapes(changelog_sources: dict[str, dict[str, Any]]) -> None:
    reference = changelog_sources[LANGUAGE_CODE_DEFAULT][CHANGELOG_DATA_KEY]
    reference_versions = list(reference)
    for code, content in changelog_sources.items():
        data = content[CHANGELOG_DATA_KEY]
        require(
            list(data) == reference_versions,
            f"Changelog {code!r} versions {list(data)} do not match "
            f"{LANGUAGE_CODE_DEFAULT!r} versions {reference_versions}",
        )
        for version, entry in data.items():
            reference_entry = reference[version]
            require(
                set(entry) == set(reference_entry),
                f"Changelog {code!r} entry {version!r} fields differ from {LANGUAGE_CODE_DEFAULT!r}",
            )
            require(
                entry["released_date"] == reference_entry["released_date"],
                f"Changelog {code!r} entry {version!r} released_date differs from {LANGUAGE_CODE_DEFAULT!r}",
            )
            for category in CHANGELOG_CATEGORIES:
                if category in reference_entry:
                    require(
                        len(entry[category]) == len(reference_entry[category]),
                        f"Changelog {code!r} entry {version!r} category {category!r} "
                        f"item count differs from {LANGUAGE_CODE_DEFAULT!r}",
                    )


# ---------------------------------------------------------------------------
# version.properties alignment
# ---------------------------------------------------------------------------

def read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for line in load_text(path).splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, _, value = stripped.partition("=")
        properties[key.strip()] = value.strip()
    return properties


def current_version_label(root: Path) -> str:
    version_name = read_properties(root / "version.properties").get("VERSION_NAME", "")
    require(bool(version_name), "VERSION_NAME is missing from version.properties")
    return version_name if version_name.startswith("v") else f"v{version_name}"


def validate_version_alignment(root: Path, changelog_sources: dict[str, dict[str, Any]]) -> str:
    label = current_version_label(root)
    newest = next(iter(changelog_sources[LANGUAGE_CODE_DEFAULT][CHANGELOG_DATA_KEY]))
    require(
        newest == label,
        f"The newest changelog entry {newest!r} does not match version.properties VERSION_NAME {label!r}",
    )
    return label


# ---------------------------------------------------------------------------
# Rendering helpers
# ---------------------------------------------------------------------------

def render_template(text: str, values: dict[str, Any]) -> str:
    def replace(match: re.Match[str]) -> str:
        key = match.group(1).strip()
        require(key in values, f"Missing template value: {key}")
        return str(values[key])

    return TEMPLATE_PATTERN.sub(replace, text)


def render_dynamic(value: Any, values: dict[str, Any]) -> Any:
    if isinstance(value, dict):
        return {key: render_dynamic(item, values) for key, item in value.items()}
    if isinstance(value, list):
        return [render_dynamic(item, values) for item in value]
    if isinstance(value, str):
        return render_template(value, values)
    return value


def bullet_list(items: list[str]) -> str:
    return "\n".join(f"- {item}" for item in items)


def numbered_list(items: list[str]) -> str:
    return "\n".join(f"{index}. {item}" for index, item in enumerate(items, start=1))


def markdown_link(label: str, url: str) -> str:
    return f"[{label}]({url})"


# ---------------------------------------------------------------------------
# Source assembly
# ---------------------------------------------------------------------------

def load_languages(root: Path) -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    readme_dir = root / ".readme"
    changelog_dir = root / ".changelog"
    common = load_json(readme_dir / "common.json")
    version_name = current_version_label(root).removeprefix("v")

    raw_languages = {code: load_json(readme_dir / f"lang_{code}.json") for code in LANGUAGE_CODES}
    raw_changelogs = {code: load_json(changelog_dir / f"lang_{code}.json") for code in LANGUAGE_CODES}

    validate_key_parity(raw_languages, "README")
    validate_collection_shapes(raw_languages, "README")
    for code in LANGUAGE_CODES:
        validate_readme_language(code, raw_languages[code])
        validate_changelog_language(code, raw_changelogs[code])
    validate_changelog_shapes(raw_changelogs)
    validate_version_alignment(root, raw_changelogs)

    languages: dict[str, dict[str, Any]] = {}
    changelogs: dict[str, dict[str, Any]] = {}
    for code in LANGUAGE_CODES:
        merged_language = {
            **common,
            **raw_languages[code],
            "version_name": version_name,
        }
        # Two passes let common values reference each other (for example license_url -> repo_url).
        merged_language = render_dynamic(merged_language, merged_language)
        languages[code] = render_dynamic(merged_language, merged_language)

        raw_changelog = raw_changelogs[code]
        changelog_values = {key: value for key, value in raw_changelog.items() if key != CHANGELOG_DATA_KEY}
        changelog_values = render_dynamic(changelog_values, {**languages[code], **changelog_values})
        changelogs[code] = {
            "values": changelog_values,
            "data": render_dynamic(raw_changelog[CHANGELOG_DATA_KEY], {**languages[code], **changelog_values}),
        }

    return languages, changelogs


def format_changelog_items(changelog: dict[str, Any], limit: int | None = None, heading_level: int = 1) -> str:
    values = changelog["values"]
    heading = "#" * heading_level
    bullet = "*" if heading_level == 1 else "-"
    chunks = []
    for index, (version_name, item) in enumerate(changelog["data"].items()):
        if limit is not None and index >= limit:
            break
        date_line = f"###### {item['released_date']}" if heading_level == 1 else f"_{item['released_date']}_"
        lines = [f"{heading} {version_name}", "", date_line, ""]
        for category in CHANGELOG_CATEGORIES:
            for text in item.get(category, []):
                lines.append(f"{bullet} `{values[f'changelog_label_{category}']}` {text}")
        chunks.append("\n".join(lines).rstrip())
    return "\n\n".join(chunks).rstrip() + "\n"


def build_language_list(target_code: str, languages: dict[str, dict[str, Any]]) -> str:
    repo_url = languages[target_code]["repo_url"]
    lines = []
    for code in LANGUAGE_CODES:
        content = languages[code]
        label = f"{content['$name']} [{code}]"
        if code == target_code:
            lines.append(f"- {label} # {content['text_current_lowercase']}")
        else:
            lines.append(f"- {markdown_link(label, f'{repo_url}/blob/master/.readme/README-{code}.md')}")
    return "\n".join(lines)


def build_readme_values(
    code: str,
    languages: dict[str, dict[str, Any]],
    changelogs: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    content = dict(languages[code])
    repo_url = content["repo_url"]
    content["placeholder_ul_languages_all_supported"] = build_language_list(code, languages)
    content["placeholder_features"] = bullet_list(content["features"])
    content["placeholder_usage_steps"] = numbered_list(content["usage_steps"])
    content["placeholder_security_points"] = bullet_list(content["security_points"])
    content["placeholder_latest_release_history"] = format_changelog_items(
        changelogs[code],
        limit=README_LATEST_RELEASES,
        heading_level=4,
    ).rstrip()
    content["placeholder_read_more_in_changelog_md"] = markdown_link(
        "CHANGELOG.md",
        f"{repo_url}/blob/master/app/src/main/assets/doc/CHANGELOG-{code}.md",
    )
    return content


# ---------------------------------------------------------------------------
# Artifact construction, drift detection, and writing
# ---------------------------------------------------------------------------

def build_artifacts(root: Path) -> dict[Path, str]:
    readme_dir = root / ".readme"
    changelog_dir = root / ".changelog"
    android_changelog_dir = root / "app" / "src" / "main" / "assets" / "doc"
    android_resource_dir = root / "app" / "src" / "main" / "res"

    languages, changelogs = load_languages(root)
    readme_template = load_text(readme_dir / "template_readme.md")
    instruction_template = load_text(readme_dir / "template_plugin_instruction.md")
    changelog_template = load_text(changelog_dir / "template_changelog.md")

    artifacts: dict[Path, str] = {}

    for code in LANGUAGE_CODES:
        values = dict(languages[code])
        values["placeholder_release_history"] = format_changelog_items(changelogs[code]).rstrip()
        output = render_template(changelog_template, values)
        for name in ANDROID_CHANGELOG_ALIASES.get(code, [code]):
            artifacts[android_changelog_dir / f"CHANGELOG-{name}.md"] = output
        if code == LANGUAGE_CODE_DEFAULT:
            artifacts[android_changelog_dir / "CHANGELOG.md"] = output

    for code in LANGUAGE_CODES:
        output = render_template(readme_template, build_readme_values(code, languages, changelogs))
        artifacts[readme_dir / f"README-{code}.md"] = output
        if code == LANGUAGE_CODE_DEFAULT:
            artifacts[root / "README.md"] = output

    for code in LANGUAGE_CODES:
        output = render_template(instruction_template, build_readme_values(code, languages, changelogs))
        directory = ANDROID_INSTRUCTION_DIRECTORIES[code]
        artifacts[android_resource_dir / directory / "plugin_instruction.md"] = output
        if code == ANDROID_DEFAULT_LANGUAGE:
            artifacts[android_resource_dir / "raw" / "plugin_instruction.md"] = output

    require(
        len(artifacts) == EXPECTED_ARTIFACT_COUNT,
        f"Expected {EXPECTED_ARTIFACT_COUNT} artifacts, produced {len(artifacts)}",
    )
    return artifacts


def generated_inventory(root: Path) -> set[Path]:
    inventory: set[Path] = set()
    readme_default = root / "README.md"
    if readme_default.is_file():
        inventory.add(readme_default)
    inventory.update((root / ".readme").glob("README-*.md"))
    inventory.update((root / "app" / "src" / "main" / "assets" / "doc").glob("CHANGELOG*.md"))
    inventory.update((root / "app" / "src" / "main" / "res").glob("raw*/plugin_instruction.md"))
    return inventory


def check_artifacts(root: Path, artifacts: dict[Path, str]) -> None:
    drift: list[str] = []
    for path, expected in sorted(artifacts.items()):
        if not path.is_file():
            drift.append(f"missing: {path.relative_to(root)}")
        elif path.read_text(encoding="utf-8") != expected:
            drift.append(f"stale: {path.relative_to(root)}")
    for path in sorted(generated_inventory(root) - set(artifacts)):
        drift.append(f"orphan: {path.relative_to(root)}")
    require(not drift, "artifact drift detected -> " + "; ".join(drift))


def write_artifacts(root: Path, artifacts: dict[Path, str]) -> None:
    for path, text in artifacts.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8", newline="\n")
        print(f"Generated {path.relative_to(root)}")
    orphans = sorted(generated_inventory(root) - set(artifacts))
    require(
        not orphans,
        "orphan generated files present -> " + "; ".join(str(path.relative_to(root)) for path in orphans),
    )


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def parse_arguments(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate localized README, plugin instruction, and CHANGELOG files")
    parser.add_argument("--check", action="store_true", help="verify artifacts match sources without writing")
    parser.add_argument("--root", type=Path, default=None, help=argparse.SUPPRESS)
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    arguments = parse_arguments(argv)
    root = (arguments.root or Path(__file__).resolve().parents[1]).resolve()
    mode = "check" if arguments.check else "write"
    try:
        require(LANGUAGE_CODE_DEFAULT in LANGUAGE_CODES, f"Default language {LANGUAGE_CODE_DEFAULT!r} is not supported")
        artifacts = build_artifacts(root)
        if arguments.check:
            check_artifacts(root, artifacts)
        else:
            write_artifacts(root, artifacts)
    except MarkdownGenerationError as error:
        print(f"MARKDOWN_ERROR {error}")
        return 1
    print(f"MARKDOWN_OK languages={len(LANGUAGE_CODES)} artifacts={len(artifacts)} mode={mode}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
