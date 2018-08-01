/*
 * Copyright (c) 2014-2016 by Alexandru Nedelcu.
 * Some rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}
import sbt.Keys._
import com.typesafe.sbt.GitVersioning

addCommandAlias("ci-all", ";+clean ;+compile ;+test ;+package")
addCommandAlias("release", ";+publishSigned ;sonatypeReleaseAll")

ThisBuild / scalaVersion := "2.12.4"
ThisBuild / crossScalaVersions := Seq("2.10.7",
                                      "2.11.12",
                                      "2.12.4",
                                      "2.13.0-M4")

def scalaPartV = Def setting (CrossVersion partialVersion scalaVersion.value)
lazy val crossVersionSharedSources: Seq[Setting[_]] =
  Seq(Compile, Test).map { sc =>
    (unmanagedSourceDirectories in sc) ++= {
      (unmanagedSourceDirectories in sc).value.map { dir =>
        scalaPartV.value match {
          case Some((major, minor)) =>
            new File(dir.getPath + s"_$major.$minor")
          case None =>
            throw new NoSuchElementException("Scala version")
        }
      }
    }
  }

lazy val scalaLinterOptions =
  Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xfuture", // Turn on future language features.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match", // Pattern match may not be typesafe.
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates", // Warn if a private member is unused.
    "-Ywarn-value-discard" // Warn when non-Unit expression results are unused
  )

lazy val sharedSettings = Seq(
  scalacOptions in ThisBuild ++= Seq(
    // Note, this is used by the doc-source-url feature to determine the
    // relative path of a given source file. If it's not a prefix of a the
    // absolute path of the source file, the absolute path of that file
    // will be put into the FILE_SOURCE variable, which is
    // definitely not what we want.
    "-sourcepath",
    file(".").getAbsolutePath.replaceAll("[.]$", "")
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-Xlint",
    "-Ywarn-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Xlog-free-terms"
  ),
  // Version specific options
  scalacOptions ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) if v >= 12 =>
      scalaLinterOptions
    case Some((2, 11)) =>
      scalaLinterOptions ++ Seq("-target:jvm-1.6")
    case _ =>
      Seq("-target:jvm-1.6")
  }),
  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
    Resolver.sonatypeRepo("releases")
  ),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  testFrameworks := Seq(
    new TestFramework("minitest.runner.Framework"),
    new TestFramework("minitest.runner.IOFramework")
  )

)

lazy val scalaJSSettings = Seq(
  scalaJSStage in Test := FastOptStage
)

lazy val needsScalaParadise = settingKey[Boolean]("Needs Scala Paradise")

lazy val requiredMacroCompatDeps = Seq(
  needsScalaParadise := {
    val sv = scalaVersion.value
    (sv startsWith "2.10.") || (sv startsWith "2.11.") || (sv startsWith "2.12.") || (sv == "2.13.0-M3")
  },
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % Compile,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
    "org.typelevel" %%% "macro-compat" % "1.1.1",
  ),
  libraryDependencies ++= {
    if (needsScalaParadise.value)
      Seq(
        compilerPlugin(
          "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch))
    else Nil
  },
  scalacOptions ++= {
    if (needsScalaParadise.value) Nil
    else Seq("-Ymacro-annotations")
  }
)

lazy val minitestRoot = project
  .in(file("."))
  .aggregate(minitestJVM, minitestJS, lawsJVM, lawsJS)
  .settings(
    name := "minitest root",
    Compile / sources := Nil,
    skip in publish := true,
  )

lazy val minitest = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("."))
  .settings(
    name := "minitest",
    sharedSettings,
    crossVersionSharedSources,
    requiredMacroCompatDeps
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % "1.0",
      "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided",
      "org.typelevel" %% "cats-core" % "1.2.0",
      "org.typelevel" %% "cats-effect" % "1.0.0-RC2",
      "co.fs2" %% "fs2-core" % "0.10.5"
    ),
  )
  .jsSettings(
    scalaJSSettings,
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion,
      "org.typelevel" %%% "cats-core" % "1.2.0",
      "org.typelevel" %%% "cats-effect" % "1.0.0-RC2",
      "co.fs2" %%% "fs2-core" % "0.10.5"
    )
  )

lazy val minitestJVM = minitest.jvm
lazy val minitestJS = minitest.js
// lazy val minitestNative = minitest.native

lazy val laws = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("laws"))
  .dependsOn(minitest)
  .settings(
    name := "minitest-laws",
    sharedSettings,
    crossVersionSharedSources,
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.14.0"
    )
  )
  .jsSettings(
    scalaJSSettings
  )

lazy val lawsJVM = laws.jvm
lazy val lawsJS = laws.js
// lazy val lawsNative = laws.native
