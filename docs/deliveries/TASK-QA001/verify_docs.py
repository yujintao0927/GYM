"""Read-only structural checks; no business tests, network, DB or writes."""
from pathlib import Path
import re
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
def read(path):
    return path.read_text(encoding="utf-8")
coverage = read(HERE / "requirement-coverage.md")
cases = read(HERE / "risk-cases.md")
rows = re.findall(r"^\| (REQ-\d{3}) \| (.*)$", coverage, re.M)
expected = {f"REQ-{i:03d}" for i in range(1, 98)}
assert len(rows) == 97 and {r for r, _ in rows} == expected
qa_rows = re.findall(r"^\| (QA-\d{3}) (.*?) \| (REQ-.*?) \|", cases, re.M)
assert len(qa_rows) >= 25 and len({q for q, _, _ in qa_rows}) == len(qa_rows)
forward = {r: set(re.findall(r"QA-\d{3}", body)) for r, body in rows}
reverse = {q: set(re.findall(r"REQ-\d{3}", reqs)) for q, _, reqs in qa_rows}
for r, qs in forward.items():
    assert qs, r
    for q in qs:
        assert q in reverse and r in reverse[q], (r, q)
for q, reqs in reverse.items():
    for r in reqs:
        assert r in expected and q in forward[r], (q, r)
contracts = read(ROOT / "docs/deliveries/TASK-002/contracts.md")
known_c = set(re.findall(r"\bC\d{2}\b", contracts))
known_d = set(re.findall(r"^## (D\d{2})", read(ROOT / "docs/deliveries/TASK-002/scenarios.md"), re.M))
known_i = set(re.findall(r"^\| (I\d{2})", read(ROOT / "docs/deliveries/TASK-003D/authorization-matrix.md"), re.M))
links = 0
for file in HERE.glob("*.md"):
    body = read(file)
    for target in re.findall(r"\]\(([^)]+)\)", body):
        if target.startswith(("https://", "http://")):
            continue
        dest = target.split("#", 1)[0]
        assert (file.parent / dest).resolve().exists(), (file.name, target)
        links += 1
    for prefix, known in [("C", known_c), ("D", known_d), ("I", known_i)]:
        for ref in re.findall(r"\b" + prefix + r"\d{2}\b", body):
            assert ref in known, (file.name, ref)
    for ref in re.findall(r"\bQA-\d{3}\b", body):
        assert ref in reverse, (file.name, ref)
    for ref in re.findall(r"\bREQ-\d{3}\b", body):
        assert ref in expected, (file.name, ref)
assert all(f"L{i:02d}" in coverage for i in range(1, 10))
# Detect credential values from the applicable global rule without printing them.
global_rules = Path.home() / ".codex/AGENTS.md"
if global_rules.exists():
    secrets = re.findall(r"密码[：:]\s*([^\s]+)", read(global_rules))
    for file in HERE.iterdir():
        if file.is_file():
            assert all(value not in read(file) for value in secrets), "credential content detected"
for args in [["git", "diff", "--check"], ["git", "diff", "--cached", "--check"]]:
    subprocess.run(args, cwd=ROOT, check=True)
changed = subprocess.check_output(["git", "diff", "--name-only", "HEAD"], cwd=ROOT, text=True).splitlines()
untracked = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard"], cwd=ROOT, text=True).splitlines()
assert all(x.startswith("docs/deliveries/TASK-QA001/") for x in changed + untracked)
print(f"PASS: 97 unique REQ; {len(qa_rows)} QA; bidirectional matrix; C/D/I/REQ/QA references; {links} local links; L01-L09; credential/range/diff checks")
print("Document checks only; no future business or real-channel tests executed.")
