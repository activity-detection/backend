package com.actdet.backend.data.entities;


import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "video_details")
@Data
@NoArgsConstructor
public class VideoDetails {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne
    @JoinColumn(name = "video_id")
    @MapsId
    private Video video;

    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details_json", columnDefinition = "jsonb")
    private Details details;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Details{
        List<Detection> detections;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        static class Detection{
            String detectionLabel;
            DetectionTimestamp detectionTimestamp;


            record DetectionTimestamp(Duration from, Duration to){}
        }

    }
    /* TO-DO
    Zrobic entity do tabelki video_detections i ogarnac wewnatrz obsluge JSONB tak zeby
    sensownie moc zapisywac informacje okreslone przez model wykrywania żeby na podstawie JSONa np:
    - Móc określić w którym momencie filmiku wykryto zdarzenie (zeby np. w UI zaznaczyć ten moment na timelinie
    - (EWENTUALNIE JEZELI TO NIE BEDZIE ROBIONE PRZEZ MODEL) Móc zaznaczyć na video fragment obrazu gdzie wystepuje zdarzenie
     */
}
