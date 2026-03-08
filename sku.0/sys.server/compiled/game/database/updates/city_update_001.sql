-- ============================================================================
-- City Gameplay & Landscaping Update - Database Schema
-- ============================================================================
-- Run this script to add new tables for city enhancements
-- ============================================================================

-- City Terrain Regions (radius painting and roads)
CREATE TABLE city_terrain_regions (
    city_id NUMBER NOT NULL,
    region_id VARCHAR2(64) NOT NULL,
    region_type VARCHAR2(16) NOT NULL, -- 'RADIUS' or 'ROAD'
    center_x NUMBER,
    center_z NUMBER,
    radius NUMBER,
    -- Road-specific fields
    start_x NUMBER,
    start_z NUMBER,
    end_x NUMBER,
    end_z NUMBER,
    road_width NUMBER,
    -- Common fields
    shader_template VARCHAR2(256),
    affector_type VARCHAR2(64),
    layer_data BLOB,
    created_time NUMBER,
    CONSTRAINT pk_city_terrain PRIMARY KEY (city_id, region_id)
);

CREATE INDEX idx_city_terrain_city ON city_terrain_regions(city_id);

-- City Bulldoze State (persisted terrain flattening)
CREATE TABLE city_bulldoze (
    city_id NUMBER PRIMARY KEY,
    bulldozed_height NUMBER NOT NULL,
    bulldozed_time NUMBER NOT NULL,
    edge_blend_distance NUMBER DEFAULT 20
);

-- City Evictions
CREATE TABLE city_evictions (
    eviction_id NUMBER PRIMARY KEY,
    city_id NUMBER NOT NULL,
    citizen_id NUMBER NOT NULL,
    initiated_by NUMBER NOT NULL,
    initiated_time NUMBER NOT NULL,
    reason VARCHAR2(512),
    status VARCHAR2(32), -- PENDING, APPEALED, UPHELD, REVERSED, COMPLETED, CANCELLED
    appeal_filed_time NUMBER,
    appeal_defense VARCHAR2(1024),
    judge_id NUMBER,
    judge_decision_time NUMBER,
    judge_notes VARCHAR2(512)
);

CREATE INDEX idx_city_evictions_city ON city_evictions(city_id);
CREATE INDEX idx_city_evictions_citizen ON city_evictions(citizen_id);

CREATE SEQUENCE city_eviction_seq START WITH 1 INCREMENT BY 1;

-- City Judges (ELECTED)
CREATE TABLE city_judges (
    city_id NUMBER NOT NULL,
    citizen_id NUMBER NOT NULL,
    elected_time NUMBER NOT NULL,
    term_expires NUMBER NOT NULL,
    votes_received NUMBER NOT NULL,
    CONSTRAINT pk_city_judges PRIMARY KEY (city_id, citizen_id)
);

CREATE INDEX idx_city_judges_city ON city_judges(city_id);

-- Judge Election History
CREATE TABLE city_judge_elections (
    election_id NUMBER PRIMARY KEY,
    city_id NUMBER NOT NULL,
    start_time NUMBER NOT NULL,
    end_time NUMBER NOT NULL,
    total_votes NUMBER,
    status VARCHAR2(32) -- ACTIVE, COMPLETED, CANCELLED
);

CREATE INDEX idx_city_elections_city ON city_judge_elections(city_id);

CREATE SEQUENCE city_election_seq START WITH 1 INCREMENT BY 1;

-- Judge Vote Records (for audit/recount)
CREATE TABLE city_judge_votes (
    election_id NUMBER NOT NULL,
    voter_id NUMBER NOT NULL,
    candidate_id NUMBER NOT NULL,
    vote_time NUMBER NOT NULL,
    CONSTRAINT pk_judge_votes PRIMARY KEY (election_id, voter_id)
);

-- City Extended Taxes
CREATE TABLE city_extended_taxes (
    city_id NUMBER PRIMARY KEY,
    crafting_tax NUMBER DEFAULT 0,
    vendor_license_fee NUMBER DEFAULT 0,
    structure_placement_fee NUMBER DEFAULT 0,
    event_permit_fee NUMBER DEFAULT 0,
    starship_landing_tax NUMBER DEFAULT 0
);

-- ============================================================================
-- Version Update
-- ============================================================================
-- Update database version number (adjust as needed for your versioning)
-- UPDATE version_number SET version_number = version_number + 1 WHERE id = 1;

COMMIT;

