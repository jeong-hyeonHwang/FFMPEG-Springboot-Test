package com.jhh.audiotest.controller;

import com.jhh.audiotest.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/audio/test")
@RequiredArgsConstructor
public class AudioTestController {

    private final AudioService audioService;
    private static final int REPETITIONS_S = 60;
    private static final int REPETITIONS_M = 20;

    /**
     * Flow1: 짧은 간격의 데이터(short_a.mp3, short_b.mp3)를 채널별로 각각 merge한 후, 그 두 채널을 mix
     * 최종 결과: 약 10분 분량의 결과 파일
     */
    @GetMapping("/flow1")
    public ResponseEntity<?> flow1() {
        try {
            long startTime = System.currentTimeMillis();

            // 채널 a 처리
            MockMultipartFile channelA = getMockFile("audio/short_a.mp3");
            String channelAPath = "./flow1_channelA.mp3";
            for (int i = 1; i < REPETITIONS_S; i++) {
                File mergedFileA = audioService.mergeFilesAndSave(channelA, getMockFile("audio/short_a.mp3"), channelAPath);
                byte[] mergedBytesA = Files.readAllBytes(mergedFileA.toPath());
                channelA = new MockMultipartFile("channelA", mergedFileA.getName(), "audio/mpeg", mergedBytesA);
            }

            // 채널 b 처리
            MockMultipartFile channelB = getMockFile("audio/short_b.mp3");
            String channelBPath = "./flow1_channelB.mp3";
            for (int i = 1; i < REPETITIONS_S; i++) {
                File mergedFileB = audioService.mergeFilesAndSave(channelB, getMockFile("audio/short_b.mp3"), channelBPath);
                byte[] mergedBytesB = Files.readAllBytes(mergedFileB.toPath());
                channelB = new MockMultipartFile("channelB", mergedFileB.getName(), "audio/mpeg", mergedBytesB);
            }

            // 최종 믹싱: 채널 a와 b를 mix
            String finalOutput = "./flow1_final_mix.mp3";
            File finalFile = audioService.mixFilesAndSave(channelA, channelB, finalOutput);
            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("Flow1 (채널별 merge 후 mix): %s, 시간: %d ms, 파일 크기: %d bytes",
                    finalOutput, duration, finalFile.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Flow1 실패: " + e.getMessage());
        }
    }

    /**
     * Flow2: 짧은 간격의 데이터를 각 쌍별로 먼저 mix한 후, 그 믹스 결과들을 merge하여 붙임
     * 사용 파일: short_a.mp3, short_b.mp3
     */
    @GetMapping("/flow2")
    public ResponseEntity<?> flow2() {
        try {
            long startTime = System.currentTimeMillis();

            // 첫 mix 결과 생성
            File mixSegmentFile = audioService.mixFilesAndSave(getMockFile("audio/short_a.mp3"), getMockFile("audio/short_b.mp3"), "./flow2_segment.mp3");
            byte[] mixSegmentBytes = Files.readAllBytes(mixSegmentFile.toPath());
            MockMultipartFile mixedSegment = new MockMultipartFile("mixed", mixSegmentFile.getName(), "audio/mpeg", mixSegmentBytes);

            String mixedSegmentPath = "./flow2_mergedMixed.mp3";
            for (int i = 1; i < REPETITIONS_S; i++) {
                File newMixSegmentFile = audioService.mixFilesAndSave(getMockFile("audio/short_a.mp3"), getMockFile("audio/short_b.mp3"), "./flow2_segment.mp3");
                byte[] newMixSegmentBytes = Files.readAllBytes(newMixSegmentFile.toPath());
                MockMultipartFile newMixedSegment = new MockMultipartFile("mixed", newMixSegmentFile.getName(), "audio/mpeg", newMixSegmentBytes);

                File mergedMixedFile = audioService.mergeFilesAndSave(mixedSegment, newMixedSegment, mixedSegmentPath);
                byte[] mergedMixedBytes = Files.readAllBytes(mergedMixedFile.toPath());
                mixedSegment = new MockMultipartFile("mergedMixed", mergedMixedFile.getName(), "audio/mpeg", mergedMixedBytes);
            }

            String finalOutput = "./flow2_final_merge.mp3";
            // 최종 결과는 이미 merge된 상태 저장됨
            File finalFile = new File(finalOutput);
            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("Flow2 (각 쌍별 mix 후 merge): %s, 시간: %d ms, 파일 크기: %d bytes",
                    finalOutput, duration, finalFile.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Flow2 실패: " + e.getMessage());
        }
    }

    /**
     * Flow3: 긴 간격의 데이터(mid_a.mp3, mid_b.mp3)를 merge한 후 mix
     * 사용 파일: mid_a.mp3, mid_b.mp3
     */
    @GetMapping("/flow3")
    public ResponseEntity<?> flow3() {
        try {
            long startTime = System.currentTimeMillis();

            // 채널 a 처리
            MockMultipartFile channelA = getMockFile("audio/mid_a.mp3");
            String channelAPath = "./flow3_channelA.mp3";
            for (int i = 1; i < REPETITIONS_M; i++) {
                File mergedFileA = audioService.mergeFilesAndSave(channelA, getMockFile("audio/mid_a.mp3"), channelAPath);
                byte[] mergedBytesA = Files.readAllBytes(mergedFileA.toPath());
                channelA = new MockMultipartFile("channelA", mergedFileA.getName(), "audio/mpeg", mergedBytesA);
            }

            // 채널 b 처리
            MockMultipartFile channelB = getMockFile("audio/mid_b.mp3");
            String channelBPath = "./flow3_channelB.mp3";
            for (int i = 1; i < REPETITIONS_M; i++) {
                File mergedFileB = audioService.mergeFilesAndSave(channelB, getMockFile("audio/mid_b.mp3"), channelBPath);
                byte[] mergedBytesB = Files.readAllBytes(mergedFileB.toPath());
                channelB = new MockMultipartFile("channelB", mergedFileB.getName(), "audio/mpeg", mergedBytesB);
            }

            // 최종 믹싱: 채널 a와 b mix
            String finalOutput = "./flow3_final_mix.mp3";
            File finalFile = audioService.mixFilesAndSave(channelA, channelB, finalOutput);
            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("Flow3 (긴 간격 데이터 merge 후 mix): %s, 시간: %d ms, 파일 크기: %d bytes",
                    finalOutput, duration, finalFile.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Flow3 실패: " + e.getMessage());
        }
    }

    /**
     * Flow4: 긴 간격의 데이터(mid_a.mp3, mid_b.mp3)를 먼저 각 쌍별로 mix한 후 merge
     * 사용 파일: mid_a.mp3, mid_b.mp3
     */
    @GetMapping("/flow4")
    public ResponseEntity<?> flow4() {
        try {
            long startTime = System.currentTimeMillis();

            // 첫 mix 결과 생성
            File mixSegmentFile = audioService.mixFilesAndSave(getMockFile("audio/mid_a.mp3"), getMockFile("audio/mid_b.mp3"), "./flow4_segment.mp3");
            byte[] mixSegmentBytes = Files.readAllBytes(mixSegmentFile.toPath());
            MockMultipartFile mixedSegment = new MockMultipartFile("mixed", mixSegmentFile.getName(), "audio/mpeg", mixSegmentBytes);

            String mixedSegmentPath = "./flow4_mergedMixed.mp3";
            for (int i = 1; i < REPETITIONS_M; i++) {
                File newMixSegmentFile = audioService.mixFilesAndSave(getMockFile("audio/mid_a.mp3"), getMockFile("audio/mid_b.mp3"), "./flow4_segment.mp3");
                byte[] newMixSegmentBytes = Files.readAllBytes(newMixSegmentFile.toPath());
                MockMultipartFile newMixedSegment = new MockMultipartFile("mixed", newMixSegmentFile.getName(), "audio/mpeg", newMixSegmentBytes);

                File mergedMixedFile = audioService.mergeFilesAndSave(mixedSegment, newMixedSegment, mixedSegmentPath);
                byte[] mergedMixedBytes = Files.readAllBytes(mergedMixedFile.toPath());
                mixedSegment = new MockMultipartFile("mergedMixed", mergedMixedFile.getName(), "audio/mpeg", mergedMixedBytes);
            }

            String finalOutput = "./flow4_final_merge.mp3";
            File finalFile = new File(finalOutput);
            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("Flow4 (긴 간격 데이터 mix 후 merge): %s, 시간: %d ms, 파일 크기: %d bytes",
                    finalOutput, duration, finalFile.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Flow4 실패: " + e.getMessage());
        }
    }

    /**
     * 단순 merge: 긴 길이 파일(long_a.mp3, long_b.mp3)을 이어붙임
     */
    @GetMapping("/merge")
    public ResponseEntity<?> mergeAndSave() {
        try {
            long startTime = System.currentTimeMillis();

            Resource res1 = new ClassPathResource("audio/long_a.mp3");
            Resource res2 = new ClassPathResource("audio/long_b.mp3");

            MockMultipartFile file1 = new MockMultipartFile("file1", res1.getFilename(), "audio/mpeg", res1.getInputStream());
            MockMultipartFile file2 = new MockMultipartFile("file2", res2.getFilename(), "audio/mpeg", res2.getInputStream());

            String outputPath = "./merged_output.mp3";
            File mergedFile = audioService.mergeFilesAndSave(file1, file2, outputPath);

            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("단순 merge 파일 저장 성공: %s (병합 소요 시간: %d ms), 파일 크기: %d bytes",
                    outputPath, duration, mergedFile.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("단순 merge 실패: " + e.getMessage());
        }
    }

    /**
     * 단순 mix: 긴 길이 파일(long_a.mp3, long_b.mp3)을 믹싱
     */
    @GetMapping("/mix")
    public ResponseEntity<?> mixAndSave() {
        try {
            long startTime = System.currentTimeMillis();

            Resource res1 = new ClassPathResource("audio/long_a.mp3");
            Resource res2 = new ClassPathResource("audio/long_b.mp3");

            MockMultipartFile file1 = new MockMultipartFile("file1", res1.getFilename(), "audio/mpeg", res1.getInputStream());
            MockMultipartFile file2 = new MockMultipartFile("file2", res2.getFilename(), "audio/mpeg", res2.getInputStream());

            String outputPath = "./mixed_output.mp3";
            File mixedFile = audioService.mixFilesAndSave(file1, file2, outputPath);

            long duration = System.currentTimeMillis() - startTime;
            String result = String.format("단순 mix 파일 저장 성공: %s (작업 소요 시간: %d ms), 파일 크기: %d bytes",
                    outputPath, duration, mixedFile.length());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("단순 mix 실패: " + e.getMessage());
        }
    }

    /**
     * 리소스 경로의 파일을 읽어 MockMultipartFile로 반환하는 헬퍼 메서드
     */
    private MockMultipartFile getMockFile(String resourcePath) throws IOException {
        Resource res = new ClassPathResource(resourcePath);
        return new MockMultipartFile("file", res.getFilename(), "audio/mpeg", res.getInputStream());
    }
}
