#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
from pathlib import Path


PACKAGE_SEGMENT_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
MODULE_SUFFIXES = ("boot", "core", "iam", "system")
EXCLUDE_NAMES = {
    ".git",
    ".DS_Store",
    ".claude",
    ".playwright-mcp",
    ".vscode",
    "__pycache__",
    "target",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Bootstrap a clean project repository from the current template repository."
    )
    parser.add_argument("--target", required=True, help="Target project directory.")
    parser.add_argument("--package", required=True, help="Target Java package name.")
    return parser.parse_args()


def validate_package_name(package_name: str) -> None:
    segments = package_name.split(".")
    if not segments or any(not segment for segment in segments):
        raise ValueError("包名不合法")
    if any(PACKAGE_SEGMENT_RE.fullmatch(segment) is None for segment in segments):
        raise ValueError("包名不合法")


def ensure_target_directory(source_root: Path, target_dir: Path) -> None:
    if target_dir == source_root or source_root in target_dir.parents:
        raise ValueError("目标目录不能位于模板仓库内部")

    if target_dir.exists() and any(target_dir.iterdir()):
        raise ValueError("目标目录已存在且非空")

    target_dir.mkdir(parents=True, exist_ok=True)


def ignore_entries(_: str, names: list[str]) -> set[str]:
    return {name for name in names if name in EXCLUDE_NAMES}


def copy_template(source_root: Path, target_dir: Path) -> None:
    shutil.copytree(source_root, target_dir, dirs_exist_ok=True, ignore=ignore_entries)


def rename_module_directories(project_root: Path, project_name: str) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for suffix in MODULE_SUFFIXES:
        old_name = f"admin-{suffix}"
        new_name = f"{project_name}-{suffix}"
        source = project_root / old_name
        target = project_root / new_name
        if source.exists():
            source.rename(target)
        mapping[old_name] = new_name
    return mapping


def package_to_path(package_name: str) -> Path:
    return Path(*package_name.split("."))


def remove_empty_parent_chain(path: Path, stop_at: Path) -> None:
    current = path
    while current != stop_at and current.exists():
        try:
            current.rmdir()
        except OSError:
            break
        current = current.parent


def move_package_tree(module_dir: Path, source_package: str, target_package: str) -> None:
    for scope in ("main", "test"):
        java_root = module_dir / "src" / scope / "java"
        if not java_root.exists():
            continue

        source_dir = java_root / package_to_path(source_package)
        if not source_dir.exists():
            continue

        target_dir = java_root / package_to_path(target_package)
        target_dir.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(source_dir), str(target_dir))
        remove_empty_parent_chain(source_dir.parent, java_root)


def rename_boot_application(project_root: Path, package_name: str, boot_module_name: str) -> None:
    boot_dir = project_root / boot_module_name / "src/main/java" / package_to_path(package_name) / "boot"
    source = boot_dir / "AdminBootApplication.java"
    if source.exists():
        source.rename(boot_dir / "BootApplication.java")


def build_replacements(
    project_name: str,
    package_name: str,
) -> list[tuple[str, str]]:
    database_name = project_name.replace("-", "_")
    environment_prefix = database_name.upper()
    package_path = package_name.replace(".", "/")
    return [
        ("java-admin-starter-feature-sso", project_name),
        ("java_admin_starter_sso", database_name),
        ("JAVA_ADMIN_STARTER", environment_prefix),
        ("java_admin_starter", database_name),
        ("admin_", f"{database_name}_"),
        ("AdminBootApplication", "BootApplication"),
        ("com/example/admin", package_path),
        ("com.example.admin", package_name),
        ("java-admin-starter", project_name),
        ("admin-", f"{project_name}-"),
    ]


def is_skipped_path(path: Path) -> bool:
    return any(part in EXCLUDE_NAMES for part in path.parts)


def replace_text_content(project_root: Path, replacements: list[tuple[str, str]]) -> None:
    replacement_map = dict(replacements)
    replacement_pattern = re.compile(
        "|".join(re.escape(source) for source in sorted(replacement_map, key=len, reverse=True))
    )

    for path in project_root.rglob("*"):
        if not path.is_file() or is_skipped_path(path):
            continue

        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        updated = replacement_pattern.sub(
            lambda match: replacement_map[match.group(0)],
            content,
        )

        if updated != content:
            path.write_text(updated, encoding="utf-8")


def init_git_repository(project_root: Path) -> None:
    subprocess.run(
        ["git", "init"],
        cwd=project_root,
        check=True,
        capture_output=True,
        text=True,
    )


def main() -> int:
    args = parse_args()
    source_root = Path(__file__).resolve().parents[1]
    target_dir = Path(args.target).expanduser().resolve()
    project_name = target_dir.name

    validate_package_name(args.package)
    ensure_target_directory(source_root, target_dir)
    copy_template(source_root, target_dir)

    module_mapping = rename_module_directories(target_dir, project_name)
    for module_dir_name in module_mapping.values():
        move_package_tree(target_dir / module_dir_name, "com.example.admin", args.package)

    rename_boot_application(target_dir, args.package, module_mapping["admin-boot"])
    replace_text_content(target_dir, build_replacements(project_name, args.package))
    init_git_repository(target_dir)

    print(f"项目已初始化: {target_dir}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
    except subprocess.CalledProcessError as exc:
        print(exc.stderr or str(exc), file=sys.stderr)
        raise SystemExit(exc.returncode or 1)
