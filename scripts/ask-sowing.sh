#!/bin/env bash

# The host of farming service
HOST=127.0.0.1:8080
LAND_INFO_FILE=$(mktemp /tmp/lands.XXX)
LEAST_BLOCKS=5
MAXIMUM_BLOCKS=$(( 20 - $LEAST_BLOCKS ))

curl -s http://$HOST/lands >$LAND_INFO_FILE

land_ids=($(jq -r '[ .[].id ] | join(" ")' <$LAND_INFO_FILE))

declare -A suitable_crops
suitable_crops[1]="1 2 3"
suitable_crops[2]="4 5 6"
suitable_crops[3]="1 3 5 7 9"
suitable_crops[4]="1 2 4 6 8"
suitable_crops[5]="7 8 9"

sowing()
{
	local land_id=$1
	local climate=$(jq -r "map(select(.id == \"$land_id\"))[0].climate" <$LAND_INFO_FILE)
	local size=$(jq -r "map(select(.id == \"$land_id\"))[0].size" <$LAND_INFO_FILE)

	eval "crops=(${suitable_crops[$climate]})"
	local sowed_crop_idx=$(( $RANDOM % ${#crops[@]} ))
	local sowed_crop=${crops[$sowed_crop_idx]}
	local number_of_sowing_blocks=$(( $RANDOM % $MAXIMUM_BLOCKS + $LEAST_BLOCKS ))
	local comment="By ask-sowing.sh"

	local sowing_json="
		{
			\"asked_blocks\": $number_of_sowing_blocks,
			\"crop\": $sowed_crop,
			\"comment\": \"$comment\"
		}
	"
	sowing_json=$(tr '\n' ' '<<<"$sowing_json" | sed -Ee 's/\s+/ /g')

	printf "Sowing[%s]. %d blocks. Crop: %d \n" $land_id $number_of_sowing_blocks $sowed_crop

	curl -X POST -H "Content-Type: application/json" -d "$sowing_json" http://$HOST/land/$land_id/sow
}

for land_id in ${land_ids[@]}; do
	sowing $land_id
done
