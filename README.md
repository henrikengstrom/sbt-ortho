# sbt-ortho

A plugin that helps teams who are notoriously bad with spelling (looking at myself here) and for teams struggling with what type of English to use for documentation/comments etc.

The plugin contains two major features: spell checking and style checking. Spell checking is self-explanatory whereas style checking means to convert from one way of spelling (English in this case) to another. The latter can be good when a team is built up by some people who prefer Cowboy English and others who like to use English the way the Queen intended for her language to be spelled.

Let me know if you'd like any other features (or even better create a PR with some additional features).

# How to use

Add the plugin to your `project/plugins.sbt` file:


    resolvers += Resolver.url(
      "bintray-sbt-plugin-releases",
        url("http://dl.bintray.com/h3nk3/sbt-plugins/"))(
          Resolver.ivyStylePatterns)

    addSbtPlugin("org.h3nk3" % "sbt-ortho" % "0.1.1")

## Tasks

There are a couple of tasks available in this plugin:

* orthoReport : task will print a report on the command line (see below)
* alignStyles : task will rewrite files to align styles from one spelling type to another (default is UK to US)
* spellCheck : task will rewrite files to make sure that spelling is correct (default spelling is US English)

_Note:_ It is recommended to run the `orthoReport` task before running any of the tasks that will rewrite files. This way you can see what a rewrite will result in *before* you run it.

### Example Report

The report contains two parts; one for the style alignment and one for the spell checker. Only files containing words that the style or spell checker believes should be fixed will be part of the report.

Here is an example output from executing the task `orthoReport`:

    SomeMachine:sbt-ortho-test someuser$ sbt
    [info] Loading project definition from /Users/x/code/sbt-ortho-test/project
    [info] Set current project to sbt-ortho-test (in build file:/Users/x/code/sbt-ortho-test/)
    > orthoReport
    [info]
    [info] ORTHO STYLE ALIGNMENT REPORT FOR PROJECT FILES
    [info] FILE: /Users/someuser/code/sbt-ortho-test/src/main/scala/HelloThereSir.txt
    [info] #1 : canalising/canalizing
    [info]
    [info] ORTHO SPELLING REPORT FOR PROJECT FILES
    [info] FILE: /Users/someuser/code/sbt-ortho-test/src/main/scala/HelloThereSir.txt
    [info] #3 : cancelations/cancelation
    [info] #4 : canceeld/canceled
    [info] #5 : canceling/concealing
    [info] FILE: /Users/someuser/code/sbt-ortho-test/src/main/scala/Sample1.scala
    [info] #2 : thsi/this, flie/lie
    [info] #7 : thees/these
    [info] #9 : golas/goals
    [info]
    [success] Total time: 1 s, completed Dec 30, 2015 6:09:12 PM
    >

As you can see in file `/Users/x/code/sbt-ortho-test/src/main/scala/Sample1.scala` on line #2 the misspelled word "flie" has a suggested correction to "lie". It should more likely be corrected to "file" but the training file used probably contains more "lie" than "file" counts and therefore "lie" is the suggested correction. By providing more and better training material for your domain, which in this case is the computer domain, you can increase the chances of the right suggestions from the spell checker.

### Changing Style of Spell Checker Behavior

Both the behavior of the style and spell checker can be altered by providing other training and dictionary files.

The spell checker will traverse every work in the training file and build its "knowledge" of the spelling based on occurrences of words in this file. By providing a chunk of text in UK English you will inform the spell checker how to spell. The default training file is geared toward US English.

The default dictionary file goes from UK to US English. Should you prefer the other way around you can reverse the provided file and use that instead.

See settings here below for how to override the default files.

## Settings

The following settings are available:

* trainingFile : file to use for training of the spell checker (defaults to the provided US-training.txt file)
* dictionaryFile : file to use for the style dictionary (default to the provided UK-US.txt file )

### File Types Supported
Currently only files with a file ending of one of the following will be handled:

* .scala
* .java
* .md
* .txt

## About

Ortho is short for [orthography](https://en.wikipedia.org/wiki/Orthography) which is a fancy word for spelling correctly.

The spell checker is based on the ideas in [Peter Norvig's blog post](http://norvig.com/spell-correct.html).
Credits to [Thomas Jung](http://theyougen.blogspot.com/2009/12/peter-norvigs-spelling-corrector-in.html) for inspiration of the Scala implementation.

_The plugin has no association with the company I work for. It comes with no quality guarantees. Use at your own peril. Pull requests to improve it are more than welcome!_
