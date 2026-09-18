import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("android_locales", ROOT / "scripts/check-android-locales.py")
checker = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = checker
spec.loader.exec_module(checker)


class AndroidLocaleCatalogTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.app_src = Path(self.temp.name)
        self.source_set = self.app_src / "main"

    def write(self, directory, filename, entries):
        path = self.source_set / "res" / directory / filename
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"<resources>{entries}</resources>", encoding="utf-8")
        return path

    def seed(self, locale="values-ko"):
        self.write("values", "strings.xml", '<string name="title">Chat</string>')
        self.write(locale, "strings.xml", '<string name="title">대화</string>')

    def errors(self):
        errors = []
        checker.validate_source_set(self.source_set, errors)
        return errors

    def test_auxiliary_placeholders_are_checked_for_every_existing_catalog(self):
        self.seed()
        for filename in ("pending_attachment_strings.xml", "petdex_capability_strings.xml", "voice_dock_strings.xml"):
            with self.subTest(filename=filename):
                self.write("values", filename, '<string name="count">%1$d items</string>')
                self.write("values-ko", filename, '<string name="count">%1$s개</string>')
                self.assertTrue(any("count" in error and "placeholders" in error for error in self.errors()))
                (self.source_set / "res" / "values" / filename).unlink()
                (self.source_set / "res" / "values-ko" / filename).unlink()

    def test_existing_auxiliary_coverage_cannot_disappear(self):
        self.seed("values-de")
        self.write("values", "pending_attachment_strings.xml", '<string name="pending">Pending</string>')
        self.assertTrue(any("missing string/pending" in error for error in self.errors()))

    def test_old_locales_may_fall_back_only_for_new_extracted_catalogs(self):
        self.seed("values-de")
        for index, filename in enumerate(sorted(checker.NEW_FALLBACK_CATALOGS)):
            self.write("values", filename, f'<string name="new_{index}">New UI</string>')
        self.assertEqual([], self.errors())
        self.write("values", "future_strings.xml", '<string name="future">Future UI</string>')
        self.assertTrue(any("missing string/future" in error for error in self.errors()))

    def test_korean_requires_every_new_extracted_key(self):
        self.seed()
        for index, filename in enumerate(sorted(checker.NEW_FALLBACK_CATALOGS)):
            self.write("values", filename, f'<string name="new_{index}">New UI</string>')
        errors = self.errors()
        for index in range(len(checker.NEW_FALLBACK_CATALOGS)):
            self.assertTrue(any(f"missing string/new_{index}" in error for error in errors))

    def test_partial_fallback_still_checks_present_translations(self):
        self.seed("values-de")
        self.write("values", "supervised_strings.xml", '<string name="optional">Optional</string><string name="count">%1$d</string>')
        self.write("values-de", "supervised_strings.xml", '<string name="count">%1$s</string>')
        errors = self.errors()
        self.assertFalse(any("missing" in error for error in errors))
        self.assertTrue(any("placeholders" in error for error in errors))

    def test_strings_catalog_remains_strict(self):
        self.seed("values-de")
        self.write("values", "strings.xml", '<string name="title">Chat</string><string name="required">Required</string>')
        self.write("values-de", "korean_coverage_strings.xml", '<string name="unknown">Extra</string>')
        errors = self.errors()
        self.assertTrue(any("missing string/required" in error for error in errors))
        self.assertTrue(any("extra string/unknown" in error for error in errors))

    def test_android_keys_may_live_in_a_different_filename(self):
        self.seed()
        self.write("values", "pending_attachment_strings.xml", '<string name="pending">Pending</string>')
        self.write("values-ko", "translated_pending.xml", '<string name="pending">대기 중</string>')
        self.assertEqual([], self.errors())

    def test_duplicate_resource_across_files_is_rejected(self):
        self.seed()
        self.write("values-ko", "extra.xml", '<string name="title">중복</string>')
        self.assertTrue(any("duplicate" in error for error in self.errors()))

    def test_false_translatable_cannot_hide_required_translation(self):
        self.seed()
        self.write("values-ko", "strings.xml", '<string name="title" translatable="false">대화</string>')
        self.assertTrue(any("missing string/title" in error for error in self.errors()))

    def test_nontranslatable_protocol_constants_do_not_require_translation(self):
        self.seed()
        self.write("values", "constants.xml", '<string name="protocol" translatable="false">wss://</string>')
        self.assertEqual([], self.errors())

    def test_plural_requires_other_and_preserves_printf_types(self):
        self.seed()
        self.write("values", "pending_attachment_strings.xml", '<plurals name="pending"><item quantity="one">%1$d item</item><item quantity="other">%1$d items</item></plurals>')
        self.write("values-ko", "pending_attachment_strings.xml", '<plurals name="pending"><item quantity="one">%1$s개</item></plurals>')
        errors = self.errors()
        self.assertTrue(any("missing required 'other'" in error for error in errors))
        self.assertTrue(any("placeholders" in error for error in errors))

    def test_korean_other_only_plural_is_valid(self):
        self.seed()
        self.write("values", "pending_attachment_strings.xml", '<plurals name="pending"><item quantity="one">%1$d item</item><item quantity="other">%1$d items</item></plurals>')
        self.write("values-ko", "pending_attachment_strings.xml", '<plurals name="pending"><item quantity="other">%1$d개</item></plurals>')
        self.assertEqual([], self.errors())

    def test_string_array_cannot_silently_drop_items(self):
        self.seed()
        self.write("values", "arrays.xml", '<string-array name="choices"><item>A</item><item>B</item></string-array>')
        self.write("values-ko", "arrays.xml", '<string-array name="choices"><item>가</item></string-array>')
        self.assertTrue(any("item count" in error for error in self.errors()))

    def test_all_catalog_changes_invalidate_source_hash(self):
        self.seed()
        path = self.write("values", "voice_dock_strings.xml", '<string name="dock">Dock</string>')
        with patch.object(checker, "APP_SRC", self.app_src):
            before = checker.source_catalog_hashes([])
            path.write_text('<resources><string name="dock">Voice dock</string></resources>', encoding="utf-8")
            self.assertNotEqual(before, checker.source_catalog_hashes([]))

    def test_auxiliary_only_locale_is_discovered(self):
        self.write("values", "voice_dock_strings.xml", '<string name="dock">Dock</string>')
        self.write("values-ko", "voice_dock_strings.xml", '<string name="dock">음성 도크</string>')
        errors = []
        self.assertEqual(1, checker.validate_source_set(self.source_set, errors))
        self.assertEqual([], errors)

    def test_korean_flavor_directory_cannot_silently_disappear(self):
        self.seed()
        source_set = self.app_src / "sideload"
        values = source_set / "res" / "values"
        values.mkdir(parents=True)
        (values / "strings.xml").write_text('<resources><string name="sideload">Control</string></resources>', encoding="utf-8")
        errors = []
        checker.validate_source_set(source_set, errors)
        self.assertTrue(any("sideload/values-ko: missing string/sideload" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
