\c activitydetectordb

CREATE TABLE videos (
                        video_id BIGINT GENERATED ALWAYS AS IDENTITY,
                        video_name VARCHAR(255) NOT NULL,
                        description TEXT,
                        upload_date TIMESTAMP NOT NULL DEFAULT NOW(),
                        video_path VARCHAR(255) NOT NULL UNIQUE
)