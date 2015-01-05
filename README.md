PropertiesLoader
================

Load properties from files from classpath or file into Java system properties.

command line usage examples
==================

Load single file
------------

```
java -DpropertyLocations=file:config.properties
```

Load several files
------------------

```
java -DpropertyLocations=file:config1.properties,anotherfolder/file:anotherconfig.properties
```

Load from classpath
-------------------

```
java -DpropertyLocations=classpath:/config.properties
```

Templating inside the property file
===========================================

```
firstname=John
lastname=Smith
fullname=${firstname} ${lastname}
```


Usage in Java Code
===========================================

```
// do once in code, e.g. static
PropertiesLoader.loadPropertiesLocationsIntoSystemProperties();

// get a property
String firstname = System.getProperty("firstname");
```


