/*
CREATE UDT and table
*/

CREATE TYPE udt_block (
	bl_crop tinyint,
	bl_sow_time timestamp,
	bl_mature_time timestamp,
	bl_comment text,
	bl_harvest_amount smallint,
	bl_update_time timestamp
);

CREATE TABLE vc_land_log_by_time(
	ll_ld_id UUID,
	ll_time timestamp,
	ll_bl_id smallint,
	ll_activity tinyint,
	ll_used_time_second smallint,
	ll_bl_payload frozen<udt_block>,
	PRIMARY KEY(ll_ld_id, ll_time, ll_bl_id)
) WITH
	CLUSTERING ORDER BY (ll_time DESC, ll_bl_id ASC);
