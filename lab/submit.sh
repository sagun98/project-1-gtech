#!/usr/bin/env bash

function verify() {
	arr=("$@")
	for i in "${arr[@]}";
		do
				if [ ! -f $i ]; then

					echo "Missing ${i}"
					exit 1
				fi
		done
}

req_files=("src/dslabs.pingpong/PingApplication.java" "src/dslabs.pingpong/Messages.java" "src/dslabs.pingpong/Timers.java" "src/dslabs.pingpong/PingClient.java" "src/dslabs.pingpong/PingServer.java" "REPORT.md")
verify "${req_files[@]}"
if [[ $? -ne 0 ]]; then
    exit 1
fi

if [ $# -eq 1 ]
then
	zip "${1}.zip" src/dslabs.pingpong/PingApplication.java src/dslabs.pingpong/Messages.java src/dslabs.pingpong/Timers.java src/dslabs.pingpong/PingClient.java src/dslabs.pingpong/PingServer.java REPORT.md
else
	echo 'Please provide your GTID, eg ./submit.sh syi73'
fi
