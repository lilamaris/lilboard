"""`.github/scripts/validate-commit-messages.py`의 커밋 메시지 규칙과 머지 커밋 정책을 검증한다.

실행: python3 -m unittest discover -s .github/scripts/tests
"""

import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT_PATH = Path(__file__).resolve().parent.parent / "validate-commit-messages.py"
SPECS = "\ufe0f"


def load_validator():
    """하이픈이 포함된 스크립트 파일을 모듈로 로드한다."""
    spec = importlib.util.spec_from_file_location("validate_commit_messages", SCRIPT_PATH)
    if spec is None:
        raise RuntimeError(f"모듈 스펙을 만들 수 없습니다: {SCRIPT_PATH}")
    loader = spec.loader
    if loader is None:
        raise RuntimeError(f"모듈 로더가 없습니다: {SCRIPT_PATH}")
    module = importlib.util.module_from_spec(spec)
    loader.exec_module(module)
    return module


validator = load_validator()


class CommitMessageFormatTest(unittest.TestCase):
    """AGENTS.md 커밋 형식(`<아이콘> <타입>: <요약>`) 검증을 검증한다."""

    def test_conventional_commit_passes(self):
        """아이콘과 타입이 짝을 이루는 커밋 메시지는 통과한다."""
        self.assertIsNone(validator.check_subject("✨ feat: 게시판 생성 기능 추가", "abc123"))

    def test_all_six_types_pass(self):
        """AGENTS.md의 6개 타입이 모두 통과한다."""
        messages = {
            "✨ feat: 게시글 생성",
            "🐛 fix: 삭제된 게시판 차단",
            "♻ refactor: 예외 처리 통합",
            "✅ test: 통합 테스트 추가",
            "📝 docs: 체크리스트 갱신",
            "🔧 chore: Dependabot bump",
        }
        for message in messages:
            with self.subTest(message=message):
                self.assertIsNone(validator.check_subject(message, "abc123"))

    def test_refactor_with_vs16_passes(self):
        """♻️의 VS16(U+FE0F) 변형도 통과한다."""
        self.assertIsNone(validator.check_subject(f"♻{SPECS} refactor: 예외 처리 통합", "abc123"))

    def test_missing_icon_fails(self):
        """아이콘이 없는 메시지는 실패한다."""
        self.assertIsNotNone(validator.check_subject("feat: 게시판 생성", "abc123"))

    def test_icon_type_pairing_is_not_enforced(self):
        """현재 규칙은 아이콘과 타입의 짝을 검사하지 않는다(별도 정책 결정로 추적)."""
        self.assertIsNone(validator.check_subject("✨ fix: 게시판 생성", "abc123"))

    def test_empty_summary_fails(self):
        """요약이 없으면 실패한다."""
        self.assertIsNotNone(validator.check_subject("✨ feat:", "abc123"))

    def test_empty_subject_fails(self):
        """커밋 메시지가 비어 있으면 실패한다."""
        self.assertIsNotNone(validator.check_subject("   ", "abc123"))


class MergeCommitDetectionTest(unittest.TestCase):
    """머지 커밋 판정(parent 개수)을 검증한다."""

    def test_root_commit_is_not_merge(self):
        """parent가 없는 루트 커밋은 머지 커밋가 아니다."""
        self.assertFalse(validator.is_merge_commit(""))

    def test_single_parent_is_not_merge(self):
        """parent가 1개인 커밋은 머지 커밋가 아니다."""
        self.assertFalse(validator.is_merge_commit("abc123"))

    def test_two_parents_is_merge(self):
        """parent가 2개면 머지 커밋로 판정한다."""
        self.assertTrue(validator.is_merge_commit("abc123 def456"))

    def test_octopus_merge_is_merge(self):
        """parent가 3개(옥토버스)도 머지 커밋로 판정한다."""
        self.assertTrue(validator.is_merge_commit("a b c"))


class MergeCommitPolicyTest(unittest.TestCase):
    """머지 커밋 허용 정책(정책 B)을 검증한다."""

    def test_machine_merge_messages_pass(self):
        """git과 GitHub가 생성한 머지 메시지는 통과한다."""
        subjects = [
            "Merge branch 'main' into dependabot/gradle-minor-patch",
            "Merge branch 'main' into feat/board-crud",
            "Merge remote-tracking branch 'origin/main'",
            "Merge remote-tracking branch 'origin/main' into feat/board-crud",
            "Merge branch 'main' of https://github.com/owner/repo",
            "Merge pull request #21 from lilamaris/feat/global-controller-advice",
            "Merge tag 'v1.0' into main",
        ]
        for subject in subjects:
            with self.subTest(subject=subject):
                self.assertIsNone(validator.check_subject(subject, "abc123 def456"))

    def test_conventional_merge_message_passes(self):
        """사람이 작성한 AGENTS.md 형식 머지 메시지도 통과한다."""
        self.assertIsNone(
            validator.check_subject("🔧 chore: 커밋 메시지 검증이 추가된 main 반영", "abc123 def456")
        )

    def test_spoofed_merge_subject_fails(self):
        """머지처럼 보이도록 만든 머지 커밋 메시지는 실패한다."""
        self.assertIsNotNone(validator.check_subject("Merge branch: main 반영", "abc123 def456"))

    def test_arbitrary_subject_on_merge_commit_fails(self):
        """머지 커밋에 머지 형식과 커밋 형식 모두 아닌 메시지는 실패한다."""
        self.assertIsNotNone(validator.check_subject("sneaky merge", "abc123 def456"))

    def test_machine_merge_message_on_normal_commit_fails(self):
        """parent가 1개인 커밋는 머지 형식 메시지로 통과하지 못한다."""
        self.assertIsNotNone(validator.check_subject("Merge branch 'main'", "abc123"))

    def test_arbitrary_subject_on_normal_commit_fails(self):
        """일반 커밋의 임의 메시지는 실패한다."""
        self.assertIsNotNone(validator.check_subject("sneaky commit", "abc123"))


class GitRangeIntegrationTest(unittest.TestCase):
    """임시 git 저장소로 스크립트 전체 실행을 검증한다.

    버그 회귀: PR 브랜치가 main을 머지하면 `Merge branch 'main' into ...` 커밋가 생성되어
    기존 검증은 실패했다. 머지 커밋 허용 정책 이후에는 통과해야 한다.
    """

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self._tmp.name)
        self.git("init", "-q", "-b", "main")
        self.git("config", "user.email", "test@example.com")
        self.git("config", "user.name", "Test")
        self.write_file("base.txt", "base")
        self.git("add", ".")
        self.git("commit", "-q", "-m", "✨ feat: 초기 커밋 추가")

    def tearDown(self):
        self._tmp.cleanup()

    def git(self, *args):
        return subprocess.run(
            ["git", "-C", str(self.repo)] + list(args),
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()

    def write_file(self, name, content):
        (self.repo / name).write_text(content, encoding="utf-8")

    def commit(self, message):
        self.git("add", ".")
        self.git("commit", "-q", "-m", message)
        return self.git("rev-parse", "HEAD")

    def run_script(self, base, head):
        return subprocess.run(
            [sys.executable, str(SCRIPT_PATH), base, head],
            cwd=self.repo,
            capture_output=True,
            text=True,
        )

    def merge_main_into_feature(self, message=None):
        """main에 커밋를 추가하고 feat/x 브랜치로 머지해 머지 커밋를 만든다."""
        self.git("checkout", "-q", "-b", "feat/x")
        self.write_file("feature.txt", "feature")
        self.commit("✨ feat: 기능 추가")
        self.git("checkout", "-q", "main")
        self.write_file("main.txt", "main")
        main_head = self.commit("🐛 fix: main 수정")
        self.git("checkout", "-q", "feat/x")
        args = ["merge", "-q", "--no-ff", "main"]
        if message is not None:
            args += ["-m", message]
        self.git(*args)
        return main_head, self.git("rev-parse", "HEAD")

    def test_merge_commit_from_main_is_allowed(self):
        """main을 머지한 PR 브랜치(자동 머지 커밋 포함)는 통과한다."""
        main_head, merge_head = self.merge_main_into_feature()
        result = self.run_script(main_head, merge_head)
        self.assertEqual(0, result.returncode, msg=result.stdout + result.stderr)
        self.assertIn("머지 커밋 1개", result.stdout)

    def test_spoofed_merge_commit_fails(self):
        """임의 메시지의 2-parent 머지 커밋는 실패한다."""
        main_head, merge_head = self.merge_main_into_feature("sneaky merge")
        result = self.run_script(main_head, merge_head)
        self.assertEqual(1, result.returncode)
        self.assertIn("sneaky merge", result.stdout)

    def test_non_conforming_commit_fails(self):
        """PR 브랜치의 비준수 커밋는 실패한다."""
        self.git("checkout", "-q", "-b", "feat/y")
        self.write_file("feature.txt", "feature")
        head = self.commit("임의 메시지")
        result = self.run_script(self.git("rev-parse", "main"), head)
        self.assertEqual(1, result.returncode)

    def test_empty_range_passes(self):
        """검증할 커밋가 없는 범위는 통과한다."""
        head = self.git("rev-parse", "HEAD")
        result = self.run_script(head, head)
        self.assertEqual(0, result.returncode, msg=result.stdout + result.stderr)

    def test_missing_commit_object_is_infrastructure_failure(self):
        """커밋 객체가 없으면 규칙 실패(1)가 아닌 환경 실패(2)로 종료한다."""
        result = self.run_script(
            "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
            self.git("rev-parse", "HEAD"),
        )
        self.assertEqual(2, result.returncode)
        self.assertIn("커밋 객체를 찾을 수 없습니다", result.stderr)


if __name__ == "__main__":
    unittest.main()
