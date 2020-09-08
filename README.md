
[![](https://img.shields.io/badge/umler--viewer-v0.1.1-green)](https://github.com/krossovochkin/umler.kt/releases/tag/v0.1.1) [![](https://img.shields.io/badge/umler--scanner-v0.1.1-blue)](https://bintray.com/krossovochkin/umler.kt/umler-scanner)

# umler.kt
UML generator for Kotlin projects

# Introduction
The Unified Modeling Language (UML) is a general-purpose, developmental, modeling language in the field of software engineering that is intended to provide a standard way to visualize the design of a system. [Wiki](https://en.wikipedia.org/wiki/Unified_Modeling_Language).  
UML has various types of diagrams. One of which are structure diagrams, which allow to visualize components of the program.  
Usually, it is representation of classes, interfaces and connections between them.

UML diagrams are good to have a visual representation of architecture.  
There are various ways to use UML, from which two are:
- diagram which is created before developement to model architecture of a program
- diagram which is created/generated after development to verify that architecture meets original expectations.

umler.kt is a UML generator for Kotlin projects which aims verification of architectures.
It consists of two parts:
- umler-scanner - detekt plugin which scans source code and extracts information about program. All the information is serialized into 'umler.json' file.
- umler - standalone crossplatform UML viewer for generated 'umler.json' file (written with JavaFX)

# Getting started

## Setup detekt

In a project one wants to generate UML diagram for one should setup detekt following [guidelines](https://github.com/detekt/detekt).  
After basic integration it is needed to add dependency on a detektPlugins for umler-scanner:

```
detektPlugins "com.krossovochkin.detekt:umler-scanner:$version"
```

## Generate UML

To create 'umler.json' file for module 'module' one need to execute:

```
./gradlew :module:detektMain
```

NOTE: important that not 'detekt' task, but 'detektMain' executed, as only last has type resolution, which is required for umler.kt

## View UML

To view visualized UML diagram one need to start viewer app:

```
java -jar umler.jar
```
Then in the file chooser select 'umler.json' file generated in the previous step (located in the project dir).
Viewer allows moving elements for better visualization.

![](https://github.com/krossovochkin/umler.kt/blob/v0.1.1/image/umler-viewer.png)

# Supported elements

Elements are shown as a rectangles.  
There are two types supported:
- class
- interface

Only public classes and interfaces are shown

# Supported connections

Connections are shown as arrows between elements.
Supported connections:
- Implements (class implements interface)
- Extends (class extends class, interface extends interface)
- Aggregates (class/interface contains properties of other class/interface)
- Uses (class/interface has other class/interface in params of member functions)

NOTE: Connections are estimated. That means that from code perspective without deep analysis is difficult to distinguish e.g. Composition. For simplicity most generic connections are generated.
