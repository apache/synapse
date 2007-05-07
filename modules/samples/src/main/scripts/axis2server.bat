@echo off

REM  Copyright 2001,2004-2005 The Apache Software Foundation
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

rem ---------------------------------------------------------------------------
rem Startup script for the Simple Axis Server (with default parameters)
rem
rem Environment Variable Prequisites
rem
rem   AXIS2_HOME      Must point at your AXIS2 directory
rem
rem   JAVA_HOME       Must point at your Java Development Kit installation.
rem
rem   JAVA_OPTS       (Optional) Java runtime options
rem ---------------------------------------------------------------------------

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
if "%AXIS2_HOME%"=="" set AXIS2_HOME=%~dps0

rem find AXIS2_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x

if exist "%AXIS2_HOME%\repository\conf\axis2.xml" goto checkJava

:noAxis2Home
echo AXIS2_HOME environment variable is set incorrectly or AXIS2 could not be located.
echo Please set the AXIS2_HOME variable appropriately
goto end

:checkJava
set _JAVACMD=%JAVACMD%
set _HTTPPORT=
set _HTTPSPORT=
set _SERVERNAME=

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe

:setupArgs
if ""%1""=="""" goto runAxis2
if ""%1""==""-http"" goto httpport
if ""%1""==""-https"" goto httpsport
if ""%1""==""-name"" goto servername
shift
goto setupArgs

rem is a custom port specified
:httpport
shift
set _HTTPPORT="-Dhttp_port=%1"
shift
goto setupArgs

:httpsport
shift
set _HTTPSPORT="-Dhttps_port=%1"
shift
goto setupArgs

:servername
shift
set _SERVERNAME="-Dserver_name=%1"
shift
goto setupArgs

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo JAVA_HOME environment variable is set incorrectly or Java runtime could not be located.
echo Please set the JAVA_HOME variable appropriately
goto end

:runAxis2
rem set the classes by looping through the libs
setlocal EnableDelayedExpansion
set AXIS2_CLASS_PATH=%AXIS2_HOME%/../../lib;%AXIS2_HOME%/../../repository/conf
FOR %%c in ("%AXIS2_HOME%\..\..\lib\*.jar") DO set AXIS2_CLASS_PATH=!AXIS2_CLASS_PATH!;%%c

rem use proper bouncy castle version for the JDK

"%JAVA_HOME%\bin\java" -version 2>&1 | findstr "1.4" >NUL
IF ERRORLEVEL 1 goto checkJdk15
echo  Using Bouncy castle JAR for Java 1.4
FOR %%C in ("%AXIS2_HOME%\..\..\lib\bcprov-jdk13*.jar") DO set AXIS2_CLASS_PATH="%%~fC";!AXIS2_CLASS_PATH!
goto runServer

:checkJdk15
"%JAVA_HOME%\bin\java" -version 2>&1 | findstr "1.5" >NUL
IF ERRORLEVEL 1 goto runServer
echo  Using Bouncy castle JAR for Java 1.5
FOR %%C in ("%AXIS2_HOME%\..\..\lib\bcprov-jdk15*.jar") DO set AXIS2_CLASS_PATH="%%~fC";!AXIS2_CLASS_PATH!

:runServer
set AXIS2_ENDORSED=%AXIS2_HOME%\..\..\lib\endorsed
echo Using JAVA_HOME    %JAVA_HOME%
echo Using AXIS2_HOME   %AXIS2_HOME%

cd %AXIS2_HOME%
"%_JAVACMD%" %_HTTPPORT% %_HTTPSPORT% %_SERVERNAME% %JAVA_OPTS% -cp "%AXIS2_CLASS_PATH%" -Djava.endorsed.dirs="%AXIS2_ENDORSED%" samples.util.SampleAxis2Server -repo "%AXIS2_HOME%\repository" -conf "%AXIS2_HOME%\repository\conf\axis2.xml"
goto end

:end
set _JAVACMD=
set AXIS2_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd
