#!/bin/sh
# -----------------------------------------------------------------------------
#
# Environment Variable Prequisites
#
#   SYNAPSE_HOME   Home of Synapse installation. If not set I will  try
#                   to figure it out.
#
#   JAVA_HOME       Must point at your Java Development Kit installation.
#
# NOTE: Borrowed generously from Apache Tomcat startup scripts.

# if JAVA_HOME is not set we're not happy
if [ -z "$JAVA_HOME" ]; then
  echo "You must set the JAVA_HOME variable before running Tungsten."
  exit 1
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
OS400*) os400=true;;
esac

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set SYNAPSE_HOME if not already set
[ -z "$SYNAPSE_HOME" ] && SYNAPSE_HOME=`cd "$PRGDIR/.." ; pwd`



# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$SYNAPSE_HOME" ] && SYNAPSE_HOME=`cygpath --unix "$SYNAPSE_HOME"`
  [ -n "$AXIS2_HOME" ] && TUNGSTEN_HOME=`cygpath --unix "$SYNAPSE_HOME"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# For OS400
if $os400; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('$JOBNAME') runpty(6)'
  system $COMMAND

  # Enable multi threading
  export QIBM_MULTI_THREADED=Y
fi

# update classpath
SYNAPSE_CLASSPATH=""
for f in $SYNAPSE_HOME/lib/*.jar
do
  SYNAPSE_CLASSPATH=$SYNAPSE_CLASSPATH:$f
done
SYNAPSE_CLASSPATH=$JAVA_HOME/lib/tools.jar$SYNAPSE_CLASSPATH:$CLASSPATH

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  SYNAPSE_HOME=`cygpath --absolute --windows "$SYNAPSE_HOME"`
  AXIS2_HOME=`cygpath --absolute --windows "$SYNAPSE_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  JAVA_ENDORSED_DIRS=`cygpath --path --windows "$JAVA_ENDORSED_DIRS"`
fi
# endorsed dir
SYNAPSE_ENDORSED=$JAVA_HOME/lib/endorsed

# ----- Execute The Requested Command -----------------------------------------

cd $SYNAPSE_HOME
echo "Starting Synapse/Java ..."
echo "Using SYNAPSE_HOME:    $SYNAPSE_HOME"
echo "Using JAVA_HOME:       $JAVA_HOME"

$JAVA_HOME/bin/java -Daxis2.xml=$SYNAPSE_HOME/synapse_repository/conf/axis2.xml -Djava.endorsed.dirs=$SYNAPSE_ENDORSED -classpath $SYNAPSE_CLASSPATH org.apache.axis2.transport.http.SimpleHTTPServer $SYNAPSE_HOME/synapse_repository -p8080
