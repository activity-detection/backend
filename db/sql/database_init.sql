\c activitydetectordb

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE videos (
                        video_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        video_name VARCHAR(255) NOT NULL,
                        description TEXT,
                        upload_date TIMESTAMP NOT NULL DEFAULT NOW(),
                        video_path VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE video_details(
    video_id UUID PRIMARY KEY,
    details_json JSONB,
    CONSTRAINT fk_videos_video_detections FOREIGN KEY (video_id) REFERENCES videos(video_id) ON DELETE CASCADE
);