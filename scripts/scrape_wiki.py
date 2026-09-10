#!/usr/bin/env python3
"""
Witchfire Wiki Scraper
Fetches structured data and icons from https://witchfire.wiki.gg via MediaWiki Cargo & Action API.
Outputs:
  - src/main/resources/data/items.json
  - src/main/resources/data/arcana.json
  - src/main/resources/data/prophecies.json
  - src/main/resources/data/enemies.json
  - src/main/resources/static/images/items/<id>.webp
  - src/main/resources/static/images/arcana/<id>.webp
  - src/main/resources/static/images/prophecies/<id>.webp
  - src/main/resources/static/images/enemies/<id>.webp

Images are downloaded as the wiki's own PNGs and then re-encoded to WebP by
scripts/optimize_images.py, which main() runs as its final step.
"""

import os
import re
import json
import time
import requests

BASE_URL = "https://witchfire.wiki.gg/api.php"
HEADERS = {"User-Agent": "WitchfireRandomizerScraper/1.0 (contact@hendrikhoemberg.dev)"}

DATA_DIR = os.path.abspath("src/main/resources/data")
ITEMS_IMAGE_DIR = os.path.abspath("src/main/resources/static/images/items")
ARCANA_IMAGE_DIR = os.path.abspath("src/main/resources/static/images/arcana")
PROPHECIES_IMAGE_DIR = os.path.abspath("src/main/resources/static/images/prophecies")
ENEMIES_IMAGE_DIR = os.path.abspath("src/main/resources/static/images/enemies")

for d in [DATA_DIR, ITEMS_IMAGE_DIR, ARCANA_IMAGE_DIR, PROPHECIES_IMAGE_DIR, ENEMIES_IMAGE_DIR]:
    os.makedirs(d, exist_ok=True)

def slugify(text):
    text = text.lower().strip()
    text = re.sub(r"[^\w\s-]", "", text)
    return re.sub(r"[\s_-]+", "-", text)

def clean_wiki(text):
    if not text:
        return ""
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\{\{[Cc]olor\|[^\|]+\|([^}]+)\}\}", r"\1", text)
    text = re.sub(r"\{\{[Ii]con\|([^}]+)\}\}", r"\1", text)
    text = re.sub(r"\{\{([^\|}]+)\}\}", r"\1", text)
    text = re.sub(r"\[\[[^\|\]]+\|([^\]]+)\]\]", r"\1", text)
    text = re.sub(r"\[\[([^\]]+)\]\]", r"\1", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()

def parse_locations(raw):
    if not raw:
        return []
    parts = re.split(r"<br\s*/?>|\n", raw)
    locs = []
    for p in parts:
        cleaned = clean_wiki(p)
        if cleaned and cleaned not in locs:
            locs.append(cleaned)
    return locs

def parse_bead_requirements(raw):
    if not raw:
        return []
    reqs = []
    for part in re.split(r"<br\s*/?>|,|;", raw):
        m = re.search(r"(?:\[\[)?([A-Za-z]+)(?:\]\])?\s*(\d+)", part)
        if m:
            reqs.append({"stat": m.group(1).capitalize(), "value": int(m.group(2))})
    return reqs

def parse_int_or_none(val):
    if val is None:
        return None
    val_str = str(val).strip()
    if not val_str:
        return None
    try:
        return int(float(val_str))
    except ValueError:
        return None

def fetch_cargo(table, fields):
    url = f"{BASE_URL}?action=cargoquery&tables={table}&fields={','.join(fields)}&limit=500&format=json"
    res = requests.get(url, headers=HEADERS)
    res.raise_for_status()
    data = res.json()
    return [row["title"] for row in data.get("cargoquery", [])]

def fetch_images(file_titles):
    image_urls = {}
    chunk_size = 40
    for i in range(0, len(file_titles), chunk_size):
        chunk = file_titles[i:i+chunk_size]
        titles_param = "|".join(chunk)
        url = f"{BASE_URL}?action=query&titles={titles_param}&prop=imageinfo&iiprop=url&format=json"
        res = requests.get(url, headers=HEADERS).json()
        pages = res.get("query", {}).get("pages", {})
        for p in pages.values():
            title = p.get("title")
            if "imageinfo" in p and p["imageinfo"]:
                image_urls[title] = p["imageinfo"][0]["url"]
        time.sleep(0.1)
    return image_urls

def fetch_revisions(page_titles):
    revisions = {}
    chunk_size = 40
    for i in range(0, len(page_titles), chunk_size):
        chunk = file_titles = page_titles[i:i+chunk_size]
        titles_param = "|".join(chunk)
        url = f"{BASE_URL}?action=query&titles={titles_param}&prop=revisions&rvprop=content&rvslots=main&format=json"
        res = requests.get(url, headers=HEADERS).json()
        pages = res.get("query", {}).get("pages", {})
        for p in pages.values():
            title = p.get("title")
            revs = p.get("revisions", [])
            wt = revs[0].get("slots", {}).get("main", {}).get("*", "") if revs else ""
            revisions[title] = wt
        time.sleep(0.1)
    return revisions

def parse_mysteria(wikitext):
    tiers = []
    if not wikitext:
        return tiers
    match = re.search(r"\{\{Mysteria\s*(.*?)\n\}\}", wikitext, re.DOTALL | re.IGNORECASE)
    if not match:
        return tiers
    content = match.group(1)
    for lvl in [1, 2, 3]:
        effects = [clean_wiki(m.group(1)) for m in re.finditer(rf"\|l{lvl}c\d+=(.*?)(?=\|[lr]|\Z)", content, re.DOTALL)]
        reqs = [clean_wiki(m.group(1)) for m in re.finditer(rf"\|l{lvl}r\d+=(.*?)(?=\|[lr]|\Z)", content, re.DOTALL)]
        if effects or reqs:
            tiers.append({
                "level": lvl,
                "effect": effects[0] if len(effects) == 1 else None,
                "charismata": effects if len(effects) > 1 or not effects else None,
                "requirements": reqs
            })
    return tiers

def download_image(url, dest_path):
    if os.path.exists(dest_path) and os.path.getsize(dest_path) > 0:
        return
    try:
        res = requests.get(url, headers=HEADERS, timeout=10)
        if res.status_code == 200:
            with open(dest_path, "wb") as f:
                f.write(res.content)
    except Exception as e:
        print(f"Warning: failed to download {url}: {e}")

def scrape_items():
    print("Scraping items from wiki.gg...")
    weapons_raw = fetch_cargo("Weapons", [
        "_pageName", "name", "description", "rangeCategory", "type", "element1",
        "fireMode", "damage", "criticalDamage", "stunPower", "adsRange", "hipfireRange",
        "rateOfFire", "reloadSpeed", "stability", "mobility", "magSize", "ammoReserves", "location"
    ])
    melee_raw = fetch_cargo("MeleeWeapons", [
        "_pageName", "name", "description", "element", "baseDamage", "chargedDamage",
        "specialAttack", "specialDamage", "location", "type"
    ])
    spells_raw = fetch_cargo("Spells", [
        "_pageName", "name", "description", "type", "charges", "recharge",
        "element1", "element2", "location"
    ])
    magical_raw = fetch_cargo("MagicalItems", [
        "_pageName", "name", "description", "type", "element", "location"
    ])
    beads_raw = fetch_cargo("Beads", [
        "_pageName", "name", "description", "requirement", "type", "location"
    ])

    all_page_names = []
    file_map = {}
    for group in [weapons_raw, melee_raw, spells_raw, magical_raw, beads_raw]:
        for item in group:
            pname = item.get("_pageName") or item.get("name")
            all_page_names.append(pname)
            file_map[pname] = f"File:{item['name']}.png"

    image_urls = fetch_images(list(file_map.values()))
    revisions = fetch_revisions(all_page_names)

    items = []
    for w in weapons_raw:
        item_id = f"w-{slugify(w['name'])}"
        pname = w.get("_pageName") or w["name"]
        wt = revisions.get(pname, "")
        mysteria = parse_mysteria(wt)
        range_cat = w.get("rangeCategory", "")
        category = "DEMONIC_WEAPON" if "Demonic" in range_cat else "WEAPON"
        img_file = file_map.get(pname)
        img_url = image_urls.get(img_file)
        local_icon = f"/images/items/{item_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ITEMS_IMAGE_DIR, f"{item_id}.png"))

        items.append({
            "id": item_id,
            "name": w["name"],
            "category": category,
            "rangeCategory": range_cat,
            "weaponFamily": w.get("type", "").strip(),
            "element": w.get("element1") if w.get("element1") else None,
            "description": clean_wiki(w.get("description", "")),
            "damage": int(w["damage"]) if w.get("damage") else 0,
            "criticalDamage": int(w["criticalDamage"]) if w.get("criticalDamage") else 0,
            "stunPower": w.get("stunPower", ""),
            "adsRange": float(w["adsRange"]) if w.get("adsRange") else 0.0,
            "hipfireRange": float(w["hipfireRange"]) if w.get("hipfireRange") else 0.0,
            "rateOfFire": float(w["rateOfFire"]) if w.get("rateOfFire") else 0.0,
            "reloadSpeed": float(w["reloadSpeed"]) if w.get("reloadSpeed") else 0.0,
            "stability": w.get("stability", ""),
            "mobility": w.get("mobility", ""),
            "magSize": int(w["magSize"]) if w.get("magSize") else 0,
            "ammoReserves": int(w["ammoReserves"]) if w.get("ammoReserves") else 0,
            "fireMode": w.get("fireMode", ""),
            "location": clean_wiki(w.get("location", "")),
            "iconUrl": local_icon,
            "mysteriumTiers": mysteria
        })

    for mw in melee_raw:
        item_id = f"mw-{slugify(mw['name'])}"
        pname = mw.get("_pageName") or mw["name"]
        wt = revisions.get(pname, "")
        mysteria = parse_mysteria(wt)
        img_file = file_map.get(pname)
        img_url = image_urls.get(img_file)
        local_icon = f"/images/items/{item_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ITEMS_IMAGE_DIR, f"{item_id}.png"))

        items.append({
            "id": item_id,
            "name": mw["name"],
            "category": "MELEE_WEAPON",
            "element": mw.get("element") if mw.get("element") else None,
            "description": clean_wiki(mw.get("description", "")),
            "baseDamage": int(mw["baseDamage"]) if mw.get("baseDamage") else 0,
            "chargedDamage": int(mw["chargedDamage"]) if mw.get("chargedDamage") else 0,
            "specialAttack": clean_wiki(mw.get("specialAttack", "")),
            "specialDamage": mw.get("specialDamage", ""),
            "location": clean_wiki(mw.get("location", "")),
            "iconUrl": local_icon,
            "mysteriumTiers": mysteria
        })

    for s in spells_raw:
        stype = s.get("type", "")
        category = "LIGHT_SPELL" if "Light" in stype else "HEAVY_SPELL"
        item_id = f"s-{slugify(s['name'])}"
        pname = s.get("_pageName") or s["name"]
        wt = revisions.get(pname, "")
        mysteria = parse_mysteria(wt)
        img_file = file_map.get(pname)
        img_url = image_urls.get(img_file)
        local_icon = f"/images/items/{item_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ITEMS_IMAGE_DIR, f"{item_id}.png"))

        items.append({
            "id": item_id,
            "name": s["name"],
            "category": category,
            "element": s.get("element1") if s.get("element1") else None,
            "description": clean_wiki(s.get("description", "")),
            "charges": int(s["charges"]) if s.get("charges") else 1,
            "recharge": clean_wiki(s.get("recharge", "")),
            "location": clean_wiki(s.get("location", "")),
            "iconUrl": local_icon,
            "mysteriumTiers": mysteria
        })

    for m in magical_raw:
        mtype = m.get("type", "").upper()
        category = mtype if mtype in ["RELIC", "FETISH", "RING"] else "RELIC"
        item_id = f"m-{slugify(m['name'])}"
        pname = m.get("_pageName") or m["name"]
        wt = revisions.get(pname, "")
        mysteria = parse_mysteria(wt)
        img_file = file_map.get(pname)
        img_url = image_urls.get(img_file)
        local_icon = f"/images/items/{item_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ITEMS_IMAGE_DIR, f"{item_id}.png"))

        items.append({
            "id": item_id,
            "name": m["name"],
            "category": category,
            "element": m.get("element") if m.get("element") else None,
            "description": clean_wiki(m.get("description", "")),
            "location": clean_wiki(m.get("location", "")),
            "iconUrl": local_icon,
            "mysteriumTiers": mysteria
        })

    for b in beads_raw:
        item_id = f"b-{slugify(b['name'])}"
        pname = b.get("_pageName") or b["name"]
        wt = revisions.get(pname, "")
        mysteria = parse_mysteria(wt)
        reqs = parse_bead_requirements(b.get("requirement", ""))
        img_file = file_map.get(pname)
        img_url = image_urls.get(img_file)
        local_icon = f"/images/items/{item_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ITEMS_IMAGE_DIR, f"{item_id}.png"))

        items.append({
            "id": item_id,
            "name": b["name"],
            "category": "BEAD",
            "element": None,
            "description": clean_wiki(b.get("description", "")),
            "requirements": reqs,
            "location": clean_wiki(b.get("location", "")),
            "iconUrl": local_icon,
            "mysteriumTiers": mysteria
        })

    out_file = os.path.join(DATA_DIR, "items.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(items, f, indent=2, ensure_ascii=False)
    print(f"Scraped {len(items)} items -> {out_file}")

def map_arcana_element(name, ptypes, desc, effects):
    text = f"{' '.join(ptypes)} {name} {desc} {effects}".lower()
    if "fire element" in text or "burning" in text or "ignite" in text or "burn" in text:
        return "Fire"
    if "water element" in text or "freez" in text or "frost" in text or "ice" in text:
        return "Water"
    if "earth element" in text or "decay" in text:
        return "Earth"
    if "air element" in text or "shock" in text or "electr" in text or "lightning" in text:
        return "Air"
    return None

def scrape_arcana():
    print("Scraping Arcana from wiki.gg...")
    raw = fetch_cargo("Arcana", ["_pageName", "name", "prophecyTypes", "description", "effects"])
    file_titles = [f"File:{r['name']}.png" for r in raw]
    image_urls = fetch_images(file_titles)

    arcana_list = []
    for r in raw:
        card_id = f"arcana-{slugify(r['name'])}"
        raw_ptypes = r.get("prophecyTypes", "") or ""
        ptypes = [p.strip() for p in raw_ptypes.split(",") if p.strip()]
        desc = clean_wiki(r.get("description", ""))
        effects = clean_wiki(r.get("effects", ""))
        element = map_arcana_element(r["name"], ptypes, desc, effects)

        img_file = f"File:{r['name']}.png"
        img_url = image_urls.get(img_file)
        local_icon = f"/images/arcana/{card_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ARCANA_IMAGE_DIR, f"{card_id}.png"))

        arcana_list.append({
            "id": card_id,
            "name": r["name"],
            "description": desc,
            "effects": effects,
            "prophecyTypes": ptypes,
            "element": element,
            "iconUrl": local_icon
        })

    out_file = os.path.join(DATA_DIR, "arcana.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(arcana_list, f, indent=2, ensure_ascii=False)
    print(f"Scraped {len(arcana_list)} Arcana -> {out_file}")

def scrape_prophecies():
    print("Scraping Prophecies & Omens from wiki.gg...")
    prophecies_raw = fetch_cargo("Prophecies", ["_pageName", "name", "description", "omenName", "arcanaType", "location"])
    omens_raw = fetch_cargo("Omens", ["_pageName", "name", "effect"])

    omen_map = {}
    for o in omens_raw:
        omen_map[o["name"]] = clean_wiki(o.get("effect", ""))

    file_titles = [f"File:{p['name']}.png" for p in prophecies_raw]
    image_urls = fetch_images(file_titles)

    prophecies = []
    for p in prophecies_raw:
        prop_id = f"prophecy-{slugify(p['name'])}"
        omen_name = clean_wiki(p.get("omenName", ""))
        omen_effect = omen_map.get(omen_name, "")
        loc = clean_wiki(p.get("location", ""))
        arcana_type = clean_wiki(p.get("arcanaType", ""))

        img_file = f"File:{p['name']}.png"
        img_url = image_urls.get(img_file)
        local_icon = f"/images/prophecies/{prop_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(PROPHECIES_IMAGE_DIR, f"{prop_id}.png"))

        prophecies.append({
            "id": prop_id,
            "name": p["name"],
            "description": clean_wiki(p.get("description", "")),
            "arcanaType": arcana_type,
            "omenName": omen_name,
            "omenEffect": omen_effect,
            "location": loc,
            "iconUrl": local_icon
        })

    out_file = os.path.join(DATA_DIR, "prophecies.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(prophecies, f, indent=2, ensure_ascii=False)
    print(f"Scraped {len(prophecies)} Prophecies -> {out_file}")

def scrape_enemies():
    print("Scraping Enemies from wiki.gg...")
    enemies_raw = fetch_cargo("Enemy", [
        "_pageName", "name", "description", "rank", "gnosis", "health", "damage",
        "variants", "location", "fireResistance", "earthResistance", "waterResistance",
        "airResistance", "burnResistance", "decayResistance", "freezeResistance",
        "shockResistance", "stunResistance", "staggerResistance"
    ])

    file_titles = [f"File:{e['name']}.png" for e in enemies_raw]
    image_urls = fetch_images(file_titles)

    enemies = []
    for e in enemies_raw:
        enemy_id = f"enemy-{slugify(e['name'])}"
        img_file = f"File:{e['name']}.png"
        img_url = image_urls.get(img_file)
        local_icon = f"/images/enemies/{enemy_id}.webp"
        if img_url:
            download_image(img_url, os.path.join(ENEMIES_IMAGE_DIR, f"{enemy_id}.png"))

        enemies.append({
            "id": enemy_id,
            "name": e["name"],
            "description": clean_wiki(e.get("description", "")),
            "rank": clean_wiki(e.get("rank", "")) or "Minor",
            "gnosis": parse_int_or_none(e.get("gnosis")),
            "health": parse_int_or_none(e.get("health")),
            "damage": clean_wiki(e.get("damage", "")),
            "variants": clean_wiki(e.get("variants", "")),
            "locations": parse_locations(e.get("location", "")),
            "fireResistance": parse_int_or_none(e.get("fireResistance")),
            "earthResistance": parse_int_or_none(e.get("earthResistance")),
            "waterResistance": parse_int_or_none(e.get("waterResistance")),
            "airResistance": parse_int_or_none(e.get("airResistance")),
            "burnResistance": parse_int_or_none(e.get("burnResistance")),
            "decayResistance": parse_int_or_none(e.get("decayResistance")),
            "freezeResistance": parse_int_or_none(e.get("freezeResistance")),
            "shockResistance": parse_int_or_none(e.get("shockResistance")),
            "stunResistance": parse_int_or_none(e.get("stunResistance")),
            "staggerResistance": parse_int_or_none(e.get("staggerResistance")),
            "iconUrl": local_icon
        })

    out_file = os.path.join(DATA_DIR, "enemies.json")
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(enemies, f, indent=2, ensure_ascii=False)
    print(f"Scraped {len(enemies)} Enemies -> {out_file}")

def main():
    # Only scrape items if items.json does not exist
    if not os.path.exists(os.path.join(DATA_DIR, "items.json")):
        scrape_items()
    scrape_arcana()
    scrape_prophecies()
    scrape_enemies()

    # The wiki serves full-size PNGs; re-encode them to the WebP the iconUrl values point at.
    try:
        import optimize_images
        optimize_images.main()
    except ImportError:
        print("Warning: Pillow is not installed - run scripts/optimize_images.py manually")

if __name__ == "__main__":
    main()
