#!/usr/bin/env sh

set -eu

MIGRATION_DIR="boot/src/main/resources/db/migration"
JAVA_MIGRATION_DIR="boot/src/main/java/db/migration"

is_versioned_migration() {
  case "$1" in
    "$MIGRATION_DIR"/V*.sql) return 0 ;;
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

is_timestamp_version() {
  printf '%s\n' "$1" | awk '
    {
      value = $0
      if (length(value) != 14 || value !~ /^[0-9]+$/) {
        exit 1
      }

      year = substr(value, 1, 4) + 0
      month = substr(value, 5, 2) + 0
      day = substr(value, 7, 2) + 0
      hour = substr(value, 9, 2) + 0
      minute = substr(value, 11, 2) + 0
      second = substr(value, 13, 2) + 0

      days[1] = 31
      days[2] = 28
      days[3] = 31
      days[4] = 30
      days[5] = 31
      days[6] = 30
      days[7] = 31
      days[8] = 31
      days[9] = 30
      days[10] = 31
      days[11] = 30
      days[12] = 31

      if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) {
        days[2] = 29
      }

      if (year < 1 || month < 1 || month > 12 || day < 1 || day > days[month] ||
          hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
        exit 1
      }
    }
  '
}

is_valid_new_migration_name() {
  case "$1" in
    "$MIGRATION_DIR"/V*__*.sql) ;;
    *) return 1 ;;
  esac

  is_timestamp_version "$(migration_version "$1")"
}

contains_line() {
  value="$1"
  lines="$2"
  case "
$lines
" in
    *"
$value
"*) return 0 ;;
    *) return 1 ;;
  esac
}

check_tree() {
  JAVA_MIGRATION_VIOLATIONS=""
  ALL_JAVA_MIGRATIONS="$(git ls-files -- "$JAVA_MIGRATION_DIR" 2>/dev/null || true)"

  while IFS= read -r MIGRATION_PATH; do
    [ -n "$MIGRATION_PATH" ] || continue
    if is_java_migration "$MIGRATION_PATH"; then
      JAVA_MIGRATION_VIOLATIONS="${JAVA_MIGRATION_VIOLATIONS}${MIGRATION_PATH}\n"
    fi
  done <<EOF
$ALL_JAVA_MIGRATIONS
EOF

  if [ -n "$JAVA_MIGRATION_VIOLATIONS" ]; then
    printf '%b' "禁止新增 Java Flyway migration，请统一使用 $MIGRATION_DIR/VyyyyMMddHHmmss__*.sql：\n$JAVA_MIGRATION_VIOLATIONS\n"
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

# 一次性基线合并（squash）：本次提交删除全部历史版本化迁移、且仅新增一个
# 时间戳迁移时，放行删除操作；其余修改/重命名/部分删除仍被拦截。
SQUASH_ALLOWED=0
HEAD_VERSIONED=""
STAGED_DELETED=""
STAGED_ADDED=""

while IFS= read -r MIGRATION_PATH; do
  [ -n "$MIGRATION_PATH" ] || continue
  if is_versioned_migration "$MIGRATION_PATH"; then
    HEAD_VERSIONED="${HEAD_VERSIONED}${MIGRATION_PATH}\n"
  fi
done <<EOF
$(git ls-tree -r --name-only --full-tree HEAD -- "$MIGRATION_DIR" 2>/dev/null || true)
EOF

while IFS="$TAB" read -r STATUS PATH1 PATH2; do
  [ -n "$STATUS" ] || continue
  case "$STATUS" in
    D)
      if is_versioned_migration "$PATH1"; then
        STAGED_DELETED="${STAGED_DELETED}${PATH1}\n"
      fi
      ;;
    A)
      if is_versioned_migration "$PATH1"; then
        STAGED_ADDED="${STAGED_ADDED}${PATH1}\n"
      fi
      ;;
  esac
done <<EOF
$STAGED_CHANGES
EOF

if [ -n "$HEAD_VERSIONED" ] \
    && [ -n "$STAGED_DELETED" ] \
    && [ "$(printf '%b' "$STAGED_DELETED" | sort)" = "$(printf '%b' "$HEAD_VERSIONED" | sort)" ]; then
  ADDED_COUNT="$(printf '%b' "$STAGED_ADDED" | sed '/^$/d' | wc -l | tr -d ' ')"
  if [ "$ADDED_COUNT" -eq 1 ]; then
    SQUASH_ALLOWED=1
  fi
fi

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
        if [ "$STATUS" = "D" ] && [ "$SQUASH_ALLOWED" = 1 ]; then
          :
        else
          VIOLATIONS="${VIOLATIONS}${STATUS} ${PATH1}\n"
        fi
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

while IFS= read -r MIGRATION_PATH; do
  [ -n "$MIGRATION_PATH" ] || continue
  if is_valid_new_migration_name "$MIGRATION_PATH"; then
    EXISTING_VERSIONS="${EXISTING_VERSIONS}$(migration_version "$MIGRATION_PATH")
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
    printf '%s\n' "新增数据库迁移必须使用 $MIGRATION_DIR/VyyyyMMddHHmmss__*.sql。"
  fi

  if [ -n "$INVALID_NEW_MIGRATIONS" ]; then
    printf '%b' "新增 migration 命名不合法：\n$INVALID_NEW_MIGRATIONS\n"
    printf '%s\n' "新增版本化迁移必须使用有效的 14 位时间戳，例如 V20260713163027__add_user_table.sql；禁止继续使用 V9、V10 等自增版本号。"
  fi

  if [ -n "$DUPLICATE_MIGRATION_VERSIONS" ]; then
    printf '%b' "新增 migration 复用了已有版本号：\n$DUPLICATE_MIGRATION_VERSIONS\n"
    printf '%s\n' "新增版本化迁移必须使用新的唯一时间戳，例如 V20260713163027__add_user_table.sql。"
  fi

  printf '%s\n' "如需调整历史迁移，请新增时间戳版本脚本，不要回改已有 V*.sql。"
  exit 1
fi
