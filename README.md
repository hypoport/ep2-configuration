PropertiesLoader
================

Load properties from files into java system properties.

command line usage
==================

Simple usage
------------

```
java -DpropertyLocations=file:config.properties
```

Load several files
------------------

```
java -DpropertyLocations=file:config1.properties,anotherfolder/anotherconfig.properties
```

Load from classpath
-------------------

```
java -DpropertyLocations=classpath:/config.properties
```

Load the properties inside the java program
===========================================

```java
PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();
```

simple templating inside the property file
------------------------------------------

```
firstname=John
lastname=Smith
fullname=${firstname} ${lastname}
```