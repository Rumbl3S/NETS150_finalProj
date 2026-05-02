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
import tarfile
import zipfile
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

    project_root = Path(__file__).resolve().parents[1]
    out = project_root / "data" / "movielens-20m"
    out.mkdir(parents=True, exist_ok=True)
    staging = out / ".extract_staging"
    if staging.exists():
        shutil.rmtree(staging, ignore_errors=True)
    staging.mkdir(parents=True, exist_ok=True)

    try:
        movies, ratings = find_movies_ratings_csv(root, staging)
        if movies is None or ratings is None:
            print("Could not find movies.csv / ratings.csv.", file=sys.stderr)
            print("Top-level entries under", root, ":", file=sys.stderr)
            try:
                for p in sorted(root.iterdir()):
                    print(" ", p.name, "(dir)" if p.is_dir() else f"(file {p.stat().st_size} B)", file=sys.stderr)
            except OSError as e:
                print(" ", e, file=sys.stderr)
            print("Sample files (recursive, first 50):", file=sys.stderr)
            n = 0
            for p in sorted(root.rglob("*")):
                if p.is_file():
                    print(" ", p.relative_to(root), file=sys.stderr)
                    n += 1
                    if n >= 50:
                        break
            return 1

        shutil.copy2(movies, out / "movies.csv")
        shutil.copy2(ratings, out / "ratings.csv")
    finally:
        shutil.rmtree(staging, ignore_errors=True)

    print("Copied to:", out.resolve())
    print("  movies <-", movies)
    print("  ratings <-", ratings)
    print("Run the Java app from the project root (default data/ picks up movielens-20m).")
    return 0


def find_movies_ratings_csv(root: Path, staging: Path) -> tuple[Path | None, Path | None]:
    """Locate movies.csv and ratings.csv (case-insensitive), including inside nested archives."""
    movies, ratings = _find_csvs_in_tree(root)
    if movies is not None and ratings is not None:
        return movies, ratings

    # kagglehub may leave a single archive (sometimes named *.archive) instead of loose CSVs.
    candidates = sorted(root.rglob("*"), key=lambda p: (-p.stat().st_size if p.is_file() else 0))
    for p in candidates:
        if not p.is_file():
            continue
        if zipfile.is_zipfile(p):
            shutil.rmtree(staging, ignore_errors=True)
            staging.mkdir(parents=True, exist_ok=True)
            found = _extract_csvs_from_zip(p, staging)
            if found[0] is not None and found[1] is not None:
                return found
        if tarfile.is_tarfile(p):
            shutil.rmtree(staging, ignore_errors=True)
            staging.mkdir(parents=True, exist_ok=True)
            found = _extract_csvs_from_tar(p, staging)
            if found[0] is not None and found[1] is not None:
                return found

    return movies, ratings


def _find_csvs_in_tree(base: Path) -> tuple[Path | None, Path | None]:
    movies = ratings = None
    for path in base.rglob("*"):
        if not path.is_file():
            continue
        name = path.name.lower()
        if name == "movies.csv":
            movies = path
        elif name == "ratings.csv":
            ratings = path
    return movies, ratings


def _extract_csvs_from_zip(zp: Path, staging: Path) -> tuple[Path | None, Path | None]:
    try:
        with zipfile.ZipFile(zp, "r") as zf:
            names = zf.namelist()
            movie_m = [n for n in names if Path(n).name.lower() == "movies.csv"]
            rating_m = [n for n in names if Path(n).name.lower() == "ratings.csv"]
            if not movie_m or not rating_m:
                return None, None
            zf.extract(movie_m[0], staging)
            zf.extract(rating_m[0], staging)
            return _find_csvs_in_tree(staging)
    except (zipfile.BadZipFile, OSError):
        return None, None


def _extract_csvs_from_tar(tp: Path, staging: Path) -> tuple[Path | None, Path | None]:
    try:
        with tarfile.open(tp, "r:*") as tf:
            members = [m for m in tf.getmembers() if m.isfile()]
            names = [m.name for m in members]
            movie_m = [n for n in names if Path(n).name.lower() == "movies.csv"]
            rating_m = [n for n in names if Path(n).name.lower() == "ratings.csv"]
            if not movie_m or not rating_m:
                return None, None
            m0, r0 = movie_m[0], rating_m[0]
            if sys.version_info >= (3, 12):
                tf.extract(m0, staging, filter="data")
                tf.extract(r0, staging, filter="data")
            else:
                tf.extract(m0, staging)
                tf.extract(r0, staging)
            return _find_csvs_in_tree(staging)
    except (tarfile.TarError, OSError):
        return None, None


if __name__ == "__main__":
    raise SystemExit(main())
