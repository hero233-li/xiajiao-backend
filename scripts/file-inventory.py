"""Read-only byte inventory. Run capture/compare ROOT MANIFEST with writes paused."""
import argparse, hashlib, json
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('action',choices=['capture','compare']);parser.add_argument('root',type=Path);parser.add_argument('manifest',type=Path)
a=parser.parse_args();root=a.root.resolve(strict=True)
def fingerprint(path):
 if path.is_symlink():raise SystemExit(f'Symlink requires manual verification: {path}')
 digest=hashlib.sha256()
 with path.open('rb') as stream:
  for chunk in iter(lambda:stream.read(1024*1024),b''):digest.update(chunk)
 return {'bytes':path.stat().st_size,'sha256':digest.hexdigest()}
if a.action=='capture':
 if a.manifest.exists():raise SystemExit('Manifest already exists; choose a new path')
 result={}
 for path in sorted(root.rglob('*')):
  if path.is_symlink():raise SystemExit(f'Symlink requires manual verification: {path}')
  if path.is_file():result[path.relative_to(root).as_posix()]=fingerprint(path)
 with a.manifest.open('x') as output:json.dump(result,output,ensure_ascii=False,indent=2)
 print(f'Captured {len(result)} files; contents were not exported.')
else:
 result=json.loads(a.manifest.read_text());bad=[]
 for name,expected in result.items():
  path=root/name
  if not path.resolve().is_relative_to(root):raise SystemExit('Manifest path escapes private root')
  if not path.is_file() or fingerprint(path)!=expected:bad.append(name)
 if bad:raise SystemExit('Original files missing or changed: '+', '.join(bad))
 print(f'Verified {len(result)} original files unchanged.')
