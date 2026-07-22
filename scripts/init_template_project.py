#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
import shutil
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
PROJECT_NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
PACKAGE_SEGMENT_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
INITIAL_BRANCH_NAME = "main"
JAVA_RESERVED_WORDS = frozenset(
    """
    _ abstract assert boolean break byte case catch char class const continue default
    do double else enum extends false final finally float for goto if implements import
    instanceof int interface long native new null package private protected public return
    short static strictfp super switch synchronized this throw throws transient true try
    void volatile while
    """.split()
)
LEGACY_COMPOSE_PROJECT_NAME = "java-admin-starter-feature-sso"
LEGACY_DEV_DATABASE_NAME = "java_admin_starter_sso"
LEGACY_TEST_DATABASE_NAME = "java_admin_starter_test"
LEGACY_REMOTE_DATABASE_NAME = "basic_platform_sso"
GENERATED_TEXT_REPLACEMENTS = {
    "当前 `feature/sso` 分支": "当前分支",
    "- 当前工作分支：`feature/sso`": "- 当前工作分支：`main`",
    "feature/sso gateway-SSO conventions": "gateway-SSO conventions",
    "只描述当前 `feature/sso` 网关 SSO 基座": "只描述当前网关 SSO 基座",
    "implement a feature/sso change": "implement a repository change",
}
EXCLUDE_NAMES = {
    ".codex",
    ".DS_Store",
    ".git",
    ".idea",
    ".mypy_cache",
    ".playwright-mcp",
    ".pytest_cache",
    ".ruff_cache",
    ".tox",
    ".venv",
    ".vscode",
    "__pycache__",
    "target",
}


@dataclass(frozen=True)
class TemplateMetadata:
    project_name: str
    package_name: str
    module_names: tuple[str, ...]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="从当前 Java 模板仓库初始化一个独立的新项目。"
    )
    parser.add_argument(
        "--project-name",
        required=True,
        help="项目名，使用小写 kebab-case，并写入 Maven POM。",
    )
    parser.add_argument(
        "--project-path",
        required=True,
        help="新项目的输出目录；目录名可以与项目名不同。",
    )
    parser.add_argument("--package", required=True, help="目标 Java 根包名。")
    return parser.parse_args()


def validate_project_name(project_name: str) -> None:
    if PROJECT_NAME_RE.fullmatch(project_name) is None:
        raise ValueError("项目名不合法，必须使用小写 kebab-case")


def validate_package_name(package_name: str) -> None:
    segments = package_name.split(".")
    if not segments or any(not segment for segment in segments):
        raise ValueError("包名不合法")
    if any(PACKAGE_SEGMENT_RE.fullmatch(segment) is None for segment in segments):
        raise ValueError("包名不合法")
    if any(segment in JAVA_RESERVED_WORDS for segment in segments):
        raise ValueError("包名不合法，不能使用 Java 关键字")


def read_template_metadata(source_root: Path) -> TemplateMetadata:
    pom_path = source_root / "pom.xml"
    try:
        root = ET.parse(pom_path).getroot()
    except (OSError, ET.ParseError) as exc:
        raise ValueError(f"无法读取模板根 POM: {pom_path}") from exc

    artifact_id = root.findtext("m:artifactId", namespaces=MAVEN_NAMESPACE)
    group_id = root.findtext("m:groupId", namespaces=MAVEN_NAMESPACE)
    module_elements = root.findall("m:modules/m:module", MAVEN_NAMESPACE)

    project_name = artifact_id.strip() if artifact_id else ""
    package_name = group_id.strip() if group_id else ""
    module_names = tuple(
        element.text.strip()
        for element in module_elements
        if element.text and element.text.strip()
    )

    if not project_name or not package_name or not module_names:
        raise ValueError("模板根 POM 缺少 groupId、artifactId 或 modules")
    validate_project_name(project_name)
    validate_package_name(package_name)

    for module_name in module_names:
        module_path = Path(module_name)
        if module_path.is_absolute() or len(module_path.parts) != 1:
            raise ValueError(f"仅支持根目录下的 Maven 模块: {module_name}")

    return TemplateMetadata(project_name, package_name, module_names)


def ensure_target_directory(source_root: Path, target_dir: Path) -> None:
    if target_dir == source_root or source_root in target_dir.parents:
        raise ValueError("目标目录不能位于模板仓库内部")

    if target_dir.exists() and any(target_dir.iterdir()):
        raise ValueError("目标目录已存在且非空")

    target_dir.mkdir(parents=True, exist_ok=True)


def ignore_entries(directory: str, names: list[str]) -> set[str]:
    ignored: set[str] = set()
    for name in names:
        is_local_env = (
            name in {".env", ".env.local"}
            or name.startswith(".env.") and name.endswith(".local")
        )
        is_claude_local_state = Path(directory).name == ".claude" and name != "skills"
        if name in EXCLUDE_NAMES or is_local_env or is_claude_local_state:
            ignored.add(name)
    return ignored


def copy_template(source_root: Path, target_dir: Path) -> None:
    shutil.copytree(
        source_root,
        target_dir,
        dirs_exist_ok=True,
        ignore=ignore_entries,
        symlinks=True,
    )


def canonical_module_name(module_name: str, source_project_name: str) -> str:
    project_prefix = f"{source_project_name}-"
    if module_name.startswith(project_prefix):
        return module_name[len(project_prefix) :]
    if module_name.startswith("admin-"):
        return module_name[len("admin-") :]
    return module_name


def rename_module_directories(
    project_root: Path,
    source_project_name: str,
    module_names: tuple[str, ...],
) -> dict[str, str]:
    mapping = {
        module_name: canonical_module_name(module_name, source_project_name)
        for module_name in module_names
    }
    if len(set(mapping.values())) != len(mapping):
        raise ValueError("Maven 模块重命名后发生名称冲突")

    for old_name, new_name in mapping.items():
        source = project_root / old_name
        target = project_root / new_name
        if not source.is_dir():
            raise ValueError(f"根 POM 声明的模块目录不存在: {old_name}")
        if source == target:
            continue
        if target.exists():
            raise ValueError(f"目标模块目录已存在: {new_name}")
        source.rename(target)

    return mapping


def find_boot_module_name(
    metadata: TemplateMetadata,
    module_mapping: dict[str, str],
) -> str:
    boot_modules = [
        module_name
        for module_name in metadata.module_names
        if canonical_module_name(module_name, metadata.project_name) == "boot"
    ]
    if len(boot_modules) != 1:
        raise ValueError("模板必须且只能包含一个 boot 模块")
    return module_mapping[boot_modules[0]]


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
    if source_package == target_package:
        return

    for scope in ("main", "test"):
        java_root = module_dir / "src" / scope / "java"
        if not java_root.exists():
            continue

        source_dir = java_root / package_to_path(source_package)
        if not source_dir.exists():
            continue

        staging_root = Path(tempfile.mkdtemp(prefix=".package-move-", dir=java_root))
        staged_source = staging_root / "source"
        source_dir.rename(staged_source)
        remove_empty_parent_chain(source_dir.parent, java_root)

        target_dir = java_root / package_to_path(target_package)
        if target_dir.exists():
            raise ValueError(f"目标包目录已存在: {target_dir}")
        target_dir.parent.mkdir(parents=True, exist_ok=True)
        staged_source.rename(target_dir)
        staging_root.rmdir()


def rename_boot_application(
    project_root: Path,
    package_name: str,
    boot_module_name: str,
) -> str:
    boot_dir = (
        project_root
        / boot_module_name
        / "src/main/java"
        / package_to_path(package_name)
        / "boot"
    )
    if not boot_dir.is_dir():
        raise ValueError(f"启动包目录不存在: {boot_dir}")

    canonical_file = boot_dir / "BootApplication.java"
    if canonical_file.is_file():
        return canonical_file.stem

    candidates = sorted(boot_dir.glob("*Application.java"))
    if len(candidates) != 1:
        raise ValueError("无法唯一识别 Spring Boot 启动类")

    source = candidates[0]
    source_class_name = source.stem
    source.rename(canonical_file)
    return source_class_name


def database_name(project_name: str) -> str:
    return project_name.replace("-", "_")


def environment_prefix(project_name: str) -> str:
    return database_name(project_name).upper()


def add_replacement(
    replacements: dict[str, str],
    source: str,
    target: str,
) -> None:
    if not source or source == target:
        return
    existing = replacements.get(source)
    if existing is not None and existing != target:
        raise ValueError(f"模板标识存在冲突替换: {source}")
    replacements[source] = target


def build_replacements(
    source_root: Path,
    target_dir: Path,
    metadata: TemplateMetadata,
    target_project_name: str,
    target_package_name: str,
    source_boot_class_name: str,
    module_mapping: dict[str, str],
) -> dict[str, str]:
    target_database_name = database_name(target_project_name)
    replacements: dict[str, str] = {}

    add_replacement(replacements, str(source_root), str(target_dir))
    add_replacement(
        replacements,
        LEGACY_COMPOSE_PROJECT_NAME,
        target_project_name,
    )
    add_replacement(replacements, LEGACY_DEV_DATABASE_NAME, target_database_name)
    add_replacement(
        replacements,
        LEGACY_TEST_DATABASE_NAME,
        f"{target_database_name}_test",
    )
    add_replacement(replacements, LEGACY_REMOTE_DATABASE_NAME, target_database_name)
    add_replacement(
        replacements,
        environment_prefix(metadata.project_name),
        environment_prefix(target_project_name),
    )
    add_replacement(
        replacements,
        database_name(metadata.project_name),
        target_database_name,
    )
    add_replacement(
        replacements,
        metadata.project_name,
        target_project_name,
    )
    add_replacement(
        replacements,
        metadata.package_name,
        target_package_name,
    )
    add_replacement(
        replacements,
        metadata.package_name.replace(".", "/"),
        target_package_name.replace(".", "/"),
    )
    add_replacement(
        replacements,
        source_boot_class_name,
        "BootApplication",
    )
    for source, target in GENERATED_TEXT_REPLACEMENTS.items():
        add_replacement(replacements, source, target)

    for old_name, new_name in module_mapping.items():
        add_replacement(replacements, old_name, new_name)

    return replacements


def is_skipped_path(path: Path) -> bool:
    return any(part in EXCLUDE_NAMES for part in path.parts)


def replace_text_content(project_root: Path, replacements: dict[str, str]) -> None:
    if not replacements:
        return

    replacement_pattern = re.compile(
        "|".join(
            re.escape(source)
            for source in sorted(replacements, key=len, reverse=True)
        )
    )
    for path in project_root.rglob("*"):
        if not path.is_file() or is_skipped_path(path):
            continue

        raw_content = path.read_bytes()
        if b"\0" in raw_content:
            continue
        try:
            content = raw_content.decode("utf-8")
        except UnicodeDecodeError:
            continue

        updated = replacement_pattern.sub(
            lambda match: replacements[match.group(0)],
            content,
        )
        if updated != content:
            path.write_bytes(updated.encode("utf-8"))


def init_git_repository(project_root: Path) -> None:
    subprocess.run(
        ["git", "init", "--initial-branch", INITIAL_BRANCH_NAME],
        cwd=project_root,
        check=True,
        capture_output=True,
        text=True,
    )


def main() -> int:
    args = parse_args()
    source_root = Path(__file__).resolve().parents[1]
    target_dir = Path(args.project_path).expanduser().resolve()
    target_project_name = args.project_name

    validate_project_name(target_project_name)
    validate_package_name(args.package)
    metadata = read_template_metadata(source_root)
    ensure_target_directory(source_root, target_dir)
    copy_template(source_root, target_dir)

    module_mapping = rename_module_directories(
        target_dir,
        metadata.project_name,
        metadata.module_names,
    )
    for module_dir_name in module_mapping.values():
        move_package_tree(
            target_dir / module_dir_name,
            metadata.package_name,
            args.package,
        )

    boot_module_name = find_boot_module_name(metadata, module_mapping)
    source_boot_class_name = rename_boot_application(
        target_dir,
        args.package,
        boot_module_name,
    )
    replacements = build_replacements(
        source_root,
        target_dir,
        metadata,
        target_project_name,
        args.package,
        source_boot_class_name,
        module_mapping,
    )
    replace_text_content(target_dir, replacements)
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
    except (OSError, shutil.Error) as exc:
        print(f"项目初始化失败: {exc}", file=sys.stderr)
        raise SystemExit(1)
