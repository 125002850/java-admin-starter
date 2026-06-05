# Template Project Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为模板仓库新增一个初始化脚本，在模板仓库根目录执行后生成独立新仓库，并按目标项目名与显式包名完成模块、配置和源码目录重命名。

**Architecture:** 采用 Python 3 标准库实现单文件脚本，复制模板目录后在目标目录内完成结构化目录迁移与受控文本替换。通过脚本级 Python 回归测试覆盖端到端 happy path 与目标目录保护逻辑，避免依赖 Maven 构建来验证模板化行为。

**Tech Stack:** Python 3、unittest、tempfile、pathlib、shutil、subprocess

---

### Task 1: 新增脚本级回归测试

**Files:**
- Create: `scripts/tests/test_init_template_project.py`

- [ ] **Step 1: Write the failing test**

```python
import subprocess
import tempfile
import unittest
from pathlib import Path


class InitTemplateProjectTests(unittest.TestCase):
    def test_bootstraps_clean_repository_with_renamed_project_assets(self) -> None:
        repo_root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "track-bench"
            result = subprocess.run(
                [
                    "python3",
                    "scripts/init_template_project.py",
                    "--target",
                    str(target_dir),
                    "--package",
                    "com.trackbench",
                ],
                cwd=repo_root,
                capture_output=True,
                text=True,
            )

        self.assertEqual(result.returncode, 0, msg=result.stderr)
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 -m unittest scripts.tests.test_init_template_project -v`  
Expected: FAIL，错误原因为 `scripts/init_template_project.py` 不存在

- [ ] **Step 3: 扩展同一个测试的断言范围**

```python
            self.assertTrue((target_dir / ".git").is_dir())
            self.assertTrue((target_dir / "track-bench-boot").is_dir())
            self.assertTrue((target_dir / "track-bench-core").is_dir())
            self.assertTrue((target_dir / "track-bench-mdm").is_dir())
            self.assertTrue((target_dir / "track-bench-system").is_dir())
            self.assertTrue(
                (
                    target_dir
                    / "track-bench-boot/src/main/java/com/trackbench/boot/BootApplication.java"
                ).is_file()
            )
            self.assertIn(
                "<artifactId>track-bench</artifactId>",
                (target_dir / "pom.xml").read_text(encoding="utf-8"),
            )
            self.assertIn(
                "spring.application.name: track-bench",
                (target_dir / "track-bench-boot/src/main/resources/application.yml")
                .read_text(encoding="utf-8"),
            )
```

- [ ] **Step 4: 新增目标目录保护测试**

```python
    def test_rejects_non_empty_target_directory(self) -> None:
        repo_root = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "track-bench"
            target_dir.mkdir()
            (target_dir / "keep.txt").write_text("keep", encoding="utf-8")

            result = subprocess.run(
                [
                    "python3",
                    "scripts/init_template_project.py",
                    "--target",
                    str(target_dir),
                    "--package",
                    "com.trackbench",
                ],
                cwd=repo_root,
                capture_output=True,
                text=True,
            )

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("目标目录已存在且非空", result.stderr)
```

- [ ] **Step 5: Run tests to verify they still fail for the missing script**

Run: `python3 -m unittest scripts.tests.test_init_template_project -v`  
Expected: 至少一个 FAIL，原因仍是脚本不存在

### Task 2: 实现模板初始化脚本

**Files:**
- Create: `scripts/init_template_project.py`

- [ ] **Step 1: 创建脚本骨架与参数校验**

```python
#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


PACKAGE_SEGMENT_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", required=True)
    parser.add_argument("--package", required=True)
    return parser.parse_args()


def validate_package_name(package_name: str) -> None:
    segments = package_name.split(".")
    if not segments or any(not segment for segment in segments):
        raise ValueError("包名不合法")
    if any(PACKAGE_SEGMENT_RE.fullmatch(segment) is None for segment in segments):
        raise ValueError("包名不合法")


def main() -> int:
    args = parse_args()
    validate_package_name(args.package)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
```

- [ ] **Step 2: 实现目录复制与目标目录保护**

```python
import shutil


EXCLUDE_NAMES = {".git", "target", ".DS_Store", ".claude", ".playwright-mcp", ".vscode"}


def ensure_target_directory(target_dir: Path) -> None:
    if target_dir.exists() and any(target_dir.iterdir()):
        raise ValueError("目标目录已存在且非空")
    target_dir.mkdir(parents=True, exist_ok=True)


def ignore_entries(_: str, names: list[str]) -> set[str]:
    return {name for name in names if name in EXCLUDE_NAMES}


def copy_template(source_root: Path, target_dir: Path) -> None:
    shutil.copytree(source_root, target_dir, dirs_exist_ok=True, ignore=ignore_entries)
```

- [ ] **Step 3: 实现模块目录、包目录和启动类重命名**

```python
MODULE_SUFFIXES = ("boot", "core", "mdm", "system")


def rename_module_directories(project_root: Path, project_name: str) -> dict[str, str]:
    mapping: dict[str, str] = {}
    for suffix in MODULE_SUFFIXES:
        old_name = f"demo-{suffix}"
        new_name = f"{project_name}-{suffix}"
        (project_root / old_name).rename(project_root / new_name)
        mapping[old_name] = new_name
    return mapping


def package_to_path(package_name: str) -> Path:
    return Path(*package_name.split("."))


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


def rename_boot_application(project_root: Path, package_name: str, module_name: str) -> None:
    boot_dir = project_root / module_name / "src/main/java" / package_to_path(package_name) / "boot"
    source = boot_dir / "DemoBootApplication.java"
    if source.exists():
        source.rename(boot_dir / "BootApplication.java")
```

- [ ] **Step 4: 实现关键文本替换与 `git init`**

```python
TEXT_FILE_SUFFIXES = {".md", ".xml", ".yml", ".yaml", ".java", ".properties", ".sh", ".txt"}


def build_replacements(project_name: str, package_name: str, module_mapping: dict[str, str]) -> dict[str, str]:
    database_name = project_name.replace("-", "_")
    replacements = {
        "java-demo-feature-sso": project_name,
        "java_demo_sso": database_name,
        "java-demo": project_name,
        "com.demo": package_name,
        "DemoBootApplication": "BootApplication",
        "${user.home}/.java-demo/uploads": f"${{user.home}}/.{project_name}/uploads",
        "${java.io.tmpdir}/java-demo/test-uploads": f"${{java.io.tmpdir}}/{project_name}/test-uploads",
    }
    replacements.update(module_mapping)
    return replacements


def replace_text_content(project_root: Path, replacements: dict[str, str]) -> None:
    for path in project_root.rglob("*"):
        if not path.is_file() or path.suffix not in TEXT_FILE_SUFFIXES:
            continue
        content = path.read_text(encoding="utf-8")
        updated = content
        for source, target in replacements.items():
            updated = updated.replace(source, target)
        if updated != content:
            path.write_text(updated, encoding="utf-8")


def init_git_repository(project_root: Path) -> None:
    subprocess.run(["git", "init"], cwd=project_root, check=True, capture_output=True, text=True)
```

- [ ] **Step 5: 组装主流程并输出结果**

```python
def main() -> int:
    args = parse_args()
    source_root = Path(__file__).resolve().parents[1]
    target_dir = Path(args.target).expanduser().resolve()
    project_name = target_dir.name
    validate_package_name(args.package)
    ensure_target_directory(target_dir)
    copy_template(source_root, target_dir)
    module_mapping = rename_module_directories(target_dir, project_name)
    for module_name in module_mapping.values():
        move_package_tree(target_dir / module_name, "com.demo", args.package)
    rename_boot_application(target_dir, args.package, module_mapping["demo-boot"])
    replace_text_content(target_dir, build_replacements(project_name, args.package, module_mapping))
    init_git_repository(target_dir)
    print(f"项目已初始化: {target_dir}")
    return 0
```

### Task 3: 验证与整理

**Files:**
- Verify: `scripts/tests/test_init_template_project.py`
- Verify: `scripts/init_template_project.py`

- [ ] **Step 1: Run test to verify it passes**

Run: `python3 -m unittest scripts.tests.test_init_template_project -v`  
Expected: PASS

- [ ] **Step 2: 手工执行一次 smoke check**

Run:

```bash
TMP_DIR="$(mktemp -d)"
python3 scripts/init_template_project.py --target "$TMP_DIR/track-bench" --package com.trackbench
find "$TMP_DIR/track-bench" -maxdepth 2 -type d | sort
```

Expected: 输出包含 `track-bench-boot`、`track-bench-core`、`track-bench-mdm`、`track-bench-system`

- [ ] **Step 3: 清理 smoke 目录并复查 git 状态**

Run:

```bash
rm -rf "$TMP_DIR"
git status --short
```

Expected: 仅显示本次新增/修改的脚本、测试和计划文档
