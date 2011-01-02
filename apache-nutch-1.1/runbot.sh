# runbot script to run the Nutch bot for crawling and re-crawling.
# Usage: bin/runbot [safe]
#        If executed in 'safe' mode, it doesn't delete the temporary
#        directories generated during crawl. This might be helpful for
#        analysis and recovery in case a crawl fails.
#
# Author: Susam Pal
# Modified by: Si Chen

# Si: set the depth to sufficiently large
depth=20
threads=5
adddays=5
# Si: I commented out the topN option
#topN=15 #Comment this statement if you don't want to set topN value

# Arguments for rm and mv
RMARGS="-rf"
MVARGS="--verbose"

# Parse arguments
if [ "$1" == "safe" ]
then
  safe=yes
fi

if [ -z "$NUTCH_HOME" ]
then
  NUTCH_HOME=.
  echo runbot: $0 could not find environment variable NUTCH_HOME
  echo runbot: NUTCH_HOME=$NUTCH_HOME has been set by the script
else
  echo runbot: $0 found environment variable NUTCH_HOME=$NUTCH_HOME
fi

if [ -z "$CATALINA_HOME" ]
then
  CATALINA_HOME=/opt/apache-tomcat-6.0.10
  echo runbot: $0 could not find environment variable NUTCH_HOME
  echo runbot: CATALINA_HOME=$CATALINA_HOME has been set by the script
else
  echo runbot: $0 found environment variable CATALINA_HOME=$CATALINA_HOME
fi

if [ -n "$topN" ]
then
  topN="-topN $topN"
else
  topN=""
fi

steps=8
echo "----- Inject (Step 1 of $steps) -----"
$NUTCH_HOME/bin/nutch inject crawl/crawldb urls

echo "----- Generate, Fetch, Parse, Update (Step 2 of $steps) -----"
for((i=0; i < $depth; i++))
do
  echo "--- Beginning crawl at depth `expr $i + 1` of $depth ---"
  # add a counter to count execution time
  start_time=$(date +%s)

  $NUTCH_HOME/bin/nutch generate crawl/crawldb crawl/segments $topN \
      -adddays $adddays
  if [ $? -ne 0 ]
  then
    echo "runbot: Stopping at depth $depth. No more URLs to fetch."
    break
  fi
  segment=`ls -d crawl/segments/* | tail -1`

  $NUTCH_HOME/bin/nutch fetch $segment -threads $threads
  if [ $? -ne 0 ]
  then
    echo "runbot: fetch $segment at depth `expr $i + 1` failed."
    echo "runbot: Deleting segment $segment."
    rm $RMARGS $segment
    continue
  fi

  $NUTCH_HOME/bin/nutch updatedb crawl/crawldb $segment
  
  # calculate the execution time
  end_time=$(date +%s)
  totalseconds=$((end_time-start_time))
  hours=$((totalseconds/3600))
  remainder=$((totalseconds%3600))
  minutes=$((remainder/60))
  seconds=$((remainder%60))
  echo "Finished depth `expr $i +1` in: $hours hours $minutes minutes $seconds seconds"


done

echo "----- Merge Segments (Step 3 of $steps) -----"
$NUTCH_HOME/bin/nutch mergesegs crawl/MERGEDsegments crawl/segments/*
if [ "$safe" != "yes" ]
then
  rm $RMARGS crawl/segments
else
  rm $RMARGS crawl/BACKUPsegments
  mv $MVARGS crawl/segments crawl/BACKUPsegments
fi

mv $MVARGS crawl/MERGEDsegments crawl/segments

echo "----- Invert Links (Step 4 of $steps) -----"
$NUTCH_HOME/bin/nutch invertlinks crawl/linkdb crawl/segments/*

echo "----- Index (Step 5 of $steps) -----"
$NUTCH_HOME/bin/nutch index crawl/NEWindexes crawl/crawldb crawl/linkdb \
    crawl/segments/*

echo "----- Dedup (Step 6 of $steps) -----"
$NUTCH_HOME/bin/nutch dedup crawl/NEWindexes

echo "----- Merge Indexes (Step 7 of $steps) -----"
$NUTCH_HOME/bin/nutch merge crawl/NEWindex crawl/NEWindexes

echo "----- Loading New Index (Step 8 of $steps) -----"
${CATALINA_HOME}/bin/shutdown.sh

if [ "$safe" != "yes" ]
then
  rm $RMARGS crawl/NEWindexes
  rm $RMARGS crawl/index
else
  rm $RMARGS crawl/BACKUPindexes
  rm $RMARGS crawl/BACKUPindex
  mv $MVARGS crawl/NEWindexes crawl/BACKUPindexes
  mv $MVARGS crawl/index crawl/BACKUPindex
fi

mv $MVARGS crawl/NEWindex crawl/index

${CATALINA_HOME}/bin/startup.sh

echo "runbot: FINISHED: Crawl completed!"
echo ""
