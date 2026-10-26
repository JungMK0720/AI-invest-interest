"""Export repository slices without modifying the source or creating Git remotes."""
from pathlib import Path, PurePosixPath
import argparse
import json
import shutil
import subprocess

REPOSITORIES = ('front', 'back', 'infra', 'docs')
EXCLUDED = {'.git', '.agents', '.codex', '.aws', 'node_modules', 'target', 'dist', '.vite', '__pycache__', '.venv', '.pytest_cache', '.build'}


def is_link(path):
    return path.is_symlink() or getattr(path, 'is_junction', lambda: False)()


def guard_path(root, path):
    relative = path.relative_to(root)
    current = root
    for part in relative.parts:
        current = current / part
        if is_link(current):
            raise ValueError('Symlinks and junctions are refused: ' + str(relative))
    if not path.resolve().is_relative_to(root):
        raise ValueError('Path leaves workspace')


def export(root, output, dry_run=False):
    root = root.resolve()
    output = output if output.is_absolute() else root / output
    guard_path(root, output)
    output = output.resolve()
    relative_output = output.relative_to(root)
    if not relative_output.parts or relative_output.parts[0] in {*REPOSITORIES, '.git', '.agents', '.codex', '.aws', 'tools'}:
        raise ValueError('Output cannot be the workspace or a source/config directory')
    if output.exists() and (not output.is_dir() or any(output.iterdir())):
        raise ValueError('Output must be new or empty; existing files are never overwritten')
    for repository in REPOSITORIES:
        if not (root / repository).is_dir():
            raise ValueError('Missing source directory: ' + repository)
    result = subprocess.run(['git', '-C', str(root), 'ls-files', '--cached', '--others', '--exclude-standard', '-z', '--', *REPOSITORIES], capture_output=True, check=True)
    files = []
    skipped = 0
    counts = dict.fromkeys(REPOSITORIES, 0)
    for name in sorted(set(result.stdout.decode('utf-8').split('\0')) - {''}):
        relative = PurePosixPath(name)
        if relative.is_absolute() or '..' in relative.parts or '\\' in name or relative.parts[0] not in REPOSITORIES:
            raise ValueError('Unsafe Git path')
        source = root.joinpath(*relative.parts)
        guard_path(root, source)
        if not source.exists():
            continue
        filename = relative.name.lower()
        secret = ((filename == '.env' or filename.startswith('.env.')) and not filename.endswith('.example')) or relative.suffix.lower() in {'.key', '.pem', '.p12', '.pfx', '.jks'} or filename in {'credentials.json', 'service-account.json', 'id_rsa', 'id_ed25519'}
        if secret or filename in EXCLUDED or any(part.lower() in EXCLUDED for part in relative.parts[:-1]) or filename.endswith(('.pyc', '.log', '.tsbuildinfo')):
            skipped += 1
            continue
        if not source.is_file():
            raise ValueError('Not a regular file: ' + name)
        files.append((source, output.joinpath(*relative.parts)))
        counts[relative.parts[0]] += 1
    if not all(counts.values()):
        raise ValueError('Each repository must have eligible files')
    if not dry_run:
        output.mkdir(parents=True, exist_ok=True)
        for source, target in files:
            target.parent.mkdir(parents=True, exist_ok=True)
            with source.open('rb') as src, target.open('xb') as dst:
                shutil.copyfileobj(src, dst)
        commit = subprocess.run(['git', '-C', str(root), 'rev-parse', '--verify', 'HEAD'], capture_output=True).stdout.decode('ascii').strip()
        manifest = {'source_commit': commit or None, 'source': 'current working tree, including nonignored untracked files', 'files': counts, 'excluded_count': skipped}
        with (output / 'export-manifest.json').open('x', encoding='utf-8') as stream:
            json.dump(manifest, stream, ensure_ascii=False, indent=2)
    return counts, skipped


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path('.build/repositories'))
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    try:
        counts, skipped = export(Path(__file__).resolve().parents[1], args.output, args.dry_run)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, 'Export refused: ' + str(error) + '\n')
    print(('Would export' if args.dry_run else 'Exported') + ': ' + ', '.join(f'{name}={count}' for name, count in counts.items()))
    print(f'Excluded secret/cache/environment files: {skipped}')


if __name__ == '__main__':
    main()

