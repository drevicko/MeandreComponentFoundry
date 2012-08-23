#!/bin/bash
for f in "$@"
do 
   echo $f
   jar tf $f | grep meandre
done
