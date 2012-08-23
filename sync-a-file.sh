#!/bin/bash
for f in "$@"
do rsync -avz -e ssh "$f" 150.203.163.24:/scratch/backups/backup-home-meandre/meandre-changed-src.1.4.11;
done
