import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "testing/verify-flyway-mysql8.sh"
VERSIONS = ["20260901105646", "20261028171959"]
CONTAINER = "c" * 64


class FlywayMySqlVerificationTests(unittest.TestCase):
    def run_verify(self, failure="", ref=False, missing_history=False, mysql_image=None, docker_platform=None):
        with tempfile.TemporaryDirectory() as directory:
            work = Path(directory)
            repo = work / "repo"
            script = repo / "scripts/testing/verify-flyway-mysql8.sh"
            script.parent.mkdir(parents=True)
            shutil.copy2(SCRIPT, script)
            migrations = repo / "boot/src/main/resources/db/migration"
            migrations.mkdir(parents=True)
            for version in VERSIONS:
                (migrations / f"V{version}__fixture.sql").write_text("-- fixture\n")
            (repo / "target").mkdir()
            (repo / "target/keep.txt").write_text("existing build")
            mvnw = work / "mvn"
            mvnw.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
root = pathlib.Path.cwd()
record = {
    'cwd': str(root),
    'url': os.environ['JAVA_ADMIN_STARTER_DATASOURCE_URL'],
    'native_url': os.environ['SPRING_DATASOURCE_URL'],
    'flyway_url': os.environ['SPRING_FLYWAY_URL'],
    'external_config': os.environ.get('SPRING_CONFIG_LOCATION'),
    'reused_password': os.environ['JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD'] == 'do-not-use',
    'old_target_copied': (root / 'target/keep.txt').exists(),
    'docker_platform': os.environ.get('DOCKER_DEFAULT_PLATFORM'),
    'args': [a for a in sys.argv[1:] if 'password=' not in a],
}
pathlib.Path(os.environ['FLYWAY_SCRIPT_TEST_RECORD']).write_text(json.dumps(record))
sys.exit(7 if os.environ.get('FLYWAY_SCRIPT_TEST_FAILURE') == 'maven' else 0)
''')
            mvnw.chmod(0o755)
            if ref:
                subprocess.run(["git", "init", "-q", str(repo)], check=True)
                subprocess.run(["git", "-C", str(repo), "add", "boot"], check=True)
                subprocess.run(["git", "-C", str(repo), "-c", "user.name=Test", "-c",
                                "user.email=test@example.com", "-c", "commit.gpgsign=false",
                                "commit", "-qm", "fixture"], check=True)
                subprocess.run(["git", "-C", str(repo), "tag", "v1.4.0"], check=True)
                (migrations / "V20261029120000__uncommitted.sql").write_text("-- not in release\n")
            commands = work / "docker.jsonl"
            record = work / "maven.json"
            docker = work / "docker"
            docker.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
args = sys.argv[1:]
with pathlib.Path(os.environ['FLYWAY_SCRIPT_TEST_COMMANDS']).open('a') as output:
    output.write(json.dumps(args) + '\\n')
failure = os.environ.get('FLYWAY_SCRIPT_TEST_FAILURE')
container = 'c' * 64
if args[0] == 'run':
    pathlib.Path(args[args.index('--cidfile') + 1]).write_text(container)
    print(container)
    sys.exit(9 if failure == 'run' else 0)
if args[0] == 'port':
    print('127.0.0.1:49152')
elif args[0] == 'inspect':
    print('false')
elif args[0] == 'logs':
    print('temporary MySQL log')
elif args[0] == 'exec':
    if failure == 'startup':
        sys.exit(1)
    query = args[-1]
    if query == 'SELECT 1':
        print(1)
    elif query == 'SELECT VERSION()':
        print('8.0.24')
    elif 'COUNT(*)' in query:
        print(0)
    elif 'SELECT version' in query:
        print(os.environ['FLYWAY_SCRIPT_TEST_VERSIONS'])
''')
            docker.chmod(0o755)
            versions = VERSIONS[:1] if missing_history else VERSIONS
            env = dict(os.environ, PATH=f"{work}:{os.environ['PATH']}",
                       FLYWAY_SCRIPT_TEST_FAILURE=failure,
                       FLYWAY_SCRIPT_TEST_COMMANDS=str(commands), FLYWAY_SCRIPT_TEST_RECORD=str(record),
                       FLYWAY_SCRIPT_TEST_VERSIONS="\n".join(versions),
                       JAVA_ADMIN_STARTER_DATASOURCE_URL="jdbc:mysql://production.example/real",
                       JAVA_ADMIN_STARTER_DATASOURCE_PASSWORD="do-not-use",
                       SPRING_DATASOURCE_URL="jdbc:mysql://production.example/real",
                       SPRING_FLYWAY_URL="jdbc:mysql://production.example/real",
                       SPRING_CONFIG_LOCATION="file:/production/application.yml")
            for key, value in (("FLYWAY_VERIFY_MYSQL_IMAGE", mysql_image),
                               ("DOCKER_DEFAULT_PLATFORM", docker_platform)):
                env.pop(key, None)
                if value is not None:
                    env[key] = value
            result = subprocess.run(["bash", str(script), *(["--ref", "v1.4.0"] if ref else [])],
                                    env=env, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=20)
            calls = [json.loads(line) for line in commands.read_text().splitlines()]
            metadata = json.loads(record.read_text()) if record.exists() else None
            logs = next((repo / "target/flyway-mysql-verify").glob("run.*"))
            evidence = {path.name: path.read_text() for path in logs.iterdir() if path.is_file()}
            self.assertEqual((repo / "target/keep.txt").read_text(), "existing build")
            return result, calls, metadata, evidence

    def test_uses_new_loopback_container_and_overrides_external_database(self):
        result, calls, metadata, evidence = self.run_verify()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        run = next(call for call in calls if call[0] == "run")
        self.assertIn("mysql:8.0", run)
        self.assertIsNone(metadata["docker_platform"])
        self.assertEqual(run[run.index("--publish") + 1], "127.0.0.1::3306")
        self.assertNotIn("--volume", run)
        self.assertIn(["rm", "-fv", CONTAINER], calls)
        self.assertIn("127.0.0.1:49152/oig_flyway_verify?", metadata["url"])
        self.assertEqual(metadata["native_url"], metadata["url"])
        self.assertEqual(metadata["flyway_url"], metadata["url"])
        self.assertFalse(metadata["reused_password"])
        self.assertFalse(metadata["old_target_copied"])
        self.assertIsNone(metadata["external_config"])
        self.assertEqual(evidence["expected-versions.txt"], evidence["applied-versions.txt"])

    def test_image_and_platform_overrides_are_preserved(self):
        result, calls, metadata, evidence = self.run_verify(mysql_image="mysql:8.4", docker_platform="linux/arm64")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        run = next(call for call in calls if call[0] == "run")
        self.assertIn("mysql:8.4", run)
        self.assertEqual(metadata["docker_platform"], "linux/arm64")

    def test_git_ref_excludes_uncommitted_migrations(self):
        result, calls, metadata, evidence = self.run_verify(ref=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(evidence["expected-versions.txt"].splitlines(), VERSIONS)

    def test_failures_keep_logs_and_remove_only_the_owned_container(self):
        for failure, expected in (("run", 9), ("startup", 1), ("maven", 7)):
            with self.subTest(failure=failure):
                result, calls, metadata, evidence = self.run_verify(failure=failure)
                self.assertEqual(result.returncode, expected, result.stdout + result.stderr)
                self.assertEqual([call for call in calls if call[0] == "rm"], [["rm", "-fv", CONTAINER]])
                self.assertIn("mysql.log", evidence)
                if failure == "maven":
                    self.assertIn("maven.log", evidence)
                else:
                    self.assertIsNone(metadata)

    def test_zero_exit_from_maven_does_not_hide_missing_migrations(self):
        result, calls, metadata, evidence = self.run_verify(missing_history=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("实际迁移列表与源码不一致", result.stderr)
        self.assertTrue(evidence["versions.diff"])


if __name__ == "__main__":
    unittest.main()
