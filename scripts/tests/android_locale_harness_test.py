import argparse
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("locale_harness", ROOT / "scripts/android-locale-harness.py")
harness = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = harness
spec.loader.exec_module(harness)


class AndroidLocaleHarnessTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.app_src = self.root / "app" / "src"
        self.status = self.root / "status.json"
        self.status.write_text('{"locales":{"en":{"verification":"canonical"}}}', encoding="utf-8")
        self.addCleanup(patch.stopall)
        patch.object(harness, "ROOT", self.root).start()
        patch.object(harness, "APP_SRC", self.app_src).start()
        patch.object(harness, "STATUS", self.status).start()
        self.write("main", "strings.xml", '<string name="title">Chat</string>')
        self.write("main", "pending_attachment_strings.xml", '<string name="pending">%1$d pending</string>')
        self.write("sideload", "strings.xml", '<string name="control">Control</string>')
        self.args = argparse.Namespace(tag="ko", native_name="한국어", force=False)

    def write(self, source_set, filename, entries):
        path = self.app_src / source_set / "res" / "values" / filename
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"<resources>{entries}</resources>", encoding="utf-8")
        return path

    def prepare(self):
        with contextlib.redirect_stdout(io.StringIO()):
            harness.prepare(self.args)
        return self.root / "build" / "i18n" / "ko"

    def test_prepare_and_install_include_auxiliary_catalogs_and_shared_hashes(self):
        draft = self.prepare()
        self.assertTrue((draft / "main" / "pending_attachment_strings.xml").is_file())
        with contextlib.redirect_stdout(io.StringIO()):
            harness.install(self.args)
        self.assertTrue((self.app_src / "main" / "res" / "values-ko" / "pending_attachment_strings.xml").is_file())
        registry = json.loads(self.status.read_text(encoding="utf-8"))
        self.assertEqual(
            harness.checker.source_catalog_hashes([], self.app_src),
            registry["locales"]["ko"]["source_sha256"],
        )

    def test_auxiliary_source_changes_reject_stale_draft(self):
        self.prepare()
        self.write("main", "pending_attachment_strings.xml", '<string name="pending">%1$d ready</string>')
        with self.assertRaisesRegex(ValueError, "English catalogs changed"):
            harness.install(self.args)
        self.assertFalse((self.app_src / "main" / "res" / "values-ko").exists())

    def test_missing_auxiliary_draft_fails_before_installing_any_file(self):
        draft = self.prepare()
        (draft / "main" / "pending_attachment_strings.xml").unlink()
        with self.assertRaisesRegex(ValueError, "catalog keys differ"):
            harness.install(self.args)
        self.assertFalse((self.app_src / "main" / "res" / "values-ko").exists())

    def test_broken_auxiliary_placeholder_fails_before_installing_any_file(self):
        draft = self.prepare()
        (draft / "main" / "pending_attachment_strings.xml").write_text('<resources><string name="pending">%1$s개 대기 중</string></resources>', encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "placeholders"):
            harness.install(self.args)
        self.assertFalse((self.app_src / "main" / "res" / "values-ko").exists())

    def test_old_strings_only_manifest_requires_regeneration(self):
        draft = self.prepare()
        path = draft / "manifest.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data["schema_version"] = 1
        path.write_text(json.dumps(data), encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "regenerate the draft"):
            harness.install(self.args)

    def test_existing_flavor_locale_prevents_partial_install(self):
        self.prepare()
        destination = self.app_src / "sideload" / "res" / "values-ko"
        destination.mkdir(parents=True)
        (destination / "strings.xml").write_text('<resources/>', encoding="utf-8")
        with self.assertRaises(FileExistsError):
            harness.install(self.args)
        self.assertFalse((self.app_src / "main" / "res" / "values-ko").exists())


if __name__ == "__main__":
    unittest.main()
