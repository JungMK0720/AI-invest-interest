from pathlib import Path
import subprocess
import tempfile
import unittest
from export_repositories import export

class ExportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        subprocess.run(['git', '-C', str(self.root), 'init', '-q'], check=True)
        for name in ('front', 'back', 'infra', 'docs'):
            self.write(name + '/README.md', name)

    def write(self, name, content):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        return path

    def test_tracked_new_unicode_deleted_ignored_and_secret(self):
        self.write('.gitignore', 'node_modules/\n.build/\n')
        deleted = self.write('back/deleted.txt', 'old')
        self.write('back/.env', 'SECRET=hidden')
        self.write('back/.env.example', 'SECRET=')
        self.write('back/private.pem', 'PRIVATE KEY')
        subprocess.run(['git', '-C', str(self.root), 'add', '.'], check=True)
        deleted.unlink()
        self.write('docs/증권업.md', '선물과 옵션')
        self.write('front/node_modules/ignored', 'ignored')
        export(self.root, Path('.build/repos'))
        result = self.root / '.build/repos'
        self.assertEqual((result / 'docs/증권업.md').read_text(encoding='utf-8'), '선물과 옵션')
        self.assertTrue((result / 'back/.env.example').exists())
        self.assertFalse((result / 'back/.env').exists())
        self.assertFalse((result / 'back/private.pem').exists())
        self.assertFalse((result / 'back/deleted.txt').exists())
        self.assertFalse((result / 'front/node_modules').exists())

    def test_refuses_existing_output_and_source_or_outside_output(self):
        sentinel = self.write('.build/repos/keep', 'keep')
        for output in (Path('.build/repos'), self.root, self.root.parent / 'escape', Path('front/export'), Path('.build/../front/export')):
            with self.subTest(output=output), self.assertRaises(ValueError):
                export(self.root, output)
        self.assertEqual(sentinel.read_text(), 'keep')

    def test_refuses_symlink_before_writing(self):
        target = self.write('back/data.txt', 'data')
        try:
            (self.root / 'docs/link.txt').symlink_to(target)
        except OSError:
            self.skipTest('Symlink creation unavailable on this host')
        with self.assertRaisesRegex(ValueError, 'Symlinks'):
            export(self.root, Path('.build/repos'))
        self.assertFalse((self.root / '.build').exists())

if __name__ == '__main__':
    unittest.main()

