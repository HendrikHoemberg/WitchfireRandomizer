import json, sys, os

targets = [
    ("src/main/resources/data/arcana.json", 100),
    ("src/main/resources/data/prophecies.json", 15),
    ("src/main/resources/data/enemies.json", 50),
]

for fpath, min_count in targets:
    if not os.path.exists(fpath):
        print(f"FAIL: {fpath} does not exist")
        sys.exit(1)
    try:
        with open(fpath, "r", encoding="utf-8") as f:
            data = json.load(f)
            if len(data) < min_count:
                print(f"FAIL: {fpath} has {len(data)} items, expected >= {min_count}")
                sys.exit(1)
            print(f"PASS: {fpath} loaded {len(data)} items")
    except Exception as e:
        print(f"FAIL: {fpath} - {e}")
        sys.exit(1)

print("ALL DATASETS VALID!")
