
commons-j
=========

Welcome to commons-j - a small repository of (hopefully) useful java code that I have not found elsewhere.
If you see something here that exists elsewhere report an issue with a link - will keep this code smaller :)

What's Inside
-------------

### Simplified XML Parsing

Removes the application code bloat/noise of SAX/StAX/JAXB in a very simple callback-style interface.
Allows mixing of StAX and/or JAXB in one or more POJO handlers in a single parse.
Great for parsing large XML files with a small memory footprint _even with JAXB_.

For more detail go [here](saxbp.md) (readers on the Maven-generated site should go [here](saxbp.html) instead).

### P2P Publish/Subscribe

No frills peer-to-peer network messaging layer for broadcasting messages in a cluster (not necessarily on a local network).
Simpler than JMS's publish/subscribe API and no middleware required.

For more detail go [here](pubsub.md) (readers on the Maven-generated site should go [here](pubsub.html) instead).

### Miscellaneous Utility Code

* C++'s pair (as well as triple and quadruple)
* Guava functions that capture/delegate exceptions and a decision function based on a predicate
* Ternary enum (true, false, unknown)
* InputStream wrapper around ByteBuffer
* Network utility to more easily work with machine local IP addresses
* Thread-safe basic integer statistic

Usage
-----

For use with Maven add the following repositories to your pom.xml:

    <repositories>
        <repository>
            <id>us.harward.repo.release</id>
            <name>Maven 2 Release Repository for harward.us</name>
            <url>http://www.harward.us/maven2/repo/release</url>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>us.harward.repo.snapshot</id>
            <name>Maven 2 Snapshot Repository for harward.us</name>
            <url>http://www.harward.us/maven2/repo/snapshot</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>interval:5</updatePolicy>
            </snapshots>
            <releases>
                <enabled>false</enabled>
            </releases>
        </repository>
    </repositories>

And then the following for the actual library:

    <dependency>
        <groupId>nerds.antelax</groupId>
        <artifactId>commons-j</artifactId>
        <version>1.0.0</version>
    </dependency>

Dependencies
------------

commons-j depends on these fine external libraries:

* [Guava](http://code.google.com/p/guava-libraries/) - general purpose utility code
* [Netty](http://netty.io/) - supports the publish/subscribe feature
* [SLF4J](http://www.slf4j.org/) - logging framework which (at the time of writing) will very likely work with your application's existing logging framework
