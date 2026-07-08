#!/usr/bin/env sh

set -eu

MIGRATION_DIR="demo-boot/src/main/resources/db/migration"
JAVA_MIGRATION_DIR="demo-boot/src/main/java/db/migration"
LEGACY_JAVA_MIGRATION="$JAVA_MIGRATION_DIR/V11__fix_sys_user_cache_columns.java"

is_versioned_migration() {
  case "$1" in
    "$MIGRATION_DIR"/V*.sql) return 0 ;;
    *) return 1 ;;
  esac
}

is_valid_new_migration_name() {
  case "$1" in
    "$MIGRATION_DIR"/V*__*.sql) return 0 ;;
    *) return 1 ;;
  esac
}

is_java_migration() {
  case "$1" in
    "$JAVA_MIGRATION_DIR"/V*.java) return 0 ;;
    *) return 1 ;;
  esac
}

migration_version() {
  file_name="${1##*/}"
  version="${file_name#V}"
  version="${version%%__*}"
  printf '%s\n' "$version"
}

contains_line() {
  value="$1"
  lines="$2"
  case "
$lines" in
    *"
$value
"*) return 0 ;;
    *) return 1 ;;
  esac
}

check_tree() {
  JAVA_MIGRATION_VIOLATIONS=""
  ALL_JAVA_MIGRATIONS="$(git ls-files -- "$JAVA_MIGRATION_DIR" 2>/dev/null || true)"

  while IFS= read -r PATH; do
    [ -n "$PATH" ] || continue
    if is_java_migration "$PATH" && [ "$PATH" != "$LEGACY_JAVA_MIGRATION" ]; then
      JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}${PATH}\n"
    fi
  done <<EOF
$ALL_JAVA_MIGRATIONS
EOF

  if [ -n "$JAVA_MIGRATION_VIOLATIONS" ]; then
    printf '%b' "禁止新增 Java Flyway migration，请统一使用 $MIGRATION_DIR/V*__*.sql：\n$JAVA_MIGRATION_VIOLATIONS\n"
    printf '%s\n' "历史 Java migration $LEGACY_JAVA_MIGRATION 仅作为兼容保留，不得继续复制该模式。"
    exit 1
  fi
}

if [ "${1:-}" = "--all" ]; then
  check_tree
  exit 0
fi

STAGED_CHANGES="$(git diff --cached --name-status --find-renames -- "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" || true)"

if [ -z "$STAGED_CHANGES" ]; then
  exit 0
fi

VIOLATIONS=""
JAVA_MIGRATION_VIOLATIONS=""
TAB="$(printf '\t')"

while IFS="$TAB" read -r STATUS PATH1 PATH2; do
  [ -n "$STATUS" ] || continue

  case "$STATUS" in
    A)
      if is_versioned_migration "$PATH1"; then
        :
      elif is_java_migration "$PATH1"; then
        JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}${STATUS} ${PATH1}\n"
      fi
      ;;
    M|D|T|U)
      if is_versioned_migration "$PATH1"; then
        VIOLATIONS="${VIOLATIONS}${STATUS} ${PATH1}\n"
      elif is_java_migration "$PATH1"; then
        JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}${STATUS} ${PATH1}\n"
      fi
      ;;
    R*|C*)
      if is_versioned_migration "$PATH1" || is_versioned_migration "$PATH2"; then
        VIOLATIONS="${VIOLATIONS}${STATUS} ${PATH1}"
        if [ -n "${PATH2:-}" ]; then
          VIOLATIONS="${VIOLATIONS} -> ${PATH2}"
        fi
        VIOLATIONS="${VIOLATIONS}\n"
      elif is_java_migration "$PATH1" || is_java_migration "$PATH2"; then
        JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}${STATUS} ${PATH1}"
        if [ -n "${PATH2:-}" ]; then
          JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS} -> ${PATH2}"
        fi
        JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}\n"
      fi
      ;;
    *)
      if is_versioned_migration "$PATH1" || is_versioned_migration "$PATH2"; then
        VIOLATIONS="${VIOLATIONS}${STATUS} ${PATH1}"
        if [ -n "${PATH2:-}" ]; then
          VIOLATIONS="${VIOLATIONS} -> ${PATH2}"
        fi
        VIOLATIONS="${VIOLATIONS}\n"
      elif is_java_migration "$PATH1" || is_java_migration "$PATH2"; then
        JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}${STATUS} ${PATH1}"
        if [ -n "${PATH2:-}" ]; then
          JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS} -> ${PATH2}"
        fi
        JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}\n"
      fi
      ;;
  esac
done <<EOF
$STAGED_CHANGES
EOF

INVALID_NEW_MIGRATIONS=""
DUPLICATE_MIGRATION_VERSIONS=""
EXISTING_VERSIONS=""
NEW_VERSIONS=""

HEAD_MIGRATIONS="$(git ls-tree -r --name-only --full-tree HEAD -- "$MIGRATION_DIR" 2>/dev/null || true)"

while IFS= read -r PATH; do
  [ -n "$PATH" ] || continue
  if is_valid_new_migration_name "$PATH"; then
    EXISTING_VERSIONS="${EXISTING_VERSIONS}$(migration_version "$PATH")
"
  fi
done <<EOF
$HEAD_MIGRATIONS
EOF

while IFS="$TAB" read -r STATUS PATH1 PATH2; do
  [ -n "$STATUS" ] || continue

  case "$STATUS" in
    A)
      if is_versioned_migration "$PATH1"; then
        if ! is_valid_new_migration_name "$PATH1"; then
          INVALID_NEW_MIGRATIONS="${INVALID_NEW_MIGRATIONS}${PATH1}\n"
          continue
        fi

        version="$(migration_version "$PATH1")"

        if contains_line "$version" "$EXISTING_VERSIONS" || contains_line "$version" "$NEW_VERSIONS"; then
          DUPLICATE_MIGRATION_VERSIONS="${DUPLICATE_MIGRATION_VERSIONS}${PATH1} (version: ${version})\n"
          continue
        fi

        NEW_VERSIONS="${NEW_VERSIONS}${version}
"
      fi
      ;;
  esac
done <<EOF
$STAGED_CHANGES
EOF

if [ -n "$VIOLATIONS" ] || [ -n "$JAVA_MIGRATION_VIOLATIONS" ] || [ -n "$INVALID_NEW_MIGRATIONS" ] || [ -n "$DUPLICATE_MIGRATION_VERSIONS" ]; then
  if [ -n "$VIOLATIONS" ]; then
    printf '%b' "禁止修改、删除或重命名已存在的版本化迁移文件：\n$VIOLATIONS\n"
  fi

  if [ -n "$JAVA_MIGRATION_VIOLATIONS" ]; then
    printf '%b' "禁止新增、修改、删除或重命名 Java Flyway migration：\n$JAVA_MIGRATION_VIOLATIONS\n"
    printf '%s\n' "新增数据库迁移必须使用 $MIGRATION_DIR/V*__*.sql；历史 Java migration 仅作为兼容保留。"
  fi

  if [ -n "$INVALID_NEW_MIGRATIONS" ]; then
    printf '%b' "新增 migration 命名不合法：\n$INVALID_NEW_MIGRATIONS\n"
    printf '%s\n' "新增版本化迁移必须匹配 $MIGRATION_DIR/V*__*.sql，例如 V2__add_user_table.sql。"
  fi

  if [ -n "$DUPLICATE_MIGRATION_VERSIONS" ]; then
    printf '%b' "新增 migration 复用了已有版本号：\n$DUPLICATE_MIGRATION_VERSIONS\n"
    printf '%s\n' "新增版本化迁移必须使用新的唯一版本号，例如在已有 V1 后新增 V2__*.sql。"
  fi

  printf '%s\n' "如需调整历史迁移，请新增下一版本脚本，不要回改已有 V*.sql。"
  exit 1
fi
