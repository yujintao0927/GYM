from pathlib import Path
import re,struct,xml.etree.ElementTree as E,subprocess
O=Path(__file__).resolve().parent
s=(O/'page-inventory.md').read_text(encoding='utf-8')
rows=[x for x in s.splitlines() if x.startswith('| PAGE-')]
assert len(rows)==24
forward={}
for line in rows:
 p=line.split('|'); pid=re.search(r'PAGE-\d{2}',p[1])[0]
 forward[pid]=(set(re.findall(r'REQ-\d{3}',p[6])),set(re.findall(r'C\d{2}',p[5])))
for n in range(1,98):
 req=f'REQ-{n:03}'; lines=[x for x in s.splitlines() if x.startswith('- '+req+' →')];assert len(lines)==1,req
 expected={pid for pid,(rs,cs) in forward.items() if req in rs}
 if expected:assert set(re.findall(r'PAGE-\d{2}',lines[0]))==expected,req
for n in range(1,15):
 c=f'C{n:02}';line=next(x for x in s.splitlines() if x.startswith('- '+c+' →'))
 assert set(re.findall(r'PAGE-\d{2}',line))=={pid for pid,(rs,cs) in forward.items() if c in cs},c
links=0
for f in O.rglob('*.md'):
 for t in re.findall(r'\]\(([^)]+)\)',f.read_text(encoding='utf-8')):
  if not t.startswith(('http:','https:','#')):assert (f.parent/t.split('#')[0]).exists(),(f,t);links+=1
svgs=list((O/'wireframes').glob('*.svg'));assert len(svgs)==9
for f in svgs:
 E.parse(f);png=O/'wireframes/previews'/f.with_suffix('.png').name
 b=png.read_bytes();assert b[:8]==b'\x89PNG\r\n\x1a\n';assert struct.unpack('>II',b[16:24])==(1200,1000)
tracked=subprocess.check_output(['git','status','--porcelain','--untracked-files=all'],text=True)
assert all(x[3:].startswith('docs/deliveries/TASK-UX001/') for x in tracked.splitlines()),tracked
for f in O.rglob('*'):
 if f.suffix in ('.md','.svg','.html','.py'):
  t=f.read_text(encoding='utf-8-sig');assert not re.search(r'gh[pousr]_[A-Za-z0-9]{20,}|' + '-----BEGIN ' + r'[A-Z ]+' + 'PRIVATE' + ' KEY',t),f
print(f'PASS: 24 PAGE, 97 REQ, 14 C bidirectional; {links} links; 9 SVG/PNG 1200x1000; task-only scope; sensitive token patterns')
