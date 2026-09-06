#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
check_script="$(cd -- "$script_dir/.." && pwd -P)/check-migrations.sh"
repository_root="$(cd -- "$script_dir/../.." && pwd -P)"
temp_parent="$(cd -- "${TMPDIR:-/tmp}" && pwd -P)"
test_dir="$(mktemp -d "$temp_parent/java-admin-starter-migration-tests.XXXXXX")"
test_dir="$(cd -- "$test_dir" && pwd -P)"
cleanup() {
  cd -- "$temp_parent"
  case "$test_dir" in
    "$temp_parent"/java-admin-starter-migration-tests.*) rm -rf -- "$test_dir" ;;
    *) printf 'Unsafe temporary path: %s\n' "$test_dir" >&2; return 1 ;;
  esac
}
trap cleanup EXIT

migration_dir='boot/src/main/resources/db/migration'
baseline="$migration_dir/V20260805103327__initial.sql"
new_migration="$migration_dir/V20260805103328__next.sql"
approved_baseline="$migration_dir/V20260805103327__squash_initial_schema.sql"
cases=0
failures=0
repositories=0

new_repo() {
  repositories=$((repositories + 1))
  mkdir -p "$test_dir/$repositories"
  cd -- "$test_dir/$repositories"
  git init -q
  git config user.name 'Migration Check Test'
  git config user.email 'migration-check@example.invalid'
  git config core.autocrlf false
  git config core.symlinks false
  mkdir -p "$migration_dir"
  printf 'select 1;\n' > "$baseline"
  git add .
  git commit -qm 'initial fixture'
  initial_commit="$(git rev-parse HEAD)"
}

expect_pass() {
  cases=$((cases + 1))
  local label="$1"
  shift
  if bash "$check_script" "$@" > "$test_dir/output.log" 2>&1; then
    printf 'PASS %s\n' "$label"
  else
    printf 'FAIL %s: expected acceptance\n' "$label"
    cat "$test_dir/output.log"
    failures=$((failures + 1))
  fi
}

expect_fail() {
  cases=$((cases + 1))
  local label="$1"
  shift
  if bash "$check_script" "$@" > "$test_dir/output.log" 2>&1; then
    printf 'FAIL %s: expected rejection\n' "$label"
    failures=$((failures + 1))
  else
    printf 'PASS %s\n' "$label"
  fi
}

stage_symlink() {
  local path="$1" target="$2" blob
  printf '%s' "$target" > "$path"
  blob="$(printf '%s' "$target" | git hash-object -w --stdin)"
  git update-index --add --cacheinfo 120000 "$blob" "$path"
}

merge_pr_with_advanced_target() {
  local branch_version="$1"
  git checkout -qb feature
  if [ -n "$branch_version" ]; then
    printf 'select 3;\n' > "$migration_dir/V${branch_version}__branch.sql"
  else
    printf 'documentation only\n' > notes.md
  fi
  git add .
  git commit -qm 'feature change'
  git checkout -qb target "$initial_commit"
  printf 'select 2;\n' > "$migration_dir/V20260805103330__target.sql"
  git add .
  git commit -qm 'target branch migration'
  target_commit="$(git rev-parse HEAD)"
  git checkout -qb pr-merge
  git merge -q --no-ff -m 'PR merge fixture' feature
}

prepare_repair_fixtures() {
  repaired_fixture="$test_dir/repaired-baseline.sql"
  historical_fixture="$test_dir/historical-baseline.sql"
  sed 's/\r$//' "$repository_root/$approved_baseline" > "$repaired_fixture"
  awk '
    /^create table / { table = $3 }
    /^\);/ { table = "" }
    {
      after_column = ""
      if (table == "sys_dict_type_global") {
        if ($1 == "remark") after_column = "dict_type_name"
        if ($1 == "status") after_column = "remark"
        if ($1 == "version") after_column = "status"
      }
      if (table == "sys_dict_item_global") {
        if ($1 == "remark") after_column = "dict_item_name"
        if ($1 == "status") after_column = "remark"
        if ($1 == "sort_order") after_column = "status"
        if ($1 == "version") after_column = "sort_order"
      }
      if (after_column != "") restored += sub(/,$/, " after " after_column ",")
      print
    }
    END {
      if (restored != 7) {
        printf "Expected seven historical AFTER clauses, got %d\n", restored > "/dev/stderr"
        exit 1
      }
    }
  ' "$repaired_fixture" > "$historical_fixture"
  for entry in \
    "aa2845e1365913945414328175e5d1758751770b:$repaired_fixture" \
    "b39166bf1209ba802c784aa4e722b4144f95f08f:$historical_fixture"; do
    expected_blob="${entry%%:*}"
    fixture_path="${entry#*:}"
    actual_blob="$(git hash-object --no-filters "$fixture_path")"
    if [ "$actual_blob" != "$expected_blob" ]; then
      printf 'Unexpected repair fixture blob: expected %s, got %s\n' "$expected_blob" "$actual_blob" >&2
      exit 1
    fi
  done
}

new_repair_repo() {
  new_repo
  git rm -q "$baseline"
  mkdir -p "$migration_dir"
  cp "$historical_fixture" "$approved_baseline"
  git add .
  git commit -qm 'known historical baseline fixture'
  initial_commit="$(git rev-parse HEAD)"
}

new_repo
expect_pass 'unchanged staged tree'

new_repo
printf 'select 2;\n' > "$new_migration"
git add .
expect_pass 'newer timestamp migration'

new_repo
printf 'select 2;\n' > "$migration_dir/V20260805103327__duplicate.sql"
git add .
expect_fail 'duplicate existing version'

new_repo
printf 'select 2;\n' > "$migration_dir/V20260804103327__backdated.sql"
git add .
expect_fail 'version older than baseline'

new_repo
printf 'select 2;\n' > "$migration_dir/V20260230103327__invalid_date.sql"
git add .
expect_fail 'invalid timestamp calendar date'

new_repo
printf 'select 2;\n' > "$migration_dir/V20260805103328__.sql"
git add .
expect_fail 'missing migration description'

new_repo
printf 'select 2;\n' > "$baseline"
git add .
expect_fail 'modified historical SQL'

new_repo
git rm -q "$baseline"
expect_fail 'deleted historical SQL'

new_repo
git mv "$baseline" "$migration_dir/V20260805103327__renamed.sql"
expect_fail 'renamed historical SQL'

new_repo
git rm -q "$baseline"
mkdir -p "$migration_dir"
printf 'select 2;\n' > "$new_migration"
git add .
expect_fail 'complete history replacement cannot bypass protection'

new_repo
mkdir -p boot/src/main/java/db/migration
printf 'class V20260805103328__JavaMigration {}\n' > boot/src/main/java/db/migration/V20260805103328__JavaMigration.java
git add .
expect_fail 'Java migration'

new_repo
printf 'select 2;\n' > "$baseline"
git add .
git commit -qm 'modify baseline in committed diff'
expect_fail 'committed CI range protects history' --base "$initial_commit" --head HEAD

new_repo
printf 'select 2;\n' > "$new_migration"
git add .
git commit -qm 'append migration in committed diff'
expect_pass 'committed CI range accepts append' --base "$initial_commit" --head HEAD

new_repo
expect_fail 'invalid CI baseline fails closed' --base does-not-exist --head HEAD

new_repo
printf 'select 2;\n' > "$migration_dir/V1__invalid.sql"
git add .
expect_fail 'full tree checks timestamp names' --all

new_repo
printf 'select 2;\n' > "$migration_dir/V20260805103327__duplicate.sql"
git add .
expect_fail 'full tree checks duplicate versions' --all

new_repo
printf 'select 2;\n' > "$new_migration"
git add .
expect_pass 'full tree accepts valid migrations' --all

new_repo
expect_fail 'unknown command arguments fail closed' --unknown

new_repo
mkdir -p boot/src/main/java/db/migration/nested
printf 'class V20260805103328__Nested {}\n' > boot/src/main/java/db/migration/nested/V20260805103328__Nested.java
git add .
expect_fail 'nested Java migration in staged tree'
expect_fail 'nested Java migration in full tree' --all
git commit -qm 'nested Java migration fixture'
expect_fail 'nested Java migration in committed range' --base "$initial_commit"

new_repo
mkdir -p boot/src/main/java/db/migration
printf 'class CustomMigration {}\n' > boot/src/main/java/db/migration/CustomMigration.java
git add .
expect_fail 'Java migration location cannot bypass protection with another class name'

new_repo
mkdir -p "$migration_dir/nested"
printf 'select 2;\n' > "$migration_dir/nested/V20260805103328__nested.sql"
git add .
expect_fail 'nested versioned SQL cannot bypass root naming rules'

new_repo
printf 'select 2;\n' > boot/src/main/resources/shared.sql
git add boot/src/main/resources/shared.sql
stage_symlink "$new_migration" '../../shared.sql'
expect_fail 'migration symlink in staged Git metadata'
expect_fail 'migration symlink in full-tree Git metadata' --all
git commit -qm 'historical symlink fixture'
symlink_commit="$(git rev-parse HEAD)"
printf 'select 3;\n' > boot/src/main/resources/shared.sql
git add boot/src/main/resources/shared.sql
git commit -qm 'change historical symlink target outside migration directory'
expect_fail 'historical symlink target cannot change outside protected diff' --base "$symlink_commit"

new_repo
stage_symlink "$migration_dir/nested" '../external-migrations'
expect_fail 'migration location cannot contain a linked directory'

new_repo
merge_pr_with_advanced_target ''
expect_pass 'PR merge retains migrations appended to target after branch divergence' --base "$target_commit"

new_repo
merge_pr_with_advanced_target '20260805103331'
expect_pass 'PR merge accepts migration newer than latest target version' --base "$target_commit"

new_repo
merge_pr_with_advanced_target '20260805103328'
expect_fail 'PR merge rejects migration older than latest target version' --base "$target_commit"

new_repo
merge_pr_with_advanced_target '20260805103330'
expect_fail 'PR merge rejects version duplicated on latest target branch' --base "$target_commit"

new_repo
repeatable_migration="$migration_dir/R__refresh_view.sql"
printf 'select 2;\n' > "$repeatable_migration"
git add .
expect_fail 'repeatable SQL cannot bypass staged timestamp naming rules'
expect_fail 'repeatable SQL cannot bypass full-tree timestamp naming rules' --all
git commit -qm 'repeatable migration fixture'
repeatable_commit="$(git rev-parse HEAD)"
printf 'select 3;\n' > "$repeatable_migration"
git add .
git commit -qm 'modify historical repeatable migration'
expect_fail 'committed repeatable SQL changes cannot bypass append-only history' --base "$repeatable_commit"

new_repo
printf 'select 2;\n' > "$migration_dir/beforeMigrate.sql"
git add .
expect_fail 'SQL callback cannot bypass versioned migration naming rules'

new_repo
printf 'Migration documentation\n' > "$migration_dir/README.md"
git add .
expect_pass 'non-SQL migration documentation remains allowed'

prepare_repair_fixtures

new_repair_repo
cp "$repaired_fixture" "$approved_baseline"
git add .
expect_pass 'approved seven-clause baseline syntax repair in staged tree'
git commit -qm 'approved baseline syntax repair'
expect_pass 'approved seven-clause baseline syntax repair in committed range' --base "$initial_commit"

new_repair_repo
cp "$repaired_fixture" "$approved_baseline"
printf 'select 99;\n' >> "$approved_baseline"
git add .
expect_fail 'approved repair mixed with another SQL edit in staged tree'
git commit -qm 'unapproved mixed baseline edits'
expect_fail 'approved repair mixed with another SQL edit in committed range' --base "$initial_commit"

new_repair_repo
cp "$repaired_fixture" "$approved_baseline"
git add .
git commit -qm 'approved baseline repair fixture'
repaired_commit="$(git rev-parse HEAD)"
printf 'select 99;\n' >> "$approved_baseline"
git add .
expect_fail 'further changes after approved repair in staged tree'
git commit -qm 'modify repaired baseline'
expect_fail 'further changes after approved repair in committed range' --base "$repaired_commit"

new_repair_repo
printf -- '-- unexpected original content\n' >> "$approved_baseline"
git add .
git commit -qm 'different historical blob fixture'
unexpected_commit="$(git rev-parse HEAD)"
cp "$repaired_fixture" "$approved_baseline"
git add .
expect_fail 'approved new blob with unmatched original blob in staged tree'
git commit -qm 'repair from unapproved historical content'
expect_fail 'approved new blob with unmatched original blob in committed range' --base "$unexpected_commit"

new_repair_repo
printf 'select 2;\n' > "$new_migration"
git add .
git commit -qm 'additional historical migration fixture'
two_migrations_commit="$(git rev-parse HEAD)"
cp "$repaired_fixture" "$approved_baseline"
printf 'select 3;\n' > "$new_migration"
git add .
expect_fail 'approved repair cannot allow another historical migration edit in staged tree'
git commit -qm 'repair plus another historical edit'
expect_fail 'approved repair cannot allow another historical migration edit in committed range' --base "$two_migrations_commit"

new_repair_repo
git rm -q "$approved_baseline"
expect_fail 'approved baseline path cannot be deleted in staged tree'
git commit -qm 'delete approved baseline path'
expect_fail 'approved baseline path cannot be deleted in committed range' --base "$initial_commit"

new_repair_repo
different_baseline="$migration_dir/V20260805103327__another_baseline.sql"
git mv "$approved_baseline" "$different_baseline"
git commit -qm 'different baseline path fixture'
different_path_commit="$(git rev-parse HEAD)"
cp "$repaired_fixture" "$different_baseline"
git add .
expect_fail 'approved blobs at a different path in staged tree'
git commit -qm 'repair unapproved baseline path'
expect_fail 'approved blobs at a different path in committed range' --base "$different_path_commit"

printf '%s cases, %s failures\n' "$cases" "$failures"
test "$failures" -eq 0
