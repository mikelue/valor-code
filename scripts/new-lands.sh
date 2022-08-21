#!/bin/env bash

# The host of farming service
HOST=127.0.0.1:8080

# ==================================================
# Sample lands
# ==================================================
declare -A land_info

land_info["KZC-01"]=80000
land_info["KZC-02"]=7000
land_info["KZC-03"]=10000
land_info["KZC-04"]=5000
land_info["KZC-05"]=80000
land_info["UIH-01"]=2300
land_info["UIH-02"]=3200
land_info["UIH-03"]=3200
land_info["UIH-04"]=4600
land_info["UIH-05"]=1200
# ================================================== :~)

CLIMATES_NUMBER=5

add_land()
{
	local name="$1"
	local size="$2"
	local climate=$(( $RANDOM % $CLIMATES_NUMBER + 1 ))

	printf "Add land. Name: %s. Size: %d. Climate: %d.\n" $name $size $climate

	curl -X POST -H "Content-Type: application/json" -d "{ \"name\": \"$name\", \"size\": $size, \"climate\": $climate }" http://$HOST/land

	echo -e "\n"
}

for key in "${!land_info[@]}"; do
	add_land $key ${land_info[$key]}
done
