# HTRC-FeatureExtractor
Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust
corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research.

# Build
* To generate a "fat" executable JAR, run:  
  `sbt assembly`  
  then look for it in `target/scala-2.11/` folder.

  *Note:* you can run the JAR via the usual: `java -jar JARFILE`

* To generate a package that can be invoked via a shell script, run:  
  `sbt stage`  
  then find the result in `target/universal/stage/` folder.
  
# Run
```
feature-extractor
HathiTrust Research Center
  -c, --compress                Compress the output
  -i, --indent                  Indent the output
  -l, --lang-dir  <DIR>         The path to the language profiles
  -m, --nlp-models-dir  <DIR>   The path to the NLP models
  -n, --num-partitions  <N>     The number of partitions to split the input set
                                of HT IDs into, for increased parallelism
  -o, --output  <DIR>           Write the output to DIR (should not exist, or be empty)
  -p, --pairtree  <DIR>         The path to the paitree root hierarchy to
                                process
      --help                    Show help message
      --version                 Show version of this program

 trailing arguments:
  htids (not required)   The file containing the HT IDs to be searched (if not
                         provided, will read from stdin)
```
