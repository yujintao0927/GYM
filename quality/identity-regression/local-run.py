import os,pathlib,re,subprocess,sys
root=pathlib.Path(__file__).resolve().parents[2]
rules=(pathlib.Path.home()/'.codex/AGENTS.md').read_text(encoding='utf-8-sig')
m=re.search(r'密码[：:]\s*([^\s]+)',rules)
if not m:sys.exit('Private configuration unavailable')
env=os.environ.copy();env['GYM_DB_PASSWORD']=m.group(1);env['JAVA_HOME']='D:/java_environment';env['GYM_DB_URL']='jdbc:mysql://localhost:3306/gym_dev_test_003b'
sys.exit(subprocess.call([sys.executable,str(pathlib.Path(__file__).with_name('run.py'))],env=env))
