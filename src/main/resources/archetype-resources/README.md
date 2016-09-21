ODLMapleAdapter
===============

This is an adapter of Maple in OpenDaylight.

## Documents

API Guide: TODO
User Guide: TODO
Developer Guide: TODO

## Installation

### Build MapleCore

ODLMapleAdapter is dependent on MapleCore Library. You must build MapleCore
before building ODLMapleAdapter.

``` bash
git clone https://github.com/snlab/MapleCore.git
cd MapleCore
mvn clean install
```

### Prerequirements

Before building ODLMapleAdapter, please make sure your maven can access the
repository of OpenDaylight. Edit your `settings.xml` of maven like the following
link:

<https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup#Edit_your_.7E.2F.m2.2Fsettings.xml>

### Build ODLMapleAdapter

And then, you can use maven to build ODLMapleAdapter:

``` bash
git clone https://github.com/snlab/odlmaple.git
cd odlmaple
mvn clean install
```

## Usage

Start Maple from karaf:

``` bash
$(odlmaple_root_directory)/karaf/target/assembly/bin/karaf
```

