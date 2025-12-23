package com.actdet.backend.services;

import com.actdet.backend.data.entities.VideoDetails;
import com.actdet.backend.data.repositories.VideoDetailsRepository;
import com.actdet.backend.data.repositories.VideoRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    VideoRepository videoRepository;
    @Mock
    VideoDetailsRepository videoDetailsRepository;


    VideoService videoService;

    @BeforeEach
    void setUp(){
        this.videoService = new VideoService("", 1, videoRepository, videoDetailsRepository);
    }


    @Test
    void getVideoPathForIdentifier_ShouldReturnFilePath_IfRecordForIdExists(){
        // given
        UUID uuid = UUID.randomUUID();
        String path = "/path/to/file";
        Path baseDir = Paths.get("").toAbsolutePath();

        when(videoRepository.getPathById(uuid)).thenReturn(Optional.of(path));
        // when
        Path result = videoService.getVideoPathForIdentifier(uuid.toString());

        // then
        assertEquals(baseDir.resolve(Paths.get(path)), result);
    }
}