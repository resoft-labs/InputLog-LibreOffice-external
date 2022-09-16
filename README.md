# LibreOffice-external

Simple example standalone application capable of doing these things (in this order): 

- Start a libreoffice with the command line required to allow remote access
- Start a logging session using this libreoffice
- Stop this logging session

## Compiling and running

- checkout the code
- compile using gradle: `./gradlew build`

This should result in all necessary jars ending up in `lib`.

Run like this: 

`java -cp "./lib/*" test.Main`

This should print the usage: 

    Usage: [ startoffice | startsession | stopsession ]

Next try the following: 

`java -cp "./lib/*" test.Main startoffice`

This should start a libreoffice. The application assumes libreoffice can be found in your path

`java -cp "./lib/*" test.Main startsession`

This should start a writer session with logging.

`java -cp "./lib/*" test.Main stopsession`

This should stop the writer session, ask for notes, etc.


## Useful resources
- LibreOffice Developer's Guide - https://wiki.documentfoundation.org/Documentation/DevGuide
- Developer's Guide Examples - https://api.libreoffice.org/examples/DevelopersGuide/examples.html
- API examples - https://api.libreoffice.org/examples/examples.html
- Writing UNO Components - https://wiki.documentfoundation.org/Documentation/DevGuide/Writing_UNO_Components
- DispatchCommands - https://wiki.documentfoundation.org/Development/DispatchCommands
- Spreadsheet Example written in C# - https://api.libreoffice.org/examples/CLI/CSharp/Spreadsheet/
