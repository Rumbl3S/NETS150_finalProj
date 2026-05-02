#!/usr/bin/env python3
"""
Download the full MovieLens 20M dataset from Kaggle (grouplens/movielens-20m-dataset)
via kagglehub, then copy movie + rating tables into data/movielens-20m/ as movies.csv and ratings.csv
(standard MovieLens names expected by the Java loader).

Prerequisites:
  python3 -m pip install kagglehub   (use -m pip so it matches `python3`)
  Kaggle API credentials configured if required by your kagglehub setup.

Usage (from project root):
  python3 scripts/download_movielens.py

Optional environment (ratings are huge; default is a fast subset):
  MOVIELENS_RATINGS_MAX_LINES   default 1000000; use 0 for full ~20M rows
  MOVIELENS_RATINGS_ORDER       head (default) or tail — which slice of the file to keep
"""
from __future__ import annotations

import os
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
            print("Could not find movie/movies or rating/ratings CSV.", file=sys.stderr)
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
        sample_ratings_to_output(ratings, out / "ratings.csv")
    finally:
        shutil.rmtree(staging, ignore_errors=True)

    print("Copied to:", out.resolve())
    print("  movies.csv  <-", movies)
    print("  ratings.csv <-", ratings, "(see MOVIELENS_RATINGS_MAX_LINES / ORDER if subset)")
    print("Run the Java app from the project root (default data/ picks up movielens-20m).")
    return 0


def find_movies_ratings_csv(root: Path, staging: Path) -> tuple[Path | None, Path | None]:
    """Locate movie + rating tables (GroupLens or Kaggle singular names), including inside archives."""
    movies, ratings = _find_csvs_in_tree(root)
    if movies is not None and ratings is not None:
        return movies, ratings

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


def _pick_movie_file(base: Path) -> Path | None:
    matches = [p for p in base.rglob("*") if p.is_file() and p.name.lower() in ("movies.csv", "movie.csv")]
    if not matches:
        return None
    for p in matches:
        if p.name.lower() == "movies.csv":
            return p
    return matches[0]


def _pick_rating_file(base: Path) -> Path | None:
    matches = [p for p in base.rglob("*") if p.is_file() and p.name.lower() in ("ratings.csv", "rating.csv")]
    if not matches:
        return None
    for p in matches:
        if p.name.lower() == "ratings.csv":
            return p
    return matches[0]


def _find_csvs_in_tree(base: Path) -> tuple[Path | None, Path | None]:
    return _pick_movie_file(base), _pick_rating_file(base)


def _zip_pick_member(names: list[str], preferred: str, fallback: str) -> str | None:
    pref = [n for n in names if Path(n).name.lower() == preferred]
    if pref:
        return pref[0]
    fb = [n for n in names if Path(n).name.lower() == fallback]
    return fb[0] if fb else None


def _extract_csvs_from_zip(zp: Path, staging: Path) -> tuple[Path | None, Path | None]:
    try:
        with zipfile.ZipFile(zp, "r") as zf:
            names = zf.namelist()
            mm = _zip_pick_member(names, "movies.csv", "movie.csv")
            rm = _zip_pick_member(names, "ratings.csv", "rating.csv")
            if not mm or not rm:
                return None, None
            zf.extract(mm, staging)
            zf.extract(rm, staging)
            return _find_csvs_in_tree(staging)
    except (zipfile.BadZipFile, OSError):
        return None, None


def _extract_csvs_from_tar(tp: Path, staging: Path) -> tuple[Path | None, Path | None]:
    try:
        with tarfile.open(tp, "r:*") as tf:
            names = [m.name for m in tf.getmembers() if m.isfile()]
            mm = _zip_pick_member(names, "movies.csv", "movie.csv")
            rm = _zip_pick_member(names, "ratings.csv", "rating.csv")
            if not mm or not rm:
                return None, None
            if sys.version_info >= (3, 12):
                tf.extract(mm, staging, filter="data")
                tf.extract(rm, staging, filter="data")
            else:
                tf.extract(mm, staging)
                tf.extract(rm, staging)
            return _find_csvs_in_tree(staging)
    except (tarfile.TarError, OSError):
        return None, None


def sample_ratings_to_output(src: Path, dst: Path) -> None:
    raw = os.environ.get("MOVIELENS_RATINGS_MAX_LINES", "1000000").strip()
    max_lines = int(raw) if raw else 1_000_000
    order = os.environ.get("MOVIELENS_RATINGS_ORDER", "head").strip().lower()
    if max_lines <= 0:
        shutil.copy2(src, dst)
        print("Ratings: copied full file (MOVIELENS_RATINGS_MAX_LINES<=0).")
        return
    if order == "tail":
        write_ratings_tail(src, dst, max_lines)
        print(f"Ratings: wrote last {max_lines:,} data rows (+ header) -> ratings.csv")
    else:
        write_ratings_head(src, dst, max_lines)
        print(f"Ratings: wrote first {max_lines:,} data rows (+ header) -> ratings.csv")


def write_ratings_head(src: Path, dst: Path, max_data_lines: int) -> None:
    with open(src, "r", encoding="utf-8", errors="replace") as inf, open(
        dst, "w", encoding="utf-8", newline=""
    ) as outf:
        header = inf.readline()
        if not header:
            raise OSError("empty ratings source")
        outf.write(header)
        n = 0
        for line in inf:
            if n >= max_data_lines:
                break
            outf.write(line)
            n += 1


def write_ratings_tail(src: Path, dst: Path, max_data_lines: int) -> None:
    with open(src, "r", encoding="utf-8", errors="replace") as f:
        header = f.readline()
    if not header:
        raise OSError("empty ratings source")
    block = 8 * 1024 * 1024
    chunk = b""
    with open(src, "rb") as bf:
        bf.seek(0, 2)
        size = bf.tell()
        pos = size
        while pos > 0:
            step = min(block, pos)
            pos -= step
            bf.seek(pos)
            chunk = bf.read(step) + chunk
            if chunk.count(b"\n") >= max_data_lines + 2:
                break
    first_nl = chunk.find(b"\n")
    if first_nl >= 0:
        chunk = chunk[first_nl + 1 :]
    text = chunk.decode("utf-8", errors="replace")
    lines = text.splitlines()
    tail = lines[-max_data_lines:] if len(lines) > max_data_lines else lines
    with open(dst, "w", encoding="utf-8", newline="") as outf:
        outf.write(header)
        for line in tail:
            outf.write(line + "\n")


if __name__ == "__main__":
    raise SystemExit(main())
