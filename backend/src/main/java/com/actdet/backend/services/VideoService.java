package com.actdet.backend.services;

import com.actdet.backend.data.entities.Video;
import com.actdet.backend.data.repositories.VideoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class VideoService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Path videoFolderPath;
    private final int maxDepth;
    private final VideoRepository videoRepository;

    @Autowired
    public VideoService(@Value("${activity-detector.video.folderPath}") String relativeFolderPath,
                        @Value("${activity-detector.video.subfolderDepth}") int subfolderDepth,
                        VideoRepository videoRepository) {
        this.maxDepth = subfolderDepth;
        this.videoRepository = videoRepository;
        //Aktualnie sciezka do katalogu jest wzgledem katalogu w ktorym uruchamiamy projekt
        Path baseDir = Paths.get("").toAbsolutePath();

        this.videoFolderPath =  baseDir.resolve(relativeFolderPath);
        logger.info("IdentifierToVideoMapperService has been initialized. Video files will be read from: {}", this.videoFolderPath);
    }

    public Path getVideoPathForIdentifier(String videoIdentifier){
        String fileName = getFilePathForId(videoIdentifier);
        return videoFolderPath.resolve(fileName);
    }

    private String getFilePathForId(String id){
        return videoRepository.getPathById(Long.parseLong(id))
                .orElseThrow(() -> new RuntimeException("Plik z podanym id ("+id+") nie istnieje!"));
        //Potem musze lepiej obsłużyć ten wyjątek
    }

    public void saveVideoDatabaseRecord(String videoName, Path videoPath){
        saveVideoDatabaseRecord(videoName, null, videoPath);
    }

    public void saveVideoDatabaseRecord(String videoName, String description, Path videoPath){
        String videoPathString = videoPath.toString();
        Video video = Video.builder().name(videoName).description(description).pathToFile(videoPathString).build();
        if(videoRepository.existsVideoByPathToFile(videoPathString)){
            return;
        }
        videoRepository.save(video);
        logger.debug("Record saved to database: {}", video);
    }

    @Transactional
    public void deleteVideoDatabaseRecord(String videoPath){
        videoRepository.deleteVideoByPathToFile(videoPath);
    }

    public boolean isVideoRecordRegistered(String videoPath){
        return videoRepository.existsVideoByPathToFile(videoPath);
    }


    @Transactional
    public long deleteNonExistentVideoRecords(){
        AtomicLong deletedRecordsCount = new AtomicLong();
        try(Stream<String> stream = videoRepository.streamAllVideoPaths()){
            stream.forEach(path -> {
                if(!Files.isRegularFile(this.videoFolderPath.resolve(path))){
                    videoRepository.deleteVideoByPathToFile(path);
                    deletedRecordsCount.getAndIncrement();
                }
            });
        }
        return deletedRecordsCount.get();
    }

    public Path getVideoFolderPath(){return this.videoFolderPath;}
    public int getMaxDepth(){return this.maxDepth;}


}
