"""Reference fronts of the MaF problems with 3 objectives (MaF01-MaF15 except MaF08, which exists).

Evolver and jMetal have the reference fronts of the MaF benchmark (Cheng et al., "A benchmark test
suite for evolutionary many-objective optimization", Complex & Intelligent Systems 3, 2017,
doi:10.1007/s40747-017-0039-7) for 5, 10 and 15 objectives, as in the CEC 2018 competition. This
script writes the ones for 3 objectives, ``MaFxx.3D.csv``, with the method that PlatEMO uses for the
benchmark (the ``GetOptimum`` method of each problem in PlatEMO, commit d25e65d1; Y. Tian et al.,
"Sampling reference points on the Pareto fronts of benchmark multi-objective optimization problems",
CEC 2018): points spread uniformly on the simplex (normal-boundary intersection with about 10000
points) mapped to the front of each problem. jMetal's MaF objective functions match PlatEMO's
(checked to machine precision), so the fronts are those of the problems of jMetal.

    python scripts/maf_reference_fronts.py --output-dir resources/referenceFronts

``MaFReferenceFrontsIT`` (src/test) checks them against the problems of jMetal: solutions of
the Pareto set of each problem must lie on its front.
"""

import argparse
from math import comb, cos, pi, sin
from pathlib import Path

import numpy as np

M = 3
N = 10000


# ---- PlatEMO's UniformPoint ----------------------------------------------------------------------
def _simplex_layer(h: int, m: int) -> np.ndarray:
    """The points of the simplex lattice with h divisions (Das and Dennis), as in PlatEMO's NBI."""
    from itertools import combinations

    c = np.array(list(combinations(range(1, h + m), m - 1))) - np.arange(m - 1) - 1
    return (np.hstack([c, np.full((len(c), 1), h)]) - np.hstack([np.zeros((len(c), 1)), c])) / h


def uniform_point(n: int, m: int) -> np.ndarray:
    """PlatEMO's UniformPoint(N, M) with the NBI method (two layers when there are few divisions)."""
    h1 = 1
    while comb(h1 + m, m - 1) <= n:
        h1 += 1
    w = _simplex_layer(h1, m)
    if h1 < m:
        h2 = 0
        while comb(h1 + m - 1, m - 1) + comb(h2 + m, m - 1) <= n:
            h2 += 1
        if h2 > 0:
            w = np.vstack([w, _simplex_layer(h2, m) / 2 + 1 / (2 * m)])
    return np.maximum(w, 1e-6)


def grid_point(n: int, m: int) -> np.ndarray:
    """PlatEMO's UniformPoint(N, M, 'grid'): a regular grid of the unit hypercube."""
    gap = np.linspace(0, 1, int(np.ceil(n ** (1 / m))))
    return np.stack(np.meshgrid(*[gap] * m, indexing="ij"), axis=-1).reshape(-1, m, order="F")


def non_dominated(points: np.ndarray) -> np.ndarray:
    """The non-dominated points (minimization), as PlatEMO's NDSort(R, 1) == 1."""
    keep = np.ones(len(points), dtype=bool)
    for start in range(0, len(points), 500):
        block = points[start:start + 500]
        dominated = ((points[None, :, :] <= block[:, None, :]).all(axis=2)
                     & (points[None, :, :] < block[:, None, :]).any(axis=2)).any(axis=1)
        keep[start:start + 500] = ~dominated
    return points[keep]


def _normalize_rows(r: np.ndarray) -> np.ndarray:
    return r / np.sqrt((r ** 2).sum(axis=1, keepdims=True))


# ---- the fronts ---------------------------------------------------------------------------------
def maf01() -> np.ndarray:
    return 1 - uniform_point(N, M)


def maf02() -> np.ndarray:
    r = uniform_point(N, M)
    c = np.zeros((len(r), M - 1))
    for i in range(len(r)):
        for j in range(2, M + 1):
            temp = r[i, j - 1] / r[i, 0] * np.prod(c[i, M - j + 1:M - 1])
            c[i, M - j] = np.sqrt(1 / (1 + temp ** 2))
    if M > 5:
        c = c * (cos(pi / 8) - cos(3 * pi / 8)) + cos(3 * pi / 8)
    else:
        c = c[~((c < cos(3 * pi / 8)) | (c > cos(pi / 8))).any(axis=1)]
    head = np.fliplr(np.cumprod(np.hstack([np.ones((len(c), 1)), c[:, :M - 1]]), axis=1))
    tail = np.hstack([np.ones((len(c), 1)), np.sqrt(1 - c[:, ::-1] ** 2)])
    return head * tail


def maf03() -> np.ndarray:
    r = uniform_point(N, M) ** 2
    temp = np.sqrt(r[:, :-1]).sum(axis=1) + r[:, -1]
    return r / np.hstack([np.repeat((temp ** 2)[:, None], M - 1, axis=1), temp[:, None]])


def maf04() -> np.ndarray:
    return (1 - _normalize_rows(uniform_point(N, M))) * 2.0 ** np.arange(1, M + 1)


def maf05() -> np.ndarray:
    return _normalize_rows(uniform_point(N, M)) * 2.0 ** np.arange(M, 0, -1)


def maf06() -> np.ndarray:
    i = 2
    r = _normalize_rows(uniform_point(N, i))
    r = np.hstack([np.repeat(r[:, :1], M - i, axis=1), r])
    exponents = np.maximum(np.array([M - i] + list(range(M - i, 2 - i - 1, -1))), 0)
    return r / np.sqrt(2) ** exponents


def maf07() -> np.ndarray:
    interval = [0, 0.251412, 0.631627, 0.859401]
    median = (interval[1] - interval[0]) / (interval[3] - interval[2] + interval[1] - interval[0])
    x = grid_point(N, M - 1)
    low = x <= median
    x = np.where(low, x * (interval[1] - interval[0]) / median + interval[0],
                 (x - median) * (interval[3] - interval[2]) / (1 - median) + interval[2])
    return np.hstack([x, (2 * (M - (x / 2 * (1 + np.sin(3 * pi * x))).sum(axis=1)))[:, None]])


def _maf09_points() -> np.ndarray:
    angles = pi / 2 - np.arange(1, M + 1) * 2 * pi / M
    return np.stack([np.cos(angles), np.sin(angles)], axis=1)


def _maf09_objectives(x: np.ndarray, points: np.ndarray) -> np.ndarray:
    f = np.zeros((len(x), M))
    for m in range(M):
        (x1, y1), (x2, y2) = points[m], points[(m + 1) % M]
        f[:, m] = (np.abs((x1 - x[:, 0]) * (y2 - x[:, 1]) - (x2 - x[:, 0]) * (y1 - x[:, 1]))
                   / np.hypot(x1 - x2, y1 - y2))
    return f


def _inside_polygon(x: np.ndarray, points: np.ndarray) -> np.ndarray:
    """Points inside (or on the border of) a convex polygon given counter- or clockwise."""
    sides = []
    for m in range(len(points)):
        (x1, y1), (x2, y2) = points[m], points[(m + 1) % len(points)]
        sides.append((x2 - x1) * (x[:, 1] - y1) - (y2 - y1) * (x[:, 0] - x1))
    sides = np.array(sides)
    return (sides >= -1e-12).all(axis=0) | (sides <= 1e-12).all(axis=0)


def maf09() -> np.ndarray:
    points = _maf09_points()
    side = np.linspace(-1, 1, int(np.ceil(np.sqrt(N))))
    gx, gy = np.meshgrid(side, side, indexing="ij")
    x = np.stack([gx.ravel(order="F"), gy.ravel(order="F")], axis=1)
    return _maf09_objectives(x[_inside_polygon(x, points)], points)


def _convex(x: np.ndarray) -> np.ndarray:
    head = np.fliplr(np.cumprod(np.hstack([np.ones((len(x), 1)), 1 - np.cos(x[:, :-1] * pi / 2)]),
                                axis=1))
    tail = np.hstack([np.ones((len(x), 1)), 1 - np.sin(x[:, -2::-1] * pi / 2)])
    return head * tail


def _wfg_position(r: np.ndarray, shape) -> np.ndarray:
    """The position variables of the points of a convex WFG-like front (MaF10, MaF11), found as
    PlatEMO does: the angles from the simplex, and the first one by searching a grid of 10001 values
    for the shape of the last objective."""
    c = np.ones((len(r), M))
    for i in range(len(r)):
        for j in range(2, M + 1):
            temp = r[i, j - 1] / r[i, 0] * np.prod(1 - c[i, M - j + 1:M - 1])
            c[i, M - j] = (temp ** 2 - temp + np.sqrt(2 * temp)) / (temp ** 2 + 1)
    x = np.arccos(c) * 2 / pi
    temp = (1 - np.sin(pi / 2 * x[:, 1])) * r[:, M - 1] / r[:, M - 2]
    a = np.arange(0, 10001) / 10000
    for start in range(0, len(x), 500):
        t = temp[start:start + 500, None]
        error = np.abs(t * (1 - np.cos(pi / 2 * a)) - 1 + shape(a))
        best = np.argsort(error, axis=1, kind="stable")[:, :10]
        x[start:start + 500, 0] = a[best.min(axis=1)]
    return x


def maf10() -> np.ndarray:
    r = uniform_point(N, M)
    x = _wfg_position(r, lambda a: a + np.cos(10 * pi * a + pi / 2) / 10 / pi)
    front = _convex(x)
    front[:, M - 1] = 1 - x[:, 0] - np.cos(10 * pi * x[:, 0] + pi / 2) / 10 / pi
    return front * np.arange(2, 2 * M + 1, 2)


def maf11() -> np.ndarray:
    r = uniform_point(N, M)
    x = _wfg_position(r, lambda a: a * np.cos(5 * pi * a) ** 2)
    front = _convex(x)
    front[:, M - 1] = 1 - x[:, 0] * np.cos(5 * pi * x[:, 0]) ** 2
    return non_dominated(front) * np.arange(2, 2 * M + 1, 2)


def maf12() -> np.ndarray:
    return _normalize_rows(uniform_point(N, M)) * np.arange(2, 2 * M + 1, 2)


def maf13() -> np.ndarray:
    r = _normalize_rows(uniform_point(N, 3))
    extra = np.repeat((r[:, 0] ** 2 + r[:, 1] ** 10 + r[:, 2] ** 10)[:, None], M - 3, axis=1)
    return np.hstack([r, extra])


def maf14() -> np.ndarray:
    return uniform_point(N, M)


def maf15() -> np.ndarray:
    return 1 - _normalize_rows(uniform_point(N, M))


FRONTS = {
    "MaF01": maf01, "MaF02": maf02, "MaF03": maf03, "MaF04": maf04, "MaF05": maf05,
    "MaF06": maf06, "MaF07": maf07, "MaF09": maf09, "MaF10": maf10, "MaF11": maf11,
    "MaF12": maf12, "MaF13": maf13, "MaF14": maf14, "MaF15": maf15,
}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--output-dir", type=Path, default=Path("resources/referenceFronts"))
    parser.add_argument("--problems", default=",".join(FRONTS), help="comma-separated names")
    args = parser.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for name in args.problems.split(","):
        front = FRONTS[name]()
        path = args.output_dir / f"{name}.{M}D.csv"
        np.savetxt(path, front, delimiter=",", fmt="%.16f")
        print(f"{path}: {len(front)} points")


if __name__ == "__main__":
    main()
