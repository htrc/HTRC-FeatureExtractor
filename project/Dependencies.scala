import sbt.Keys.*
import sbt.*

object Dependencies {

  def spark(version: String): Seq[Def.Setting[Seq[ModuleID]]] = Seq(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core"      % version % Provided,
      "org.apache.spark" %% "spark-streaming" % version % Provided,
      "org.apache.spark" %% "spark-sql"       % version % Provided
    )
  )

  def spark_dev(version: String): Seq[Def.Setting[Seq[ModuleID]]] = Seq(
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core"      % version
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl"),
      "org.apache.spark" %% "spark-streaming" % version
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl"),
      "org.apache.spark" %% "spark-sql"       % version
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    )
  )

}
