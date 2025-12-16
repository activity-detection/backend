package com.actdet.backend.services;

import com.actdet.backend.data.entities.Video;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class VideoFilesDatabaseSyncService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WatchService watchService;
    private final Map<WatchKey, Path> pathMap;

    private final Path watchedDirectory;
    private final int rootDepth;
    private final int maxSubfolderDepth;

    private final ThreadPoolTaskExecutor taskExecutor;
    private volatile boolean running = false;

    private final VideoService videoService;

    @Autowired
    public VideoFilesDatabaseSyncService(@Value("${activity-detector.video.folderPath}") String relativeFolderPath,
                                         @Value("${activity-detector.video.subfolderDepth}") int subfolderDepth,
                                         VideoService videoService) throws IOException {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("VideoWatcher-");
        executor.setDaemon(true);
        executor.initialize();
        this.taskExecutor = executor;

        Path baseDir = Paths.get("").toAbsolutePath();
        this.watchedDirectory = baseDir.resolve(relativeFolderPath);


        this.rootDepth = StringUtils.countOccurrencesOf(this.watchedDirectory.toString(), "\\");
        this.maxSubfolderDepth = subfolderDepth;
        this.videoService = videoService;
        this.pathMap = new HashMap<>();

        syncData();

        this.watchService = this.watchedDirectory.getFileSystem().newWatchService();
        registerAll(this.watchedDirectory, subfolderDepth);
    }

    @PostConstruct
    public void startWatcher() {
        this.running=true;
        this.taskExecutor.execute(this::processEvents);
        logger.info("Watcher started.");
    }

    @PreDestroy
    public void stopWatcher() {
        this.running=false;
        try{
            this.watchService.close();
        } catch (IOException e) {
            logger.error("Error closing video files watcher.");
        }
        this.taskExecutor.shutdown();
        logger.info("Watcher stopped.");
    }

    protected void syncData() {
        long deletedRecordCount = videoService.deleteNonExistentVideoRecords();
        if(deletedRecordCount != 0){
            logger.info("""
                        Video record data synchronized.
                        #############################
                        Statistics:
                        Non existent records deleted: {}
                        #############################"""
                    , deletedRecordCount);
        }else{
            logger.info("Video record data up to date.");
        }
    }


    private void register(Path dirPath) throws IOException {
        WatchKey watchKey = dirPath.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );
        pathMap.put(watchKey, dirPath);
    }

    //Zwraca ilość zarejestrowanych folderow
    public void registerAll(Path dirPath, int subDirDepth) throws IOException {
        long registeredDirCount = 0;
        try(Stream<Path> fileStream = Files.walk(dirPath, subDirDepth)){
            registeredDirCount = fileStream
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        try{
                            register(path);
                            return true;
                        } catch (IOException e) {
                            logger.error("Could not register directory: {}", path);
                            return false;
                        }
                    }).count();
        }
        if(registeredDirCount <1) throw new IOException("Could not register any directories under data watcher!");
        logger.info("{} directories have been registered by data watcher.", registeredDirCount);
    }

    private void processEvents(){
        while (running){
            WatchKey key;
            try {
                key = watchService.take();
            } catch(ClosedWatchServiceException e){
                break;
            } catch(InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }

            Path dir = this.pathMap.get(key);
            if(dir == null) continue;

            for(WatchEvent<?> event : key.pollEvents()){
                WatchEvent.Kind<?> kind = event.kind();

                Path name = (Path) event.context();
                Path child = dir.resolve(name);

                if(kind == StandardWatchEventKinds.ENTRY_DELETE){
                    onFileDeleted(child);
                }else if(kind == StandardWatchEventKinds.ENTRY_CREATE){
                    onFileCreated(child);
                }
                //Musze pamietac ze zmiana nazwy pliku to najpierw dla watchera jest rownoznaczna
                //z najpierw DELETE a potem CREATE
            }

            boolean valid = key.reset();
            if (!valid) {
                pathMap.remove(key);
                if (pathMap.isEmpty()) {
                    logger.error("No directories are longer watched! Check if this is an appropriate behavior.");
                    break;
                }
            }

        }
        logger.info("Video files watcher stopped.");
    }

    private String getVideoRelativePathString(Path path){
        int depth = 0;
        int index = -1;
        String pathString = path.toString();
        while(depth<=this.rootDepth){
            index = pathString.indexOf('\\', index+1);
            if(index == -1) throw new IllegalArgumentException("Could not return video relative path string. File depth is incorrect.");
            depth++;
        }
        return pathString.substring(index+1);
    }


    private void onFileDeleted(Path deletedFilePath){
        if(Video.hasSupportedExtension(deletedFilePath)){
            logger.info("Usunieto plik video: {}", deletedFilePath);
            this.videoService.deleteVideoDatabaseRecord(getVideoRelativePathString(deletedFilePath));

        }
    }

    private void onFileCreated(Path createdFilePath){
        if(Files.isDirectory(createdFilePath)){
            int createdDirDepth = StringUtils.countOccurrencesOf(createdFilePath.toString(), "\\");
            if(this.maxSubfolderDepth>=createdDirDepth-this.rootDepth){
                try{
                    register(createdFilePath);
                } catch (IOException e) {
                    logger.error("Could not register new directory ({}) in data watcher.", createdFilePath);
                }
            }else{
                logger.warn("Directory {} was not registered as its depth exceeds subfolderDepth={}.", createdFilePath, this.maxSubfolderDepth);
            }
        }
    }





}
