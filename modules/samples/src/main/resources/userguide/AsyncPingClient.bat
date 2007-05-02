@echo off
setlocal EnableDelayedExpansion
set SANDESHA2_CLASS_PATH=.\UserguideSampleClients.jar
FOR %%c in (.\lib\*.jar) DO set SANDESHA2_CLASS_PATH=!SANDESHA2_CLASS_PATH!;%%c

java -cp %SANDESHA2_CLASS_PATH% sandesha2.samples.userguide.AsyncPingClient .\