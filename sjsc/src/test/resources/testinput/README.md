Test Inputs
===========

Benchmark Suites
----------------
We've checked in the unmodified source to several benchmark suites:

1. Mozilla's Kraken suite: https://wiki.mozilla.org/Kraken http://hg.mozilla.org/projects/kraken/
2. Apple's Sunspider suite: http://www.webkit.org/perf/sunspider/sunspider.html https://github.com/WebKit/webkit/tree/master/PerformanceTests/SunSpider
3. Google's Octane suite: https://developers.google.com/octane/benchmark https://code.google.com/p/octane-benchmark

Many of the existing tests in other directories originated in these suites, but we need to be able
to compare against existing browsers and other JavaScript engines on these suites.
