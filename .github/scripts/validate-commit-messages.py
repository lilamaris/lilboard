#!/usr/bin/env python3
"""PR의 커밋 메시지가 AGENTS.md 형식을 따르는지 검증한다.

형식: <아이콘> <타입>: <한국어 요약>
  아이콘: ✨ feat, 🐛 fix, ♻️ refactor, ✅ test, 📝 docs, 🔧 chore
  예: ✨ feat: 게시판 생성 기능 추가


머지 커밋는 기계가 생성한 머지 메시지(Merge branch/remote-tracking branch/pull request/tag) 또는
AGENTS.md 커밋 형식을 허용하고, 그 외 메시지는 실패 처리한다.
(머지 커밋 정책: docs/devops/adr/0002-commit-message-merge-policy.md)

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
CONVENTION_PATTERN = re.compile(
    rf"^({ICON_ALT})[ \t]+({TYPE_ALT})[ \t]*:[ \t]+.+$",
    re.UNICODE,
)

# git과 GitHub가 자동으로 생성하는 머지 커밋 메시지
# 예: Merge branch 'main' into feat/x, Merge remote-tracking branch 'origin/main',
#     Merge branch 'main' of https://github.com/owner/repo, Merge pull request #21 from owner:branch,
#     Merge tag 'v1.0' into main
MERGE_PATTERN = re.compile(
    r"^Merge (branch|remote-tracking branch|pull request|tag) .+$",
    re.UNICODE,
)

RECORD_SEPARATOR = "\x00"
# git 인자에는 문자열 %x00 이스케이프를 전달하고, 출력의 실제 NUL 문자로 분리한다.
FORMAT_ARGS = ["--format=%P" + "%x00" + "%s" + "%x00"]

CONVENTION_HINT = "Format: <아이콘> <타입>: <한국어 요약> (예: ✨ feat: 게시판 생성 기능 추가)"
MERGE_HINT = (
    "머지 커밋은 기계가 생성한 머지 메시지(Merge branch/remote-tracking branch/pull request/tag) "
    "또는 AGENTS.md 커밋 형식을 따라야 합니다"
)


def normalize(subject: str) -> str:
    """이모지 변형 부호(VS16 등)를 제거해 비교를 안정화한다."""
    return subject.replace("\ufe0f", "")


def is_merge_commit(parents: str) -> bool:
    """parent가 2개 이상이면 머지 커밋로 판정한다(메시지 형식과 무관)."""
    return len(parents.split()) >= 2


def check_subject(subject: str, parents: str) -> str | None:
    """커밋 메시지가 규칙을 위반하면 실패 이유를 반환하고, 통과하면 None을 반환한다."""
    subject = normalize(subject.strip())
    if not subject:
        return "커밋 메시지가 비어 있습니다"
    if is_merge_commit(parents):
        if MERGE_PATTERN.match(subject) or CONVENTION_PATTERN.match(subject):
            return None
        return MERGE_HINT
    if CONVENTION_PATTERN.match(subject):
        return None
    return CONVENTION_HINT


def run_git(args: list[str]) -> str:
    return subprocess.run(
        ["git"] + args,
        check=True,
        capture_output=True,
        text=True,
    ).stdout


def commit_exists(rev: str) -> bool:
    """커밋 객체가 체크아웃된 저장소에 존재하는지 확인한다."""
    return subprocess.run(
        ["git", "cat-file", "-e", f"{rev}^{{commit}}"],
        capture_output=True,
    ).returncode == 0


def read_commits(base_sha: str, head_sha: str) -> list[tuple[str, str]]:
    """범위의 (parents, subject)를 머지 커밋 포함해 읽는다."""
    output = run_git(["log"] + FORMAT_ARGS + [f"{base_sha}..{head_sha}"])
    records = output.split(RECORD_SEPARATOR)
    commits: list[tuple[str, str]] = []
    for index in range(0, len(records) - 1, 2):
        parents = records[index].strip()
        subject = records[index + 1]
        commits.append((parents, subject))
    return commits


def main() -> None:
    if len(sys.argv) != 3:
        print("Usage: validate-commit-messages.py <base-sha> <head-sha>", file=sys.stderr)
        sys.exit(2)

    base_sha, head_sha = sys.argv[1], sys.argv[2]

    for sha in (base_sha, head_sha):
        if not commit_exists(sha):
            print(f"❌ 커밋 객체를 찾을 수 없습니다: {sha}", file=sys.stderr)
            print("fetch-depth: 0 체크아웃과 base.sha/head.sha 전달을 확인하세요.", file=sys.stderr)
            sys.exit(2)

    try:
        commits = read_commits(base_sha, head_sha)
    except subprocess.CalledProcessError as exc:
        print(f"❌ 커밋 목록 조회 실패: {exc}", file=sys.stderr)
        sys.exit(2)

    failures: list[tuple[str, str]] = []
    merge_count = 0
    for parents, subject in commits:
        if is_merge_commit(parents):
            merge_count += 1
        reason = check_subject(subject, parents)
        if reason is not None:
            failures.append((subject, reason))

    for subject, reason in failures:
        print(f"❌ Invalid commit message: {subject}")
        print(f"   {reason}")

    if failures:
        sys.exit(1)

    validated = len(commits) - merge_count
    print(f"✅ {validated}개 커밋 메시지가 형식을 따릅니다.")
    if merge_count:
        print(f"⏭️ 머지 커밋 {merge_count}개는 머지 커밋 허용 정책으로 검증했습니다.")


if __name__ == "__main__":
    main()
