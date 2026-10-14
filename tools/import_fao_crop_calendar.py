#!/usr/bin/env python3
"""
Imports regional sowing / planting and harvest periods from the FAO Crop Calendar
(https://cropcalendar.apps.fao.org, API https://api-cropcalendar.apps.fao.org/api/v1) into the
calendar entries of data/seed/plants/*.json, for the regions of geography.json that lie in a
documented FAO agro-ecological zone (AEZ).

Usage (network access to api-cropcalendar.apps.fao.org):
    python3 tools/import_fao_crop_calendar.py            # report only
    python3 tools/import_fao_crop_calendar.py --apply    # write the entries
    (--cache DIR keeps / reuses the downloaded JSON files)

Rules:
- Each region is linked to ONE AEZ whose administrative units list the region (REGION_AEZ below).
- Each plant is linked to ONE FAO crop with the same species (PLANT_CROP); crops whose FAO record
  does not match the species are left out (e.g. FAO "Bean" 0026 is recorded as Panicum miliaceum).
- The FAO sowing period becomes the plant's start activity (nursery sowing for crops sown in
  nursery, planting for vegetative propagation); for nursery crops the transplanting period is
  derived from the minimum nursery duration of the plant sheet and labelled as such.
- Sessions marked as greenhouse / tunnel crops get methodId "greenhouse", the others "open_field".
- Sessions that look inconsistent (a non-all-year period covering more than 8 months, usually a
  typing error in the source) are skipped and reported.
- Entries get status "indicative" (zone-level data, the sheets are still demo data) and the source
  "fao-crop-calendar". Re-running the script replaces the entries it wrote before.

Licence: FAO content; check https://www.fao.org/contact-us/terms/en/ before a commercial use.
"""
import argparse, json, os, re, sys, urllib.request

API = "https://api-cropcalendar.apps.fao.org/api/v1"
ROOT = os.path.join(os.path.dirname(__file__), "..", "data", "seed")
SOURCE_ID = "fao-crop-calendar"
LANGS = ("fr", "en", "ar")

# region id -> (FAO country, AEZ id); the AEZ administrative units name the region.
REGION_AEZ = {
    "ma-rsk": ("MA", "001"),  # Tanger, Kenitra, Rabat, Casablanca, Fès, Meknès, Taza
    "ma-fm": ("MA", "001"),
    "ma-mar": ("MA", "003"),  # Safi, Essaouira, Marrakech
    "ma-dt": ("MA", "006"),   # d'Errachidia jusqu'à Eddakhla
    "dz-alg": ("DZ", "001"),  # Littoral Centre et Est (Alger)
    "dz-sti": ("DZ", "005"),  # Hautes plaines du Centre et de l'Est (Sétif)
    "dz-bsk": ("DZ", "007"),  # Sud (Biskra)
    "tn-tun": ("TN", "004"),  # Nord Est et Cap Bon (Grand Tunis)
    "tn-sfx": ("TN", "006"),  # Basse Steppe (Sfax)
    "eg-cai": ("EG", "001"),  # Région du Delta (Le Caire)
    "sn-dkr": ("SN", "001"),  # Niayes (Dakar)
    "sn-kao": ("SN", "004"),  # Bassin arachidier (Kaolack)
}

# plant id -> (FAO crop id, start activity, variety id or None)
PLANT_CROP = {
    "allium-cepa": ("0247", "nursery_sowing", None),
    "arachis-hypogaea": ("0262", "direct_sowing", None),
    "cicer-arietinum": ("0087", "direct_sowing", None),
    "daucus-carota": ("0073", "direct_sowing", None),
    "fragaria-x-ananassa": ("0333", "planting", None),
    "lactuca-sativa": ("0202", "direct_sowing", None),
    "mentha-spicata": ("0221", "planting", None),       # FAO: Mentha viridis L. (synonym)
    "solanum-lycopersicum": ("0356", "nursery_sowing", None),
    "solanum-tuberosum": ("0283", "planting", None),
    "triticum-aestivum": ("0373", "direct_sowing", None),
    "zea-mays": ("0113", "direct_sowing", "sweet"),     # FAO: sweet corn only
}

GREENHOUSE = re.compile(r"serre|tunnel|greenhouse|plastic house|sous abri", re.I)
NURSERY_ONLY = re.compile(r"seedling trays|nurser|pépinière", re.I)
# Notes that contradict the crop (copy errors in the source) are not shown.
SKIP_NOTE = {"lactuca-sativa": re.compile(r"végétative|vegetative|خضري", re.I),
             "zea-mays": re.compile(r"fourrage|fodder|forage|علف", re.I)}
MAX_MONTHS = 8


def fetch(country, lang, cache):
    path = os.path.join(cache, f"cal_{country}_{lang}.json") if cache else None
    if path and os.path.exists(path):
        return json.load(open(path, encoding="utf-8"))
    with urllib.request.urlopen(f"{API}/countries/{country}/cropCalendar?language={lang}", timeout=120) as r:
        data = json.load(r)
    if path:
        os.makedirs(cache, exist_ok=True)
        json.dump(data, open(path, "w", encoding="utf-8"), ensure_ascii=False)
    return data


def span(start, end):
    """Months from start to end inclusive, wrapping over the year."""
    s, e = int(start), int(end)
    out = [s]
    while out[-1] != e:
        out.append(out[-1] % 12 + 1)
    return out


def shift(months, by):
    return sorted({(m - 1 + by) % 12 + 1 for m in months})


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--cache", default=None)
    args = ap.parse_args()

    countries = sorted({c for c, _ in REGION_AEZ.values()})
    cal = {(c, l): fetch(c, l, args.cache) for c in countries for l in LANGS}

    def record(country, lang, crop_id, aez_id):
        return next((x for x in cal[(country, lang)] if x["crop"]["id"] == crop_id and x["aez"]["id"] == aez_id), None)

    report, skipped = [], []
    for plant_id, (crop_id, start, variety) in PLANT_CROP.items():
        path = os.path.join(ROOT, "plants", f"{plant_id}.json")
        plant = json.load(open(path, encoding="utf-8"))
        kept = [e for e in plant.get("calendar", []) if SOURCE_ID not in (e.get("sourceIds") or [])]
        new = []
        nursery_months = round((plant.get("nurseryWeeks") or {}).get("min", 0) / 4.345) if start == "nursery_sowing" else 0
        for region_id, (country, aez_id) in REGION_AEZ.items():
            recs = {l: record(country, l, crop_id, aez_id) for l in LANGS}
            if not recs["en"]:
                continue
            for i, s in enumerate(recs["en"]["sessions"]):
                all_year = s.get("all_year") == "yes"
                sow = list(range(1, 13)) if all_year else span(s["early_sowing"]["month"], s["later_sowing"]["month"])
                har = list(range(1, 13)) if all_year else span(s["early_harvest"]["month"], s["late_harvest"]["month"])
                if not all_year and (len(sow) > MAX_MONTHS or len(har) > MAX_MONTHS):
                    skipped.append(f"{plant_id} {region_id} session {i + 1}: sowing {sow[0]}-{sow[-1]}, harvest {har[0]}-{har[-1]}")
                    continue
                notes = {}
                for l in LANGS:
                    sess = (recs[l] or recs["en"])["sessions"][i] if recs[l] and i < len(recs[l]["sessions"]) else s
                    notes[l] = (sess.get("additional_information") or "").strip().replace("\r", " ").replace("\n", " ")
                if SKIP_NOTE.get(plant_id) and any(SKIP_NOTE[plant_id].search(n) for n in notes.values()):
                    if plant_id == "zea-mays":
                        skipped.append(f"{plant_id} {region_id} session {i + 1}: FAO note says fodder maize")
                        continue
                    notes = {l: "" for l in LANGS}
                info_en = notes["en"]
                method = "greenhouse" if GREENHOUSE.search(info_en + " " + notes["fr"]) and not NURSERY_ONLY.search(info_en) else "open_field"
                aez = {l: (recs[l] or recs["en"])["aez"]["name"].strip() for l in LANGS}
                updated = recs["en"].get("lastUpdated", "")
                base = {
                    "fr": f"FAO Crop Calendar, zone agro-écologique « {aez['fr']} » (mise à jour FAO : {updated}) : période de semis ou de plantation et période de récolte, au mois près.",
                    "en": f"FAO Crop Calendar, agro-ecological zone “{aez['en']}” (FAO update: {updated}): sowing or planting period and harvest period, to the nearest month.",
                    "ar": f"تقويم المحاصيل لمنظمة الأغذية والزراعة، المنطقة الزراعية البيئية «{aez['ar']}» (تحديث الفاو: {updated}): فترة البذر أو الغرس وفترة الحصاد، بدقة الشهر.",
                }
                for l in LANGS:
                    if notes[l] and len(notes[l]) <= 80:
                        base[l] += {"fr": f" Note FAO : {notes[l]}", "en": f" FAO note: {notes[l]}", "ar": f" ملاحظة الفاو: {notes[l]}"}[l]

                def entry(activity, months, assumptions):
                    e = {"activity": activity, "months": months, "regionId": region_id, "methodId": method}
                    if variety:
                        e["varietyId"] = variety
                    e.update({"status": "indicative", "assumptions": assumptions, "sourceIds": [SOURCE_ID]})
                    return e

                new.append(entry(start, sorted(sow), base))
                if nursery_months and not all_year:
                    derived = {
                        "fr": base["fr"] + f" Repiquage déduit : semis FAO + {nursery_months} mois de pépinière (durée minimale de la fiche).",
                        "en": base["en"] + f" Transplanting derived: FAO sowing + {nursery_months} month(s) in nursery (minimum duration of the sheet).",
                        "ar": base["ar"] + f" الشتل مستنتج: بذر الفاو + {nursery_months} شهر في المشتل (المدة الدنيا في البطاقة).",
                    }
                    new.append(entry("transplanting", shift(sow, nursery_months), derived))
                new.append(entry("harvest", sorted(har), base))
                report.append(f"{plant_id:22} {region_id:7} {method:10} {start} {sorted(sow)} harvest {sorted(har)}")
        unique = []
        for e in new:
            if e not in unique:
                unique.append(e)
        if args.apply:
            plant["calendar"] = kept + unique
            if new and SOURCE_ID not in plant.setdefault("sourceIds", []):
                plant["sourceIds"].append(SOURCE_ID)
            with open(path, "w", encoding="utf-8") as f:
                json.dump(plant, f, ensure_ascii=False, indent=1)
                f.write("\n")
    print("\n".join(report))
    print(f"\n{len(report)} sessions imported" + ("" if args.apply else " (dry run)"))
    if skipped:
        print("Skipped (inconsistent in the source):\n  " + "\n  ".join(skipped))


if __name__ == "__main__":
    main()
