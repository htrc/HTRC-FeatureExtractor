import sbt.Keys._
import sbt._

object Dependencies {

  def spark(version: String): Seq[Def.Setting[Seq[ModuleID]]] = Seq(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core"      % version % Provided
        exclude("org.slf4j", "slf4j-log4j12"),
      "org.apache.spark" %% "spark-streaming" % version % Provided
        exclude("org.slf4j", "slf4j-log4j12"),
      "org.apache.spark" %% "spark-sql"       % version % Provided
        exclude("org.slf4j", "slf4j-log4j12")
    )
  )

  def spark_dev(version: String): Seq[Def.Setting[Seq[ModuleID]]] = Seq(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core"      % version
        exclude("org.slf4j", "slf4j-log4j12"),
      "org.apache.spark" %% "spark-streaming" % version
        exclude("org.slf4j", "slf4j-log4j12"),
      "org.apache.spark" %% "spark-sql"       % version
        exclude("org.slf4j", "slf4j-log4j12")
    )
  )

}
