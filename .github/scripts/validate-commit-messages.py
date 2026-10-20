#!/usr/bin/env python3
"""PR의 커밋 메시지가 AGENTS.md 형식을 따르는지 검증한다.

형식: <아이콘> <타입>: <한국어 요약>
  아이콘: ✨ feat, 🐛 fix, ♻️ refactor, ✅ test, 📝 docs, 🔧 chore
  예: ✨ feat: 게시판 생성 기능 추가

사용법:
    python3 .github/scripts/validate-commit-messages.py <base-sha> <head-sha>
"""

import re
import subprocess
import sys

# AGENTS.md 커밋 규칙에 정의된 아이콘 → 타입 매핑
# ♻️는 VS16(U+FE0F) 유무를 모두 허용하므로 비교 전 제거한다.
ICON_TO_TYPE = {
    "✨": "feat",
    "🐛": "fix",
    "♻": "refactor",
    "✅": "test",
    "📝": "docs",
    "🔧": "chore",
}

ICON_ALT = "|".join(re.escape(icon) for icon in ICON_TO_TYPE)
TYPE_ALT = "|".join(ICON_TO_TYPE.values())
PATTERN = re.compile(
    rf"^({ICON_ALT})[ \t]+({TYPE_ALT})[ \t]*:[ \t]+.+$",
    re.UNICODE,
)


def normalize(subject: str) -> str:
    """이모지 변형 부호(VS16 등)를 제거해 비교를 안정화한다."""
    return subject.replace("\ufe0f", "")


def main() -> None:
    if len(sys.argv) != 3:
        print("Usage: validate-commit-messages.py <base-sha> <head-sha>", file=sys.stderr)
        sys.exit(2)

    base_sha, head_sha = sys.argv[1], sys.argv[2]

    try:
        subjects = subprocess.check_output(
            ["git", "log", "--format=%s", f"{base_sha}..{head_sha}"],
            text=True,
        ).splitlines()
    except subprocess.CalledProcessError as exc:
        print(f"❌ 커밋 목록 조회 실패: {exc}", file=sys.stderr)
        sys.exit(1)

    failed = False
    for subject in subjects:
        subject = subject.strip()
        if not subject:
            continue
        if not PATTERN.match(normalize(subject)):
            print(f"❌ Invalid commit message: {subject}")
            print("   Format: <아이콘> <타입>: <한국어 요약> (예: ✨ feat: 게시판 생성 기능 추가)")
            failed = True

    if failed:
        sys.exit(1)

    print("✅ All commit messages follow the required format.")


if __name__ == "__main__":
    main()
