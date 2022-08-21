#!/bin/env bash

# The host of farming service
HOST=127.0.0.1:8080
LAND_INFO_FILE=$(mktemp /tmp/lands.XXX)
LEAST_BLOCKS=5
MAXIMUM_BLOCKS=$(( 20 - $LEAST_BLOCKS ))

curl -s http://$HOST/lands >$LAND_INFO_FILE

land_ids=($(jq -r '[ .[].id ] | join(" ")' <$LAND_INFO_FILE))

cleaning()
{
	local land_id=$1
	local climate=$(jq -r "map(select(.id == \"$land_id\"))[0].climate" <$LAND_INFO_FILE)
	local size=$(jq -r "map(select(.id == \"$land_id\"))[0].size" <$LAND_INFO_FILE)

	local number_of_cleaning_blocks=$(( $RANDOM % $MAXIMUM_BLOCKS + $LEAST_BLOCKS ))
	local comment="By ask-cleaning.sh"

	local cleaning_json="
		{
			\"asked_blocks\": $number_of_cleaning_blocks,
			\"comment\": \"$comment\"
		}
	"
	cleaning_json=$(tr '\n' ' '<<<"$cleaning_json" | sed -Ee 's/\s+/ /g')

	printf "Cleaning[%s]. %d blocks. Crop: %d \n" $land_id $number_of_cleaning_blocks $cleaned_crop

	curl -X POST -H "Content-Type: application/json" -d "$cleaning_json" http://$HOST/land/$land_id/clean
}

for land_id in ${land_ids[@]}; do
	cleaning $land_id
done
