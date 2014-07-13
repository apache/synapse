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
set DEFAULT_AXIS2_HOME=%~dp0

if "%AXIS2_HOME%"=="" set AXIS2_HOME=%DEFAULT_AXIS2_HOME%
set DEFAULT_AXIS2_HOME=

rem find AXIS2_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x

if exist "%AXIS2_HOME%\repository\conf\axis2.xml" goto checkJava

:noAxis2Home
echo AXIS2_HOME environment variable is set incorrectly or AXIS2 could not be located. 
echo Please set the AXIS2_HOME variable appropriately
goto end

:checkJava
set _JAVACMD=%JAVACMD%
set _PORT=

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe

:setupArgs
if ""%1""=="""" goto defaultParams
if ""%1""==""-port"" goto port
shift
goto setupArgs

rem is a custom port specified
:port
shift
set _PORT="-Dport=%1"
shift
goto setupArgs

:defaultParams
set AXIS2_CMD_LINE_ARGS=-repo "%AXIS2_HOME%\repository" -conf "%AXIS2_HOME%\repository\conf\axis2.xml"
goto runAxis2

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe
echo JAVA_HOME environment variable is set incorrectly or Java runtime could not be located.
echo Please set the JAVA_HOME variable appropriately
goto end

:runAxis2
rem set the classes by looping through the libs
setlocal EnableDelayedExpansion
set AXIS2_CLASS_PATH=%AXIS2_HOME%
FOR %%c in ("%AXIS2_HOME%\..\..\lib\*.jar") DO set AXIS2_CLASS_PATH=!AXIS2_CLASS_PATH!;%%c

echo Using JAVA_HOME    %JAVA_HOME%
echo Using AXIS2_HOME   %AXIS2_HOME%

cd %AXIS2_HOME%
"%_JAVACMD%" %_PORT% %JAVA_OPTS% -cp "%AXIS2_CLASS_PATH%" samples.util.SampleAxis2Server %AXIS2_CMD_LINE_ARGS%
goto end

:end
set _JAVACMD=
set AXIS2_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd
