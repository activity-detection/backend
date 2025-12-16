package com.actdet.backend.data.entities;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "videos")
public class Video {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("mp4", "mov");

    public static boolean hasSupportedExtension(Path path) {
        return hasSupportedExtension(path.getFileName().toString());
    }

    public static boolean hasSupportedExtension(String pathString) {
        int dotIndex = pathString.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "" : pathString.substring(dotIndex + 1);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long id;

    @Column(name = "video_name", length = 255, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "upload_date", insertable = false, updatable = false)
    private LocalDateTime upload_date;

    @Column(name = "video_path", length = 255, nullable = false, unique = true)
    private String pathToFile;

    @Builder
    public Video(String name, String description, String pathToFile) {
        this.name = name;
        this.description = description;
        this.pathToFile = pathToFile;
    }
}
