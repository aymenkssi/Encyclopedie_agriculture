#!/usr/bin/env python3
"""
Checks every plant's scientific name against the GBIF Backbone Taxonomy
(species/match API, CC BY 4.0) and reports mismatches (unknown name, synonym,
different family). It never edits the dataset: a human reviews the report.

Usage (requires network access to api.gbif.org):
    python3 tools/check_taxonomy_gbif.py > taxonomy_report.txt
"""
import json, os, urllib.parse, urllib.request

ROOT = os.path.join(os.path.dirname(__file__), "..", "data", "seed")
manifest = json.load(open(os.path.join(ROOT, "manifest.json"), encoding="utf-8"))
for f in manifest["plantFiles"]:
    p = json.load(open(os.path.join(ROOT, "plants", f), encoding="utf-8"))
    q = urllib.parse.urlencode({"name": p["scientificName"].replace("×", "x"), "kingdom": "Plantae", "strict": "false"})
    with urllib.request.urlopen(f"https://api.gbif.org/v1/species/match?{q}", timeout=30) as r:
        m = json.load(r)
    issues = []
    if m.get("matchType") == "NONE": issues.append("no match")
    if m.get("status") not in (None, "ACCEPTED"): issues.append(f"status={m.get('status')} accepted={m.get('species')}")
    if m.get("family") and m["family"] != p["family"]: issues.append(f"family {p['family']} != GBIF {m['family']}")
    print(f"{p['id']:<24} {m.get('scientificName', '-'):<50} {'OK' if not issues else '; '.join(issues)}")
