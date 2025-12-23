package com.actdet.backend.web.controllers.bodies;

import com.actdet.backend.data.entities.Video;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResponseVideoBody {
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime upload_date;

    public ResponseVideoBody(Video video) {
        this.id = video.getId();
        this.name = video.getName();
        this.description = video.getDescription();
        this.upload_date = video.getUpload_date();
    }
}
