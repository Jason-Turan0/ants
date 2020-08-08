@echo off
rem clean
del *.class
del MyBot.jar
rem compile
javac MyBot.java
rem package
jar cvfm MyBot.jar META-INF/MANIFEST.MF *.class 
rem clean
del *.class
