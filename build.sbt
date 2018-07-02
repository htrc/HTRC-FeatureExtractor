import Dependencies._

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "org.hathitrust.htrc",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.11.12",
  scalacOptions ++= Seq("-feature", "-language:postfixOps", "-language:implicitConversions", "-target:jvm-1.8", "-Xexperimental"),
  resolvers ++= Seq(
    "I3 Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
    Resolver.mavenLocal
  ),
  packageOptions in (Compile, packageBin) += Package.ManifestAttributes(
    ("Git-Sha", git.gitHeadCommit.value.getOrElse("N/A")),
    ("Git-Branch", git.gitCurrentBranch.value),
    ("Git-Version", git.gitDescribedVersion.value.getOrElse("N/A")),
    ("Git-Dirty", git.gitUncommittedChanges.value.toString),
    ("Build-Date", new java.util.Date().toString)
  )
)

lazy val `feature-extractor` = (project in file(".")).
  enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging).
  settings(commonSettings: _*).
  //settings(spark("2.1.0"): _*).
  settings(spark_dev("2.3.1"): _*).
  settings(
    name := "feature-extractor",
    description := "Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust " +
       "corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research",
    licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    libraryDependencies ++= Seq(
      "org.hathitrust.htrc"           %  "pairtree-helper"      % "3.0",
      "edu.illinois.i3.scala"         %% "scala-opennlp"        % "0.6.4-SNAPSHOT",
      "org.hathitrust.htrc"           %% "running-headers"      % "0.6",
      "org.hathitrust.htrc"           %% "scala-utils"          % "2.0",
      "com.cybozu.labs"               %  "langdetect"           % "20140303",
      "org.rogach"                    %% "scallop"              % "3.1.2",
      "com.jsuereth"                  %% "scala-arm"            % "1.4",
      "com.typesafe.play"             %% "play-json"            % "2.5.4"
        exclude("com.fasterxml.jackson.core", "jackson-databind"),
      "com.gilt"                      %% "gfc-time"             % "0.0.5",
      "com.github.nscala-time"        %% "nscala-time"          % "2.12.0",
      "org.scalacheck"                %% "scalacheck"           % "1.12.5"      % Test,
      "org.scalatest"                 %% "scalatest"            % "2.2.6"       % Test
    )
  )
