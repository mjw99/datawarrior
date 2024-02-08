## DataWarrior (Mavenised fork)
*DataWarrior* is a program for interactive data analysis and visualization. While it is
widely used by data analysts in general, it is particular useful for cheminformaticians,
because chemical structures and reactions are native datatypes and because of its rich
cheminformatics functionality.

*DataWarrior* runs on all major platforms. Pre-built installers exist for Linux, Macintosh and Windows.
Software installers, documentation, sample data, and a support forum are available on
https://openmolecules.org/datawarrior.

*DataWarrior* is built on the open-source projects *OpenChemLib* and *FXMolViewer*. 

### Dependencies
Apart from a working JDK11, *DataWarrior* needs various free-to-use/open-source dependencies.
All required dependency files are provided via Maven.
The most important ones are:
* OpenChemLib: Cheminformatics base functionality to handle molecules and reactions
* FXMolViewer: 3D-molecule & protein visualization, editing, interaction using JavaFX
  (includes Sunflow ray-tracer and MMTF to download and parse binary structure files from the PDB-database)
* Batik: Support for SVG vector graphics
* Database connectors for: MySQL, PostgreSQL, Oracle, Microsoft SQL-Server
* Opsin: IUPAC name conversion to chemical structures
* Substance Look&Feel: professionally designed user interface skin
* Java Expression Parser: for calculating new column data using custom equations

### How to build the project via Maven on a Debian like system
=======
```
sudo apt-get install openjdk-17 maven bintuils fakeroot

git clone https://github.com/thsa/fxmolviewer.git
cd fxmolviewer
mvn clean install "-DreleaseVersion=0.0.1-SNAPSHOT"

cd ..
git clone https://github.com/mjw99/datawarrior
cd datawarrior
mvn clean package

sudo dpkg -i ./target/dist/datawarrior*.deb
/opt/datawarrior/bin/datawarrior
```
### How to build the project via Maven on a Windows System
1) Install JDK >= 17 
2) Install Maven and ensure it is in your PATH
3) Download wix311-binaries.zip from WiX Toolset v3 releases  
(https://github.com/wixtoolset/wix3/releases) 
4) Unpack the archive into wix3 folder 
5) Add the wix3 folder to PATH environment variable

6) Clone and build the repo
```
git clone https://github.com/mjw99/datawarrior
cd datawarrior
mvn clean package
```
7) Run the installer that is created in the target/dist directory


### Platform Integration
Ideally, *DataWarrior* should be installed in a platform specific way that registers its file
extentions and causes proper file icon display. Installers for Linux, Macintosh, and Windows,
which include proper platform integration, can be downloaded from
https://openmolecules.org/datawarrior/download.html.

This platform integration for Windows is not (yet) part of this project. Explanations and
scripts of how to build installers for Linux and Macintosh with proper platform integration
can be found in the *linux* and *macosx* directories.

If you intend to run *DataWarrior* from self compiled source code and if you cannot or don't
want to do the platform integration yourself, then you may still install *DataWarrior* with the
official installer and then replace the original datawarrior_all.jar file with a freshly
built one.

### How to contribute
Contact the author under the e-mail shown on https://openmolecules.org/about.html


### License
*DataWarrior*. Copyright (C) 2023 Thomas Sander & Idorsia Pharmaceuticals Ltd.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


### Supported by

![YourKit-logo](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools 
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/),
[YourKit .NET Profiler](https://www.yourkit.com/dotnet-profiler/),
and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).
