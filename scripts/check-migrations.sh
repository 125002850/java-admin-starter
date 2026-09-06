#!/usr/bin/env bash
set -euo pipefail

MIGRATION_DIR='boot/src/main/resources/db/migration'
JAVA_MIGRATION_DIR='boot/src/main/java/db/migration'
APPROVED_BASELINE_PATH="$MIGRATION_DIR/V20260805103327__squash_initial_schema.sql"
APPROVED_BASELINE_OLD_BLOB='b39166bf1209ba802c784aa4e722b4144f95f08f'
APPROVED_BASELINE_NEW_BLOB='aa2845e1365913945414328175e5d1758751770b'
mode=staged
base_ref=HEAD
head_ref=HEAD
base_supplied=false
head_supplied=false

usage() {
  printf '%s\n' 'Usage: check-migrations.sh [--all | --base <commit> [--head <commit>]]' >&2
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --all)
      [ "$mode" = staged ] && [ "$base_supplied" = false ] && [ "$head_supplied" = false ] || { usage; exit 2; }
      mode=all
      shift
      ;;
    --base|--head)
      [ "$#" -ge 2 ] && [ "$mode" != all ] || { usage; exit 2; }
      if [ "$1" = --base ]; then
        [ "$base_supplied" = false ] || { usage; exit 2; }
        base_ref="$2"
        base_supplied=true
      else
        [ "$head_supplied" = false ] || { usage; exit 2; }
        head_ref="$2"
        head_supplied=true
      fi
      mode=range
      shift 2
      ;;
    *) usage; exit 2 ;;
  esac
done

if [ "$mode" = range ] && [ "$base_supplied" = false ]; then
  usage
  exit 2
fi

repo_root="$(git rev-parse --show-toplevel)"
cd -- "$repo_root"
scratch="$(mktemp "${TMPDIR:-/tmp}/java-admin-starter-migration-check.XXXXXX")"
trap 'rm -f -- "$scratch"' EXIT
violations=0

report_error() {
  printf '%s\n' "$*" >&2
  violations=1
}

is_sql_migration() {
  [[ "$1" == "$MIGRATION_DIR/"* && "$1" == *.sql ]]
}

is_java_migration() {
  [[ "$1" == "$JAVA_MIGRATION_DIR/"* && "$1" == *.java ]]
}

migration_version() {
  local name="${1##*/}"
  name="${name#V}"
  printf '%s\n' "${name%%__*}"
}

is_timestamp_version() {
  printf '%s\n' "$1" | awk '
    {
      value = $0
      if (length(value) != 14 || value !~ /^[0-9]+$/) exit 1
      year = substr(value, 1, 4) + 0
      month = substr(value, 5, 2) + 0
      day = substr(value, 7, 2) + 0
      hour = substr(value, 9, 2) + 0
      minute = substr(value, 11, 2) + 0
      second = substr(value, 13, 2) + 0
      days[1]=31; days[2]=28; days[3]=31; days[4]=30; days[5]=31; days[6]=30
      days[7]=31; days[8]=31; days[9]=30; days[10]=31; days[11]=30; days[12]=31
      if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) days[2]=29
      if (year < 1 || month < 1 || month > 12 || day < 1 || day > days[month] ||
          hour > 23 || minute > 59 || second > 59) exit 1
    }
  '
}

is_valid_migration_name() {
  local name="${1#"$MIGRATION_DIR"/}"
  [[ "$name" =~ ^V[0-9]{14}__[^/[:space:]]+\.sql$ ]] && is_timestamp_version "$(migration_version "$1")"
}

contains_version() {
  [[ $'\n'"$2"$'\n' == *$'\n'"$1"$'\n'* ]]
}

is_approved_baseline_syntax_repair() {
  local status="$1" path="$2" old_blob new_blob
  [ "$status" = M ] && [ "$path" = "$APPROVED_BASELINE_PATH" ] || return 1
  old_blob="$(git rev-parse --verify --end-of-options "$base_commit:$path")" || return 1
  if [ "$mode" = range ]; then
    new_blob="$(git rev-parse --verify --end-of-options "$head_commit:$path")" || return 1
  else
    new_blob="$(git rev-parse --verify --end-of-options ":$path")" || return 1
  fi
  # One approved repair: remove seven illegal AFTER clauses from the CREATE TABLE baseline.
  [ "$old_blob" = "$APPROVED_BASELINE_OLD_BLOB" ] && [ "$new_blob" = "$APPROVED_BASELINE_NEW_BLOB" ]
}

check_git_entries() {
  local entry metadata entry_mode path
  while IFS= read -r -d '' entry; do
    metadata="${entry%%$'\t'*}"
    entry_mode="${metadata%% *}"
    path="${entry#*$'\t'}"
    case "$entry_mode" in
      100644|100755) ;;
      *) report_error "迁移资源必须是普通文件，禁止符号链接或子模块：$path (Git mode $entry_mode)" ;;
    esac
  done < "$scratch"
}

check_listing() {
  local path version seen=''
  while IFS= read -r -d '' path; do
    if [ "$mode" = all ] && [ -L "$path" ]; then
      report_error "迁移资源不能是工作区符号链接：$path"
    fi
    if is_java_migration "$path"; then
      report_error "禁止 Java Flyway migration：$path"
    elif is_sql_migration "$path"; then
      if ! is_valid_migration_name "$path"; then
        report_error "migration 命名不合法，须使用有效时间戳及非空描述 VyyyyMMddHHmmss__description.sql：$path"
        continue
      fi
      version="$(migration_version "$path")"
      if contains_version "$version" "$seen"; then
        report_error "migration 版本号重复：$path ($version)"
      fi
      seen+="$version"$'\n'
    fi
  done < "$scratch"
}

if [ "$mode" = all ]; then
  git ls-files --cached --stage -z -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
  check_git_entries
  git ls-files --cached --others --exclude-standard -z -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
  check_listing
  exit "$violations"
fi

if [ "$mode" = range ]; then
  base_commit="$(git rev-parse --verify --end-of-options "$base_ref^{commit}")"
  head_commit="$(git rev-parse --verify --end-of-options "$head_ref^{commit}")"
  git ls-tree -r -z "$head_commit" -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
  check_git_entries
  git ls-tree -r --name-only -z "$head_commit" -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
else
  if git rev-parse --verify HEAD >/dev/null 2>&1; then
    base_commit="$(git rev-parse HEAD)"
  else
    base_commit="$(git hash-object -w -t tree --stdin < /dev/null)"
  fi
  git ls-files --cached --stage -z -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
  check_git_entries
  git ls-files --cached -z -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
fi
check_listing

baseline_max=''
git ls-tree -r --name-only -z "$base_commit" -- "$MIGRATION_DIR" > "$scratch"
while IFS= read -r -d '' path; do
  if is_sql_migration "$path" && is_valid_migration_name "$path"; then
    version="$(migration_version "$path")"
    if [[ "$version" > "$baseline_max" ]]; then baseline_max="$version"; fi
  fi
done < "$scratch"

if [ "$mode" = range ]; then
  git diff --no-renames --name-status -z "$base_commit" "$head_commit" -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
else
  git diff --cached --no-renames --name-status -z -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" > "$scratch"
fi

while IFS= read -r -d '' status && IFS= read -r -d '' path; do
  if is_java_migration "$path"; then
    report_error "禁止新增、修改或删除 Java Flyway migration：$status $path"
  elif is_sql_migration "$path"; then
    if [ "$status" != A ]; then
      if ! is_approved_baseline_syntax_repair "$status" "$path"; then
        report_error "禁止修改、删除或重命名历史 migration（包括整体 squash）：$status $path"
      fi
    elif is_valid_migration_name "$path"; then
      version="$(migration_version "$path")"
      if [[ -n "$baseline_max" && ! "$version" > "$baseline_max" ]]; then
        report_error "新增 migration 版本必须大于历史最大版本 $baseline_max：$path"
      fi
    fi
  fi
done < "$scratch"

exit "$violations"
