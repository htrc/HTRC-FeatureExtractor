[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/htrc/HTRC-FeatureExtractor/ci.yml?branch=main)](https://github.com/htrc/HTRC-FeatureExtractor/actions/workflows/ci.yml)
[![codecov](https://codecov.io/github/htrc/HTRC-FeatureExtractor/branch/main/graph/badge.svg?token=Y8PXGBZO01)](https://codecov.io/github/htrc/HTRC-FeatureExtractor)
[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/htrc/HTRC-FeatureExtractor?include_prereleases&sort=semver)](https://github.com/htrc/HTRC-FeatureExtractor/releases/latest)

# HTRC-FeatureExtractor
Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust
corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research.

# Build
* To generate a package that can be invoked via a shell script, run:  
  `sbt stage`  
  then find the result in `target/universal/stage/` folder.
* To generate a distributable ZIP package, run:  
  `sbt dist`  
  then find the result in `target/universal/` folder.
  
# Run
```
extract-features
  -l, --log-level  <LEVEL>    (Optional) The application log level; one of INFO,
                              DEBUG, OFF (default = INFO)
  -c, --num-cores  <N>        (Optional) The number of CPU cores to use (if not
                              specified, uses all available cores)
  -n, --num-partitions  <N>   (Optional) The number of partitions to split the
                              input set of HT IDs into, for increased
                              parallelism
  -o, --output  <DIR>         Write the output to DIR (should not exist, or be
                              empty)
  -p, --pairtree  <DIR>       The path to the paitree root hierarchy to process
  -s, --save-as-seq           (Optional) Saves the EF files as Hadoop sequence
                              files
      --spark-log  <FILE>     (Optional) Where to write logging output from
                              Spark to
  -h, --help                  Show help message
  -v, --version               Show version of this program

 trailing arguments:
  htids (not required)   The file containing the HT IDs to be searched (if not
                         provided, will read from stdin)
```
