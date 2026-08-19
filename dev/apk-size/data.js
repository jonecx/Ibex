window.BENCHMARK_DATA = {
  "lastUpdate": 1787116183507,
  "repoUrl": "https://github.com/jonecx/Ibex",
  "entries": {
    "Ibex APK Size": [
      {
        "commit": {
          "author": {
            "email": "jonecx@users.noreply.github.com",
            "name": "jonecx",
            "username": "jonecx"
          },
          "committer": {
            "email": "jonecx@users.noreply.github.com",
            "name": "jonecx",
            "username": "jonecx"
          },
          "distinct": true,
          "id": "032c702c9a176345d4e24bd1fb1f1997332c9e4b",
          "message": "fix: repair CI pipeline and breadcrumb accessibility\n\n- build: gate Sentry mapping/source uploads on SENTRY_AUTH_TOKEN so the\n  tokenless CI release build stops failing (APK Size job); drop the now\n  unneeded -x in apk-size.yml\n- a11y: stop the breadcrumb's current crumb from re-announcing the top-bar\n  title, so TalkBack reads each screen title once\n- test: realign the instrumented suite with the current app — media album\n  browsing in the fake repository, a home-grid testTag with scroll helpers\n  for off-screen remote tiles, sort/scroll tests target the file list, and\n  strict single-node title assertions after the a11y fix\n- benchmark: step into the seeded album before scrolling Images/Videos",
          "timestamp": "2026-08-18T21:28:19-07:00",
          "tree_id": "8208d540a2af7277bdc1112093dde1d6e2b95e5d",
          "url": "https://github.com/jonecx/Ibex/commit/032c702c9a176345d4e24bd1fb1f1997332c9e4b"
        },
        "date": 1787114003889,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "APK / total",
            "value": 13469805,
            "unit": "bytes"
          },
          {
            "name": "APK / dex (code)",
            "value": 8020028,
            "unit": "bytes"
          },
          {
            "name": "APK / resources",
            "value": 853108,
            "unit": "bytes"
          },
          {
            "name": "APK / native libs",
            "value": 3058872,
            "unit": "bytes"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jonecx@users.noreply.github.com",
            "name": "jonecx",
            "username": "jonecx"
          },
          "committer": {
            "email": "jonecx@users.noreply.github.com",
            "name": "jonecx",
            "username": "jonecx"
          },
          "distinct": true,
          "id": "6aa25d71b165f0c94932b42df31df3e078b6af58",
          "message": "fix(benchmark): re-find scrollable each fling to avoid StaleObjectException\n\nAfter the first fling the lazy grid re-lays-out, so reusing the UiObject2 for\nthe second fling threw StaleObjectException and failed the scroll benchmarks.\nLook the scrollable up fresh per gesture, with one retry on staleness.",
          "timestamp": "2026-08-18T22:02:31-07:00",
          "tree_id": "35f73cd02526548325cc3e54ef0d8f30a4744d35",
          "url": "https://github.com/jonecx/Ibex/commit/6aa25d71b165f0c94932b42df31df3e078b6af58"
        },
        "date": 1787116182761,
        "tool": "customSmallerIsBetter",
        "benches": [
          {
            "name": "APK / total",
            "value": 13469805,
            "unit": "bytes"
          },
          {
            "name": "APK / dex (code)",
            "value": 8020028,
            "unit": "bytes"
          },
          {
            "name": "APK / resources",
            "value": 853108,
            "unit": "bytes"
          },
          {
            "name": "APK / native libs",
            "value": 3058872,
            "unit": "bytes"
          }
        ]
      }
    ]
  }
}