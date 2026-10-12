#!/usr/bin/env python3
"""
Replaces the approximate climate normals of data/seed/geography.json with values
computed from the Open-Meteo Historical Weather API (ERA5 reanalysis, CC BY 4.0).

Usage (requires network access to archive-api.open-meteo.com):
    python3 tools/import_climate_normals.py --start 1991 --end 2020 [--region fr-idf ...] [--dry-run]

For each region it downloads daily mean temperature, minimum temperature and
precipitation at the region coordinates, computes monthly means (temperatures)
and mean monthly totals (precipitation), and writes them back with
climateStatus = "indicative" and the "open-meteo" source. Frost months are NOT
overwritten (they encode local agronomic knowledge) unless --recompute-frost is set,
in which case months whose mean daily minimum is below 2 °C are flagged.

Licence: Open-Meteo data is CC BY 4.0 — the attribution is kept in reference.json.
Check the API terms (rate limits, non-commercial use of the free tier) before use.
"""
import argparse, json, os, sys, time, urllib.request, urllib.parse
from collections import defaultdict

API = "https://archive-api.open-meteo.com/v1/archive"
ROOT = os.path.join(os.path.dirname(__file__), "..", "data", "seed")


def fetch(lat, lon, start, end):
    q = urllib.parse.urlencode({
        "latitude": lat, "longitude": lon, "start_date": f"{start}-01-01", "end_date": f"{end}-12-31",
        "daily": "temperature_2m_mean,temperature_2m_min,precipitation_sum", "timezone": "UTC",
    })
    with urllib.request.urlopen(f"{API}?{q}", timeout=120) as r:
        return json.load(r)["daily"]


def normals(daily, years):
    t, tmin, p = defaultdict(list), defaultdict(list), defaultdict(float)
    for d, a, b, c in zip(daily["time"], daily["temperature_2m_mean"], daily["temperature_2m_min"], daily["precipitation_sum"]):
        m = int(d[5:7])
        if a is not None: t[m].append(a)
        if b is not None: tmin[m].append(b)
        if c is not None: p[m] += c
    r = lambda x: round(x, 1)
    return ([r(sum(t[m]) / len(t[m])) for m in range(1, 13)],
            [r(sum(tmin[m]) / len(tmin[m])) for m in range(1, 13)],
            [r(p[m] / years) for m in range(1, 13)])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", type=int, default=1991)
    ap.add_argument("--end", type=int, default=2020)
    ap.add_argument("--region", action="append")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--recompute-frost", action="store_true")
    a = ap.parse_args()
    path = os.path.join(ROOT, "geography.json")
    geo = json.load(open(path, encoding="utf-8"))
    for c in geo["countries"]:
        for reg in c["regions"]:
            if a.region and reg["id"] not in a.region:
                continue
            print(f"{reg['id']} ({reg['latitude']}, {reg['longitude']})…", file=sys.stderr)
            mean, tmin, prec = normals(fetch(reg["latitude"], reg["longitude"], a.start, a.end), a.end - a.start + 1)
            reg.update(monthlyMeanTempC=mean, monthlyMinTempC=tmin, monthlyPrecipMm=prec, climateStatus="indicative")
            reg["sourceIds"] = sorted(set(reg.get("sourceIds", [])) - {"climate-approx"} | {"open-meteo", "koppen-beck2018"})
            if a.recompute_frost:
                reg["frostMonths"] = [m + 1 for m in range(12) if tmin[m] < 2.0]
            time.sleep(1)  # be gentle with the free API
    if a.dry_run:
        print(json.dumps(geo, ensure_ascii=False, indent=1)[:2000])
    else:
        json.dump(geo, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print("geography.json updated — bump manifest.json 'version' so apps re-import the content.", file=sys.stderr)


if __name__ == "__main__":
    main()
