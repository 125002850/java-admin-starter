import subprocess
import tempfile
import unittest
from pathlib import Path


class InitTemplateProjectTests(unittest.TestCase):
    def setUp(self) -> None:
        self.repo_root = Path(__file__).resolve().parents[2]

    def run_initializer(
        self,
        source_root: Path,
        project_path: Path,
        project_name: str,
        package_name: str,
    ) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                "python3",
                "scripts/init_template_project.py",
                "--project-name",
                project_name,
                "--project-path",
                str(project_path),
                "--package",
                package_name,
            ],
            cwd=source_root,
            capture_output=True,
            text=True,
        )

    def test_bootstraps_clean_repository_with_current_project_assets(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "generated-project"
            result = self.run_initializer(
                self.repo_root,
                target_dir,
                "sample-service",
                "com.acme.sample",
            )

            self.assertEqual(result.returncode, 0, msg=result.stderr)
            self.assertTrue((target_dir / ".git").is_dir())
            self.assertTrue(
                (
                    target_dir
                    / ".agents/skills/oig-java-development/SKILL.md"
                ).is_file()
            )
            self.assertTrue((target_dir / ".claude/skills").is_symlink())
            self.assertEqual(
                str((target_dir / ".claude/skills").readlink()),
                "../.agents/skills",
            )
            self.assertFalse((target_dir / ".claude/settings.local.json").exists())
            self.assertFalse((target_dir / ".vscode").exists())

            claude_skills_status = subprocess.run(
                [
                    "git",
                    "status",
                    "--short",
                    "--untracked-files=all",
                    "--",
                    ".claude/skills",
                ],
                cwd=target_dir,
                capture_output=True,
                text=True,
            )
            self.assertEqual(claude_skills_status.returncode, 0)
            self.assertEqual(claude_skills_status.stdout.strip(), "?? .claude/skills")

            initial_branch = subprocess.run(
                ["git", "symbolic-ref", "--short", "HEAD"],
                cwd=target_dir,
                capture_output=True,
                text=True,
            )
            self.assertEqual(initial_branch.returncode, 0, msg=initial_branch.stderr)
            self.assertEqual(initial_branch.stdout.strip(), "main")

            for suffix in ("boot", "core", "mdm", "system"):
                self.assertTrue((target_dir / f"sample-service-{suffix}").is_dir())
                self.assertFalse((target_dir / f"admin-{suffix}").exists())

            boot_application = (
                target_dir
                / "sample-service-boot/src/main/java/com/acme/sample/boot/BootApplication.java"
            )
            self.assertTrue(boot_application.is_file())
            self.assertIn(
                "package com.acme.sample.boot;",
                boot_application.read_text(encoding="utf-8"),
            )

            root_pom_path = target_dir / "pom.xml"
            root_pom = root_pom_path.read_text(encoding="utf-8")
            self.assertIn("<groupId>com.acme.sample</groupId>", root_pom)
            self.assertIn("<artifactId>sample-service</artifactId>", root_pom)
            self.assertIn("<module>sample-service-boot</module>", root_pom)

            core_pom = (target_dir / "sample-service-core/pom.xml").read_text(
                encoding="utf-8"
            )
            self.assertIn("<artifactId>sample-service</artifactId>", core_pom)
            self.assertIn("<artifactId>sample-service-core</artifactId>", core_pom)

            application_yml = (
                target_dir / "sample-service-boot/src/main/resources/application.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("name: sample-service", application_yml)
            self.assertIn("type-enums-package: com.acme.sample", application_yml)

            application_dev_yml = (
                target_dir / "sample-service-boot/src/main/resources/application-dev.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("${SAMPLE_SERVICE_DATASOURCE_URL:", application_dev_yml)
            self.assertIn("jdbc:mysql://127.0.0.1:3307/sample_service", application_dev_yml)
            self.assertIn("${user.home}/.sample-service/uploads", application_dev_yml)

            application_test_yml = (
                target_dir / "sample-service-boot/src/main/resources/application-test.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("${SAMPLE_SERVICE_DATASOURCE_URL:", application_test_yml)
            self.assertIn("jdbc:mysql://127.0.0.1:3306/sample_service_test", application_test_yml)

            compose_yml = (target_dir / "compose.yaml").read_text(encoding="utf-8")
            self.assertIn("name: sample-service", compose_yml)
            self.assertIn("MYSQL_DATABASE: sample_service", compose_yml)

            readme = (target_dir / "README.md").read_text(encoding="utf-8")
            self.assertNotIn(str(self.repo_root), readme)
            self.assertIn(str(target_dir.resolve()), readme)
            self.assertIn("当前工作分支：`main`", readme)
            self.assertNotIn("feature/sso", readme)

            generated_skill = (
                target_dir / ".agents/skills/oig-java-development/SKILL.md"
            ).read_text(encoding="utf-8")
            self.assertIn("gateway-SSO conventions", generated_skill)
            self.assertNotIn("feature/sso", generated_skill)

            generated_openai_config = (
                target_dir
                / ".agents/skills/oig-java-development/agents/openai.yaml"
            ).read_text(encoding="utf-8")
            self.assertIn("implement a repository change", generated_openai_config)
            self.assertNotIn("feature/sso", generated_openai_config)

            template_markers = (
                "java-" + "admin-" + "starter",
                "java_" + "admin_" + "starter",
                "JAVA_" + "ADMIN_" + "STARTER",
                "basic_" + "platform_" + "sso",
                "com." + "example." + "admin",
                "com/" + "example/" + "admin",
                "Admin" + "BootApplication",
            )
            for path in target_dir.rglob("*"):
                if not path.is_file() or ".git" in path.parts:
                    continue
                try:
                    content = path.read_text(encoding="utf-8")
                except UnicodeDecodeError:
                    continue
                for marker in template_markers:
                    self.assertNotIn(marker, content, msg=f"模板标识残留于 {path}")

            head_check = subprocess.run(
                ["git", "rev-parse", "--verify", "HEAD"],
                cwd=target_dir,
                capture_output=True,
                text=True,
            )
            self.assertNotEqual(head_check.returncode, 0)

            root_pom_path.write_text(
                root_pom.replace(
                    "    </modules>",
                    "        <module>sample-service-orders</module>\n    </modules>",
                ),
                encoding="utf-8",
            )
            (target_dir / "sample-service-orders").mkdir()

            derived_dir = Path(temp_dir) / "derived-project"
            derived_result = self.run_initializer(
                target_dir,
                derived_dir,
                "derived-service",
                "org.example.derived",
            )
            self.assertEqual(derived_result.returncode, 0, msg=derived_result.stderr)
            self.assertTrue((derived_dir / "derived-service-orders").is_dir())
            self.assertTrue(
                (
                    derived_dir
                    / "derived-service-boot/src/main/java/org/example/derived/boot/BootApplication.java"
                ).is_file()
            )

    def test_rejects_non_empty_target_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "sample-service"
            target_dir.mkdir()
            keep_file = target_dir / "keep.txt"
            keep_file.write_text("keep", encoding="utf-8")

            result = self.run_initializer(
                self.repo_root,
                target_dir,
                "sample-service",
                "com.acme.sample",
            )

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("目标目录已存在且非空", result.stderr)
            self.assertEqual(keep_file.read_text(encoding="utf-8"), "keep")

    def test_rejects_java_keyword_in_package_name(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "sample-service"

            result = self.run_initializer(
                self.repo_root,
                target_dir,
                "sample-service",
                "com.class.sample",
            )

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("包名不合法", result.stderr)
            self.assertFalse(target_dir.exists())

    def test_rejects_project_name_that_is_not_lower_kebab_case(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "generated-project"

            result = self.run_initializer(
                self.repo_root,
                target_dir,
                "Sample Service",
                "com.acme.sample",
            )

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("项目名不合法", result.stderr)
            self.assertFalse(target_dir.exists())

    def test_requires_project_name_cli_argument(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "generated-project"

            result = subprocess.run(
                [
                    "python3",
                    "scripts/init_template_project.py",
                    "--project-path",
                    str(target_dir),
                    "--package",
                    "com.acme.sample",
                ],
                cwd=self.repo_root,
                capture_output=True,
                text=True,
            )

            self.assertNotEqual(result.returncode, 0)
            self.assertIn("--project-name", result.stderr)
            self.assertFalse(target_dir.exists())

    def test_requires_project_path_cli_argument(self) -> None:
        result = subprocess.run(
            [
                "python3",
                "scripts/init_template_project.py",
                "--project-name",
                "sample-service",
                "--package",
                "com.acme.sample",
            ],
            cwd=self.repo_root,
            capture_output=True,
            text=True,
        )

        self.assertNotEqual(result.returncode, 0)
        self.assertIn("--project-path", result.stderr)


if __name__ == "__main__":
    unittest.main()
