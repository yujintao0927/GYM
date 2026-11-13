"""Check delivery evidence and reject accidental secrets, without echoing matches."""
import json, pathlib, re, subprocess, sys
HERE=pathlib.Path(__file__).resolve().parent
ROOT=HERE.parents[1]
def verify():
 r=json.loads((HERE/'results.json').read_text(encoding='utf-8'))
 assert r['target']=='e20e499' and r['schema']=='gym_dev_test_003b'
 assert r['total']>=40 and r['total']==len(r['cases'])
 assert all(c['pass'] for c in r['cases'])
 names=[c['case'] for c in r['cases']];assert len(names)==len(set(names))
 assert r['passed']==r['total']
 rules=(pathlib.Path.home()/'.codex/AGENTS.md').read_text(encoding='utf-8-sig')
 secret=re.search(r'密码[：:]\s*([^\s]+)',rules).group(1)
 files=list(HERE.glob('*'))+list((ROOT/'docs/deliveries/TASK-NIGHT002').glob('*'))
 for p in files:
  if p.is_file():
   text=p.read_text(encoding='utf-8')
   assert secret not in text,'secret rejected'
   assert not re.search(r'(?<![\w-])[A-Za-z0-9_-]{43}(?![\w-])',text),'opaque value rejected'
 assert not subprocess.check_output(['git','diff','e20e499','--','backend'],cwd=ROOT)
 print('Evidence, scope baseline and secret checks passed; cases='+str(r['total']))
if __name__=='__main__':
 try:verify()
 except Exception:print('Delivery verification failed; details suppressed');sys.exit(1)
