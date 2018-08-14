import Dependencies._

showCurrentGitBranch

git.useGitDescribe := true

lazy val commonSettings = Seq(
  organization := "org.hathitrust.htrc",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.11.12",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions"
  ),
  resolvers ++= Seq(
    "HTRC Repository" at "http://nexus.htrc.illinois.edu/content/groups/public",
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

lazy val ammoniteSettings = Seq(
  libraryDependencies += {
    val version = scalaBinaryVersion.value match {
      case "2.10" => "1.0.3"
      case _ ⇒ "1.1.2"
    }
    "com.lihaoyi" % "ammonite" % version % "test" cross CrossVersion.full
  },
  sourceGenerators in Test += Def.task {
    val file = (sourceManaged in Test).value / "amm.scala"
    IO.write(file, """object amm extends App { ammonite.Main.main(args) }""")
    Seq(file)
  }.taskValue,
  (fullClasspath in Test) ++= {
    (updateClassifiers in Test).value
      .configurations
      .find(_.configuration == Test.name)
      .get
      .modules
      .flatMap(_.artifacts)
      .collect { case (a, f) if a.classifier.contains("sources") => f }
  }
)

lazy val `feature-extractor` = (project in file(".")).
  enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging).
  settings(commonSettings).
  settings(ammoniteSettings).
  settings(spark("2.3.1")).
  //settings(spark_dev("2.3.1")).
  settings(
    name := "feature-extractor",
    description := "Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust " +
       "corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research",
    licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    libraryDependencies ++= Seq(
      "tdm"                           %% "feature-extractor"    % "2.1",
      "org.hathitrust.htrc"           %% "spark-utils"          % "1.1.0",
      "com.typesafe.play"             %% "play-json"            % "2.6.9"
        exclude("com.fasterxml.jackson.core", "jackson-databind")
        exclude("ch.qos.logback", "logback-classic"),
      "org.rogach"                    %% "scallop"              % "3.1.3",
      "com.gilt"                      %% "gfc-time"             % "0.0.7",
      "ch.qos.logback"                %  "logback-classic"      % "1.2.3",
      "org.codehaus.janino"           %  "janino"               % "3.0.8",
      "org.scalacheck"                %% "scalacheck"           % "1.14.0"      % Test,
      "org.scalatest"                 %% "scalatest"            % "3.0.5"       % Test
    )
  )
