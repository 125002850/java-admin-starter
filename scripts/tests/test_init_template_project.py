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
            self.assertTrue((target_dir / "track-bench-iam").is_dir())
            self.assertTrue((target_dir / "track-bench-system").is_dir())

            boot_application = (
                target_dir
                / "track-bench-boot/src/main/java/com/trackbench/boot/BootApplication.java"
            )
            self.assertTrue(boot_application.is_file())
            self.assertIn("package com.trackbench.boot;", boot_application.read_text(encoding="utf-8"))

            root_pom = (target_dir / "pom.xml").read_text(encoding="utf-8")
            self.assertIn("<groupId>com.trackbench</groupId>", root_pom)
            self.assertIn("<artifactId>track-bench</artifactId>", root_pom)
            self.assertIn("<module>track-bench-boot</module>", root_pom)

            application_yml = (
                target_dir / "track-bench-boot/src/main/resources/application.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("name: track-bench", application_yml)

            application_dev_yml = (
                target_dir / "track-bench-boot/src/main/resources/application-dev.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("jdbc:mysql://127.0.0.1:3300/track_bench", application_dev_yml)
            self.assertIn("${TRACK_BENCH_DATASOURCE_URL:", application_dev_yml)
            self.assertIn("${user.home}/.track-bench/uploads", application_dev_yml)
            self.assertNotIn("JAVA_ADMIN_STARTER", application_dev_yml)
            self.assertNotIn("java_admin_starter", application_dev_yml)

            readme = (target_dir / "README.md").read_text(encoding="utf-8")
            self.assertNotIn("java-admin-starter", readme)
            self.assertNotIn("com.example.admin", readme)
            self.assertNotIn("com/example/admin", readme)
            self.assertIn("track-bench-{biz}", readme)
            self.assertNotIn("admin-{biz}", readme)

            contract_test = (
                target_dir
                / "track-bench-boot/src/test/java/com/trackbench/boot/contract/ErrorCodeContractTests.java"
            ).read_text(encoding="utf-8")
            self.assertIn(
                "track-bench-core/src/main/java/com/trackbench/core/web/R.java",
                contract_test,
            )
            self.assertNotIn("com/example/admin", contract_test)

            second_target_dir = Path(temp_dir) / "inventory-service"
            second_result = subprocess.run(
                [
                    "python3",
                    "scripts/init_template_project.py",
                    "--target",
                    str(second_target_dir),
                    "--package",
                    "com.inventory",
                ],
                cwd=target_dir,
                capture_output=True,
                text=True,
            )
            self.assertEqual(second_result.returncode, 0, msg=second_result.stderr)
            self.assertTrue((second_target_dir / "inventory-service-boot").is_dir())
            second_boot_application = (
                second_target_dir
                / "inventory-service-boot/src/main/java/com/inventory/boot/BootApplication.java"
            )
            self.assertTrue(second_boot_application.is_file())
            self.assertIn(
                "package com.inventory.boot;",
                second_boot_application.read_text(encoding="utf-8"),
            )

            head_check = subprocess.run(
                ["git", "rev-parse", "--verify", "HEAD"],
                cwd=target_dir,
                capture_output=True,
                text=True,
            )
            self.assertNotEqual(head_check.returncode, 0)

    def test_project_name_containing_admin_is_not_replaced_twice(self) -> None:
        repo_root = Path(__file__).resolve().parents[2]

        with tempfile.TemporaryDirectory() as temp_dir:
            target_dir = Path(temp_dir) / "admin-service"
            result = subprocess.run(
                [
                    "python3",
                    "scripts/init_template_project.py",
                    "--target",
                    str(target_dir),
                    "--package",
                    "com.acme.admin",
                ],
                cwd=repo_root,
                capture_output=True,
                text=True,
            )

            self.assertEqual(result.returncode, 0, msg=result.stderr)
            self.assertTrue((target_dir / "admin-service-boot").is_dir())

            root_pom = (target_dir / "pom.xml").read_text(encoding="utf-8")
            self.assertIn("<artifactId>admin-service</artifactId>", root_pom)
            self.assertNotIn("admin-service-service", root_pom)

            application_yml = (
                target_dir / "admin-service-boot/src/main/resources/application.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("name: admin-service", application_yml)
            self.assertNotIn("name: admin-service-service", application_yml)

            application_dev_yml = (
                target_dir / "admin-service-boot/src/main/resources/application-dev.yml"
            ).read_text(encoding="utf-8")
            self.assertIn("jdbc:mysql://127.0.0.1:3300/admin_service_iam", application_dev_yml)
            self.assertNotIn("admin_service_service", application_dev_yml)


if __name__ == "__main__":
    unittest.main()
