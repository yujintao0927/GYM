"""Local replay: credential is read privately, never printed or stored."""
import os,re,subprocess,sys
from pathlib import Path
worktree=Path(__file__).resolve().parents[3]
text=(Path.home()/'.codex'/'AGENTS.md').read_text(encoding='utf-8-sig')
match=re.search(r'密码[：:]\s*([^\s`]+)',text)
if not match: raise SystemExit('Local credential configuration unavailable')
env=os.environ.copy()
env.update(GYM_TEST_DB_URL='jdbc:mysql://localhost:3306/gym_dev_test_biz004?createDatabaseIfNotExist=true&characterEncoding=UTF-8&connectTimeout=5000&socketTimeout=10000',GYM_DB_USERNAME='root',GYM_DB_PASSWORD=match.group(1))
name=sys.argv[1] if len(sys.argv)>1 else 'verify-final.log'
with (Path(__file__).parent/name).open('w') as log:
 result=subprocess.run(['cmd','/c','mvnw.cmd','-f','../modules/operations-core/pom.xml','-B','clean','verify'],cwd=worktree/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,timeout=180)
print('Maven exit:',result.returncode)
sys.exit(result.returncode)
