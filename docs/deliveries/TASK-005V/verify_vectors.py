"""Standard-library fixture validator; never a production refund library."""
import argparse
import copy
import json
from pathlib import Path
import re
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REQS = {'REQ-032', 'REQ-033', 'REQ-034', 'REQ-035', 'REQ-036', 'REQ-039', 'REQ-094'}
FIELDS = {'id', 'reqRefs', 'formula', 'paidMinor', 'totalUnits',
          'remainingUnits', 'expectedRefundMinor', 'rationale'}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate(data):
    require(isinstance(data, dict) and set(data) == {'schemaVersion', 'cases'}, 'root fields')
    require(data['schemaVersion'] == '1', 'schemaVersion')
    require(isinstance(data['cases'], list) and len(data['cases']) >= 12, 'cases array >=12')
    ids, formulas = set(), set()
    for case in data['cases']:
        require(isinstance(case, dict) and set(case) == FIELDS, 'case fields')
        ident = case['id']
        require(isinstance(ident, str) and re.fullmatch(r'V[0-9]{2,}', ident), 'id format')
        require(ident not in ids, 'duplicate ID')
        ids.add(ident)
        refs = case['reqRefs']
        require(isinstance(refs, list) and refs and all(isinstance(r, str) for r in refs), 'reqRefs type')
        require(len(refs) == len(set(refs)) and set(refs) <= REQS, 'reqRefs values')
        formula = case['formula']
        require(formula in ('TIME_RATIO', 'COUNT_RATIO'), 'formula')
        require({'REQ-032', 'REQ-035', 'REQ-033' if formula == 'TIME_RATIO' else 'REQ-034'} <= set(refs), 'formula REQ mapping')
        formulas.add(formula)
        require(isinstance(case['rationale'], str) and case['rationale'].startswith('测试夹具：'), 'fixture rationale')
        values = []
        for field in ('paidMinor', 'totalUnits', 'remainingUnits', 'expectedRefundMinor'):
            value = case[field]
            require(isinstance(value, str) and re.fullmatch(r'0|[1-9][0-9]*', value), field + ' decimal string')
            values.append(int(value))
        paid, total, remaining, expected = values
        require(total > 0, 'totalUnits positive')
        require(remaining <= total, 'remainingUnits <= totalUnits')
        quotient, remainder = divmod(paid * remaining, total)
        actual = quotient + (2 * remainder >= total)
        require(actual == expected, ident + ' expected mismatch')
    require(formulas == {'TIME_RATIO', 'COUNT_RATIO'}, 'both formulas required')
    return len(ids)


def references(data):
    trace = (ROOT / 'docs/coordination/requirements-trace.md').read_text(encoding='utf-8')
    for ref in REQS:
        require(re.search(r'\| ' + ref + r' \|', trace), 'missing REQ ' + ref)
    scenarios = (HERE.parent / 'TASK-002/scenarios.md').read_text(encoding='utf-8')
    for scenario in ('D05', 'D06', 'D07', 'D08', 'D09', 'D26'):
        require('## ' + scenario + ' ' in scenarios, 'missing scenario ' + scenario)
    cases_text = (HERE / 'cases.md').read_text(encoding='utf-8')
    for case in data['cases']:
        require('| ' + case['id'] + ' |' in cases_text, 'missing derivation ' + case['id'])
    for name in ('cases.md', 'report.md'):
        for link in re.findall(r'\[[^\]]*\]\(([^)]+)\)', (HERE / name).read_text(encoding='utf-8')):
            require(not link.startswith(('http:', 'https:')), 'local references required')
            require((HERE / link.split('#')[0]).is_file(), 'missing file ' + link)


def negative_checks(data):
    mutations = (
        ('expected', lambda d: d['cases'][0].update(expectedRefundMinor='1')),
        ('duplicate', lambda d: d['cases'][1].update(id=d['cases'][0]['id'])),
        ('zero-total', lambda d: d['cases'][0].update(totalUnits='0')),
        ('excess-remaining', lambda d: d['cases'][0].update(remainingUnits='13')),
    )
    for name, mutate in mutations:
        damaged = copy.deepcopy(data)
        mutate(damaged)
        try:
            validate(damaged)
        except ValueError as error:
            print('REJECT', name, str(error))
        else:
            raise ValueError('corruption accepted: ' + name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--negative-only', action='store_true')
    args = parser.parse_args()
    data = json.loads((HERE / 'vectors.json').read_text(encoding='utf-8'))
    count = validate(data)
    references(data)
    if not args.negative_only:
        print('PASS', count, 'fixtures; schema, exact arithmetic, IDs and references')
    negative_checks(data)
    print('PASS 4/4 corrupt copies rejected in memory; no files written')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, TypeError, OSError, KeyError) as error:
        print('FAIL', str(error), file=sys.stderr)
        sys.exit(1)
