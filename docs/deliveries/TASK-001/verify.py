"""TASK-001 文档完整性检查；不连接网络或数据库，不验证业务实现。"""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[3]
DELIVERY = ROOT / "docs/deliveries/TASK-001"
def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT).decode("utf-8")

source = git("show", "5f5e091:docs/requirements.md").splitlines()
trace = git("show", "5f5e091:docs/coordination/requirements-trace.md")
coverage = (DELIVERY / "coverage.md").read_text(encoding="utf-8")
expected = {f"REQ-{i:03}" for i in range(1, 98)}
ids = re.findall(r"^\| (REQ-\d{3}) \|", coverage, re.M)
assert len(ids) == len(set(ids)) == 97
assert set(ids) == expected
mapped = set()
for rid, line in re.findall(r"^\| (REQ-\d{3}) \| \[.*? L(\d+)\]", trace, re.M):
    number = int(line)
    mapped.add(number)
    row = next(r for r in coverage.splitlines() if r.startswith(f"| {rid} |"))
    assert f"当前 L{number}：" in row
    assert source[number-1].replace("|", " / ") in row, rid
substantive = {i for i, s in enumerate(source, 1) if s.startswith("- ") or re.match(r"\d+\. ", s) or re.match(r"\| L\d\d ", s)}
assert substantive == mapped
scenarios = (DELIVERY / "scenarios.md").read_text(encoding="utf-8")
scenario_ids = re.findall(r"^\| (S\d{2}) ", scenarios, re.M)
assert len(scenario_ids) == len(set(scenario_ids)) == 23
for row in scenarios.splitlines():
    if re.match(r"^\| S\d{2} ", row):
        assert "REQ-" in row
for reference in re.findall(r"S\d{2}", coverage):
    assert reference in scenario_ids, reference
for i in range(1, 10):
    assert f"L{i:02} / REQ-{69+i:03}" in scenarios
links = []
for doc in DELIVERY.glob("*.md"):
    content = doc.read_text(encoding="utf-8")
    for target in re.findall(r"\]\(([^)]+)\)", content):
        if "://" in target or target.startswith("#"):
            continue
        assert (doc.parent / target.split("#")[0]).resolve().exists(), (doc.name, target)
        links.append(target)
    assert not re.search(r"(?:gh[pousr]_[A-Za-z0-9]{20,}|-----BEGIN .*PRIVATE KEY|(?:password|密码)\s*[:：=]\s*\S+)", content, re.I), doc.name
changed = set(git("diff", "--name-only", "8a5fc82").splitlines())
changed |= {s[3:] for s in git("status", "--porcelain", "--untracked-files=all").splitlines() if s.startswith("?? ")}
assert all(s.startswith("docs/deliveries/TASK-001/") for s in changed), changed
print(f"REQ={len(ids)}/97; missing={sorted(expected-set(ids))}; duplicate=0")
print(f"Source={len(mapped)}/97; unmapped={sorted(substantive-mapped)}; scenarios={len(scenario_ids)}; deferred=9/9")
print(f"Markdown links={len(links)} valid; scope files={len(changed)}; credential-pattern scan=PASS")
print("Business tests/build/database/network: NOT RUN (documentation review only)")
