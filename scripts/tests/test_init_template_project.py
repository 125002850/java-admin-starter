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
            self.assertTrue((target_dir / ".git").is_dir())
            self.assertTrue((target_dir / "track-bench-boot").is_dir())
            self.assertTrue((target_dir / "track-bench-core").is_dir())
            self.assertTrue((target_dir / "track-bench-mdm").is_dir())
            self.assertTrue((target_dir / "track-bench-system").is_dir())

            boot_application = (
                target_dir
                / "track-bench-boot/src/main/java/com/trackbench/boot/BootApplication.java"
            )
            self.assertTrue(boot_application.is_file())
            self.assertIn("package com.trackbench.boot;", boot_application.read_text(encoding="utf-8"))

            root_pom = (target_dir / "pom.xml").read_text(encoding="utf-8")
            self.assertIn("<artifactId>track-bench</artifactId>", root_pom)
            self.assertIn("<module>track-bench-boot</module>", root_pom)

            application_yml = (
                target_dir / "track-bench-boot/src/main/resources/application.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("name: track-bench", application_yml)

            application_dev_yml = (
                target_dir / "track-bench-boot/src/main/resources/application-dev.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("jdbc:mysql://127.0.0.1:3307/track_bench", application_dev_yml)
            self.assertIn("${user.home}/.track-bench/uploads", application_dev_yml)

            readme = (target_dir / "README.md").read_text(encoding="utf-8")
            self.assertNotIn("java-admin-starter", readme)
            self.assertNotIn("com.example.admin", readme)

            head_check = subprocess.run(
                ["git", "rev-parse", "--verify", "HEAD"],
                cwd=target_dir,
                capture_output=True,
                text=True,
            )
            self.assertNotEqual(head_check.returncode, 0)


if __name__ == "__main__":
    unittest.main()
