import Dependencies._

showCurrentGitBranch

inThisBuild(Seq(
  organization := "org.hathitrust.htrc",
  organizationName := "HathiTrust Research Center",
  organizationHomepage := Some(url("https://www.hathitrust.org/htrc")),
  scalaVersion := "2.13.10",
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions"
  ),
  resolvers ++= Seq(
    Resolver.mavenLocal,
    "HTRC Nexus Repository" at "https://nexus.htrc.illinois.edu/repository/maven-public"
  ),
  externalResolvers := Resolver.combineDefaultResolvers(resolvers.value.toVector, mavenCentral = false),
  Compile / packageBin / packageOptions += Package.ManifestAttributes(
    ("Git-Sha", git.gitHeadCommit.value.getOrElse("N/A")),
    ("Git-Branch", git.gitCurrentBranch.value),
    ("Git-Version", git.gitDescribedVersion.value.getOrElse("N/A")),
    ("Git-Dirty", git.gitUncommittedChanges.value.toString),
    ("Build-Date", new java.util.Date().toString)
  ),
  versionScheme := Some("semver-spec"),
  credentials += Credentials(
    "Sonatype Nexus Repository Manager", // realm
    "nexus.htrc.illinois.edu", // host
    "drhtrc", // user
    sys.env.getOrElse("HTRC_NEXUS_DRHTRC_PWD", "abc123") // password
  )
))

lazy val publishSettings = Seq(
  publishTo := Some("GitHub Package Registry" at "https://maven.pkg.github.com/htrc/HTRC-FeatureExtractor"),
  // force to run 'test' before 'package' and 'publish' tasks
  publish := (publish dependsOn Test / test).value,
  Keys.`package` := (Compile / Keys.`package` dependsOn Test / test).value,
  credentials += Credentials(
    "GitHub Package Registry", // realm
    "maven.pkg.github.com", // host
    "htrc", // user
    sys.env.getOrElse("GITHUB_TOKEN", "abc123") // password
  )
)

lazy val ammoniteSettings = Seq(
  libraryDependencies +=
    {
      val version = scalaBinaryVersion.value match {
        case "2.10" => "1.0.3"
        case "2.11" => "1.6.7"
        case _ ⇒  "2.5.6"
      }
      "com.lihaoyi" % "ammonite" % version % Test cross CrossVersion.full
    },
  Test / sourceGenerators += Def.task {
    val file = (Test / sourceManaged).value / "amm.scala"
    IO.write(file, """object amm extends App { ammonite.AmmoniteMain.main(args) }""")
    Seq(file)
  }.taskValue,
  connectInput := true,
  outputStrategy := Some(StdoutOutput)
)

lazy val `extract-features` = (project in file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt, JavaAppPackaging)
  .settings(ammoniteSettings)
//  .settings(spark("3.3.1"))
  .settings(spark_dev("3.3.1"))
  .settings(
    name := "extract-features",
    description := "Extracts a set of features (such as ngram counts, POS tags, etc.) from the HathiTrust " +
       "corpus for aiding in conducting 'distant-reading' (aka non-consumptive) research",
    licenses += "Apache2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    libraryDependencies ++= Seq(
      "org.hathitrust.htrc"           %% "feature-extractor"        % "3.1.0",
      "org.hathitrust.htrc"           %% "data-model"               % "2.14.0",
      "org.hathitrust.htrc"           %% "spark-utils"              % "1.5.0",
      "com.typesafe.play"             %% "play-json"                % "2.9.4",
      "org.rogach"                    %% "scallop"                  % "4.1.0",
      "com.github.nscala-time"        %% "nscala-time"              % "2.32.0",
      "ch.qos.logback"                %  "logback-classic"          % "1.4.5",
      "org.codehaus.janino"           %  "janino"                   % "3.0.16",  // 3.1.x causes java.lang.ClassNotFoundException: org.codehaus.janino.InternalCompilerException
      "org.scalacheck"                %% "scalacheck"               % "1.17.0"  % Test,
      "org.scalatest"                 %% "scalatest"                % "3.2.15"  % Test,
      "org.scalatestplus"             %% "scalacheck-1-15"          % "3.2.11.0" % Test
    ),
    Test / parallelExecution := false,
    Test / fork := true
  )
