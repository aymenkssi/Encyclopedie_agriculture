#!/usr/bin/env python3
"""
Compares (and optionally replaces) the growing requirements of data/seed/plants/*.json with the
FAO ECOCROP database: monthly mean temperatures for growth (absolute and optimal limits) and
optimal soil pH.

ECOCROP is distributed in the R package Recocrop (GPL >= 3, inst/parameters/ecocrop.rds):
    curl -LO https://raw.githubusercontent.com/cran/Recocrop/master/inst/parameters/ecocrop.rds
    pip install rdata pandas
Usage:
    python3 tools/import_ecocrop.py --rds ecocrop.rds                      # comparison report only
    python3 tools/import_ecocrop.py --rds ecocrop.rds --apply              # optimal pH of every matched plant
    python3 tools/import_ecocrop.py --rds ecocrop.rds --apply --fields ph temperature --only zea-mays
    (--csv FILE instead of --rds when the table was already exported to CSV)

What is written (with --apply): ph (optimal range PHOPMN-PHOPMX) and, only with --fields temperature,
temperature.min / optimalMin / optimalMax / max (ECOCROP TMIN, TOPMN, TOPMX, TMAX); the "fao-ecocrop"
source is added to the plant.

Temperatures are not written by default: ECOCROP gives the widest limits observed worldwide (e.g. tomato
7-35 °C, wheat 5-27 °C), while the AgriPedia calendar engine uses them as limits for a good crop in a
region. Written as is, they widen the calendars beyond usual practice (tomato in the hottest months of
Kaolack, wheat in Dakar, no wheat in Quebec...). Use them, region by region, to check the current values. The plant status is NOT changed: ECOCROP gives general, worldwide ranges (indicative), not
local recommendations. Cycle lengths (GMIN-GMAX) are only reported: ECOCROP counts the whole
growing season while AgriPedia counts from sowing / transplanting to the first harvest.
Hardiness (KTMP) is reported only.

Licence: ECOCROP is an FAO database. Cite it (reference.json, source "fao-ecocrop") and check the
FAO terms of use before a commercial use.
"""
import argparse, csv, glob, json, os, re, sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "data", "seed")
SOURCE_ID = "fao-ecocrop"
# ECOCROP lists several entries for some species: the one used for each plant.
PREFERRED = {"allium-cepa": "Onion", "zea-mays": "Maize", "fragaria-x-ananassa": "Garden strawberry", "citrus-sinensis": "Sweet orange"}


def load_table(args):
    if args.csv:
        return list(csv.DictReader(open(args.csv, encoding="utf-8")))
    import rdata  # noqa: E402 (optional dependency)
    df = rdata.read_rds(args.rds)
    return [{str(k): ("" if v is None else str(v)) for k, v in row.items()} for row in df.to_dict("records")]


def binomial(name):
    words = [w for w in re.split(r"\s+", name.replace("×", " x ").strip()) if w]
    if not words:
        return ""
    epithet = next((w for w in words[1:] if w.lower() not in ("x",) and w[:1].islower()), "")
    return f"{words[0]} {epithet}".lower().strip(".")


def num(v):
    try:
        f = float(v)
        return int(f) if f.is_integer() else f
    except (TypeError, ValueError):
        return None


def pick(plant, rows):
    names = [plant["scientificName"], *plant.get("synonyms", [])]
    keys = {binomial(n) for n in names}
    found = [r for r in rows if binomial(r["SCIENTNAME"]) in keys]
    pref = PREFERRED.get(plant["id"])
    if pref:
        found = [r for r in found if r["NAME"].replace("*", "").strip() == pref] or found
    return found


def fmt(a, b):
    return f"{a}–{b}" if a is not None else "—"


def main():
    ap = argparse.ArgumentParser()
    src = ap.add_mutually_exclusive_group(required=True)
    src.add_argument("--rds")
    src.add_argument("--csv")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--only", nargs="*", default=None, help="plant ids to update (default: all matched)")
    ap.add_argument("--fields", nargs="*", default=["ph"], choices=["ph", "temperature"], help="fields to write (default: ph)")
    args = ap.parse_args()
    rows = load_table(args)

    print("| Plante | ECOCROP | T min / opt / max actuel | ECOCROP | pH actuel | ECOCROP | Cycle actuel | ECOCROP (saison) |")
    print("|---|---|---|---|---|---|---|---|")
    changed = []
    for path in sorted(glob.glob(os.path.join(ROOT, "plants", "*.json"))):
        plant = json.load(open(path, encoding="utf-8"))
        matches = pick(plant, rows)
        t, ph, cyc = plant.get("temperature", {}), plant.get("ph") or {}, plant.get("cycleDays") or {}
        cur_t = f"{t.get('min')} / {t.get('optimalMin')}–{t.get('optimalMax')} / {t.get('max')}"
        if len(matches) != 1:
            label = "aucune correspondance" if not matches else "ambiguë : " + ", ".join(r["NAME"] for r in matches)
            print(f"| {plant['id']} | {label} | {cur_t} | | {fmt(ph.get('min'), ph.get('max'))} | | {fmt(cyc.get('min'), cyc.get('max'))} | |")
            continue
        r = matches[0]
        eco_t = f"{num(r['TMIN'])} / {num(r['TOPMN'])}–{num(r['TOPMX'])} / {num(r['TMAX'])}"
        print(f"| {plant['id']} | {r['NAME'].replace('*', '').strip()} ({r['SCIENTNAME']}) | {cur_t} | {eco_t} | "
              f"{fmt(ph.get('min'), ph.get('max'))} | {fmt(num(r['PHOPMN']), num(r['PHOPMX']))} | "
              f"{fmt(cyc.get('min'), cyc.get('max'))} | {fmt(num(r['GMIN']), num(r['GMAX']))} |")
        if not args.apply or (args.only is not None and plant["id"] not in args.only):
            continue
        written = False
        if "temperature" in args.fields:
            values = [num(r[k]) for k in ("TMIN", "TOPMN", "TOPMX", "TMAX")]
            if None not in values and values[0] <= values[1] <= values[2] <= values[3]:
                plant.setdefault("temperature", {}).update(dict(zip(("min", "optimalMin", "optimalMax", "max"), values)))
                written = True
        lo, hi = num(r["PHOPMN"]), num(r["PHOPMX"])
        if "ph" in args.fields and lo is not None and hi is not None and lo <= hi:
            plant["ph"] = {"min": float(lo), "max": float(hi)}
            written = True
        if not written:
            continue
        if SOURCE_ID not in plant.setdefault("sourceIds", []):
            plant["sourceIds"].append(SOURCE_ID)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(plant, f, ensure_ascii=False, indent=1)
            f.write("\n")
        changed.append(plant["id"])
    if changed:
        print(f"\nUpdated: {', '.join(changed)}. Increment manifest.json version, run `yarn catalog && yarn test`.", file=sys.stderr)


if __name__ == "__main__":
    main()
