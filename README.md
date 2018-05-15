ated-subscription-frontend
==========================

[![Build Status](https://travis-ci.org/hmrc/ated-subscription-frontend.svg)](https://travis-ci.org/hmrc/ated-subscription-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/ated-subscription-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/ated-subscription-frontend/_latestVersion)

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
