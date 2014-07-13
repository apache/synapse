@echo off

REM   Licensed to the Apache Software Foundation (ASF) under one
REM   or more contributor license agreements.  See the NOTICE file
REM   distributed with this work for additional information
REM   regarding copyright ownership.  The ASF licenses this file
REM   to you under the Apache License, Version 2.0 (the
REM   "License"); you may not use this file except in compliance
REM   with the License.  You may obtain a copy of the License at
REM
REM    http://www.apache.org/licenses/LICENSE-2.0
REM
REM   Unless required by applicable law or agreed to in writing,
REM   software distributed under the License is distributed on an
REM   REM  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM   KIND, either express or implied.  See the License for the
REM   specific language governing permissions and limitations
REM   under the License.

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

rem %~dp0 is expanded pathname of the current script under NT
set DEFAULT_SYNAPSE_HOME=%~dp0..

if "%SYNAPSE_HOME%"=="" set SYNAPSE_HOME=%DEFAULT_SYNAPSE_HOME%
set DEFAULT_SYNAPSE_HOME=

set _USE_CLASSPATH=yes
set _SYNAPSE_XML=
set _XDEBUG=
set _PORT=

rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).

:setupArgs
if ""%1""=="""" goto doneStart
if ""%1""==""-sample"" goto synapseSample
if ""%1""==""-noclasspath"" goto clearclasspath
if ""%1""==""-xdebug"" goto xdebug
if ""%1""==""-port"" goto port
set SYNAPSE_CMD_LINE_ARGS=%SYNAPSE_CMD_LINE_ARGS% %1
shift
goto setupArgs

set SYNAPSE_CMD_LINE_ARGS=%1
if ""%1""=="""" goto doneStart
shift

rem here is there is a -noclasspath in the options
:clearclasspath
set _USE_CLASSPATH=no
shift
goto setupArgs

rem is there is a -xdebug in the options
:xdebug
set _XDEBUG=-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,address=8000
shift
goto setupArgs

rem is a custom port specified
:port
shift
set _PORT="-Dport=%1"
shift
goto setupArgs


rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:synapseSample
shift
set _SYNAPSE_XML=-Dsynapse.xml="%SYNAPSE_HOME%\repository\conf\sample\synapse_sample_%1.xml"
shift
goto setupArgs

:doneStart
rem find SYNAPSE_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%SYNAPSE_HOME%\README.TXT" goto checkJava

:noSYNAPSEHome
echo SYNAPSE_HOME is set incorrectly or SYNAPSE could not be located. Please set SYNAPSE_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
if  "%SYNAPSE_CMD_LINE_ARGS%" == "" goto defaultParams

goto runSynapse

:defaultParams
set SYNAPSE_CMD_LINE_ARGS="%SYNAPSE_HOME%\repository"
goto runSynapse

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe

:runSynapse
rem set the classes by looping through the libs
setlocal EnableDelayedExpansion
set SYNAPSE_CLASS_PATH="%SYNAPSE_HOME%"
FOR %%C in ("%SYNAPSE_HOME%\lib\*.jar") DO set SYNAPSE_CLASS_PATH=!SYNAPSE_CLASS_PATH!;"%%~fC"
set SYNAPSE_CLASS_PATH="%SYNAPSE_HOME%\lib";%SYNAPSE_CLASS_PATH%

rem if a sample configuration is not specified, use default
if "%_SYNAPSE_XML%" == "" set _SYNAPSE_XML=-Dsynapse.xml="%SYNAPSE_HOME%\repository\conf\synapse.xml"

set SYNAPSE_ENDORSED="%SYNAPSE_HOME%\lib\endorsed";"%JAVA_ENDORSED_DIRS%";"%JAVA_HOME%\lib\endorsed"

@rem @echo on
cd %SYNAPSE_HOME%
"%_JAVACMD%" %_PORT% %_SYNAPSE_XML% -Daxis2.xml="%SYNAPSE_HOME%\repository\conf\axis2.xml" -Djava.endorsed.dirs=%SYNAPSE_ENDORSED% %_XDEBUG% -cp %SYNAPSE_CLASS_PATH% org.apache.synapse.SynapseHTTPServer %SYNAPSE_CMD_LINE_ARGS%
goto end

:end
set _JAVACMD=
set SYNAPSE_CMD_LINE_ARGS=

if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

:mainEnd

