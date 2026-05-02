#!/usr/bin/env python3
"""
Download the full MovieLens 20M dataset from Kaggle (grouplens/movielens-20m-dataset)
via kagglehub, then copy movies.csv and ratings.csv into this repo under data/movielens-20m/.

Prerequisites:
  python3 -m pip install kagglehub   (use -m pip so it matches `python3`)
  Kaggle API credentials configured if required by your kagglehub setup.

Usage (from project root):
  python3 scripts/download_movielens.py
"""
from __future__ import annotations

import shutil
import sys
from pathlib import Path


def main() -> int:
    try:
        import kagglehub
    except ImportError:
        print(
            "Cannot import kagglehub in this Python interpreter:\n  "
            + sys.executable
            + "\n\n"
            "Your `pip` and `python3` are often different installations. Install into THIS interpreter:\n"
            "  python3 -m pip install -r scripts/requirements-movielens.txt\n"
            "or:  python3 -m pip install kagglehub\n",
            file=sys.stderr,
        )
        return 1

    print("Downloading grouplens/movielens-20m-dataset (this is large; may take a while)...")
    root = Path(kagglehub.dataset_download("grouplens/movielens-20m-dataset"))
    print("Download root:", root)

    movies = next(root.rglob("movies.csv"), None)
    ratings = next(root.rglob("ratings.csv"), None)
    if movies is None or ratings is None:
        print("Could not find movies.csv or ratings.csv under", root, file=sys.stderr)
        return 1

    project_root = Path(__file__).resolve().parents[1]
    out = project_root / "data" / "movielens-20m"
    out.mkdir(parents=True, exist_ok=True)

    shutil.copy2(movies, out / "movies.csv")
    shutil.copy2(ratings, out / "ratings.csv")

    print("Copied to:", out.resolve())
    print("Run the Java app from the project root (default data/ picks up movielens-20m).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
