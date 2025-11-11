ated-subscription-frontend
==========================

This service provides the ability for UK-based or Non-UK based ATED clients (or agents on behalf of clients) to subscribe to ATED service.
Users subscribing must also register in ETMP (using business-customer-frontend) before subscribing (ROSM).

Requirements
-------------

This service is written in [Scala] and [Play], so needs the latest [JRE] to run.


Authentication
------------

This user logs into this service using the [Government Gateway]


Acronyms
--------

In the context of this service we use the following acronyms:

* [API]: Application Programming Interface

* [HoD]: Head of Duty

* [JRE]: Java Runtime Environment

* [JSON]: JavaScript Object Notation

* [URL]: Uniform Resource Locator

### All tests and checks

> `sbt runAllChecks`

This is an sbt command alias specific to this project. It will run
- unit tests
- integration tests
- integration tests (if any)
- and produce a coverage report.

You can view the coverage report in the browser by pasting the generated url.

#### Installing sbt plugin to check for library updates.
To check for dependency updates locally you will need to create this file locally ~/.sbt/1.0/plugins/sbt-updates.sbt
and paste - addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.3") - into the file.
Then run:

> `sbt dependencyUpdates `

To view library update suggestions - this does not cover sbt plugins.
It is not advised to install the plugin for the project.

License
-------

This code is open source software licensed under the [Apache 2.0 License].

[Scala]: http://www.scala-lang.org/
[Play]: http://playframework.com/
[JRE]: http://www.oracle.com/technetwork/java/javase/overview/index.html

[Government Gateway]: http://www.gateway.gov.uk/

[API]: https://en.wikipedia.org/wiki/Application_programming_interface
[HoD]: http://webarchive.nationalarchives.gov.uk/+/http://www.hmrc.gov.uk/manuals/sam/samglossary/samgloss249.htm
[JSON]: http://json.org/
[URL]: https://en.wikipedia.org/wiki/Uniform_Resource_Locator

[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0.html
