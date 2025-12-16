package com.actdet.backend.services;

import com.actdet.backend.data.entities.Video;
import com.actdet.backend.services.utils.VideoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class VideoStorageService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final VideoService videoService;

    public VideoStorageService(VideoService videoService) {
        this.videoService = videoService;
    }

    public ResourceRegion getVideoResourceRegion(String fileIdentifier, HttpHeaders headers) {
        Resource videoMedia = new FileSystemResource(videoService.getVideoPathForIdentifier(fileIdentifier));
        return getVideoResourceRegion(videoMedia, headers);
    }

    private ResourceRegion getVideoResourceRegion(Resource media, HttpHeaders headers){
        List<HttpRange> rangeList = headers.getRange();
        HttpRange range;
        if(rangeList.isEmpty()){
            logger.error("Missing range header! Result not returned.");
            throw new RuntimeException("Missing range header! Result not returned.");
        } else if (rangeList.size()>1) logger.warn("Header has more than one range. Check if this is an appropriate behaviour.");
        range = rangeList.getFirst();
        return range.toResourceRegion(media);
    }

    public void store(MultipartFile file, String videoName, String description, Path filePathToSaveIn){
        if(file.isEmpty()){
            throw new RuntimeException("Cannot save empty file");
        }
        if(VideoUtils.getFileDepth(filePathToSaveIn)>videoService.getMaxDepth()){
            throw new RuntimeException("Specified store path is deeper than allowed in config file.");
        }
        if(!Video.hasSupportedExtension(filePathToSaveIn)){
            throw new RuntimeException("File extension is not supported.");
        }
        String timestampString = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Path timestampedDirPath = this.videoService.getVideoFolderPath().resolve(Paths.get(timestampString));
        Path storePath = timestampedDirPath.resolve(filePathToSaveIn);

        try(InputStream inputStream = file.getInputStream()){
            Files.createDirectories(timestampedDirPath);
            Files.copy(inputStream, storePath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store video file.");
        }
        this.videoService.saveVideoDatabaseRecord(videoName, description, Paths.get(timestampString).resolve(filePathToSaveIn));
        logger.debug("Dodano plik video: {}", storePath);

    }


}
