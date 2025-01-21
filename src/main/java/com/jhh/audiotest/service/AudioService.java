package com.jhh.audiotest.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class AudioService {

    /**
     * 두 오디오 파일을 이어붙여 병합한 후 지정된 경로에 저장하고, 해당 파일을 반환.
     * (파일 복사 시 기존 파일이 있다면 덮어쓰기)
     */
    public File mergeFilesAndSave(MultipartFile file1, MultipartFile file2, String outputPath) throws Exception {
        // 임시 디렉토리 생성
        Path tempDir = Files.createTempDirectory("audio-merge-");
        Path source1 = tempDir.resolve(file1.getOriginalFilename());
        Path source2 = tempDir.resolve(file2.getOriginalFilename());

        // 업로드된 파일을 임시 위치에 저장
        file1.transferTo(source1.toFile());
        file2.transferTo(source2.toFile());

        // 병합 리스트 파일 생성 (concat demuxer 사용)
        Path concatList = tempDir.resolve("concat_list.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(concatList)) {
            writer.write("file '" + source1.toAbsolutePath().toString() + "'\n");
            writer.write("file '" + source2.toAbsolutePath().toString() + "'\n");
        }

        // FFmpeg 명령어 구성 (단순 이어붙이기)
        Path mergedTempOutput = tempDir.resolve("merged_output.mp3");
        String[] command = {
                "ffmpeg",
                "-y",                   // 출력 파일 덮어쓰기
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.toAbsolutePath().toString(),
                "-c", "copy",
                mergedTempOutput.toAbsolutePath().toString()
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // FFmpeg 로그 출력 (옵션)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                System.out.println("[FFmpeg merge] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 병합 실패. Exit code: " + exitCode);
        }

        // 최종 출력 파일 복사 (지정 경로에, 기존 파일이 있다면 덮어쓰기)
        File finalOutputFile = new File(outputPath);
        Files.copy(mergedTempOutput, finalOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // 임시 파일 및 디렉토리 정리 (필요에 따라)
        Files.deleteIfExists(source1);
        Files.deleteIfExists(source2);
        Files.deleteIfExists(concatList);
        Files.deleteIfExists(mergedTempOutput);
        Files.deleteIfExists(tempDir);

        return finalOutputFile;
    }

    /**
     * 두 오디오 파일을 믹싱하여 하나의 파일로 만든 후 지정한 경로에 저장하고 반환.
     * 믹싱 시 두 파일의 시작 시점을 맞추며, 출력 길이는 더 긴 파일에 맞춥니다.
     */
    public File mixFilesAndSave(MultipartFile file1, MultipartFile file2, String outputPath) throws Exception {
        // 임시 디렉토리 생성
        Path tempDir = Files.createTempDirectory("audio-mix-");
        Path source1 = tempDir.resolve(file1.getOriginalFilename());
        Path source2 = tempDir.resolve(file2.getOriginalFilename());

        // 업로드된 파일을 임시 위치에 저장
        file1.transferTo(source1.toFile());
        file2.transferTo(source2.toFile());

        // 출력 파일 경로 (임시 디렉토리 내에 믹스된 결과물 생성)
        Path mixedTempOutput = tempDir.resolve("mixed_output.mp3");

        // FFmpeg 명령어 구성 - amix 필터 사용 (두 입력의 시작 시점을 일치시키고, duration=longest)
        String[] command = {
                "ffmpeg",
                "-y",                                   // 출력 파일 덮어쓰기
                "-i", source1.toAbsolutePath().toString(),
                "-i", source2.toAbsolutePath().toString(),
                "-filter_complex", "amix=inputs=2:duration=longest:dropout_transition=0",
                mixedTempOutput.toAbsolutePath().toString()
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // FFmpeg 로그 출력 (옵션)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                System.out.println("[FFmpeg mix] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg 믹싱 실패. Exit code: " + exitCode);
        }

        // 최종 출력 파일 복사 (지정 경로에, 기존 파일이 있으면 덮어쓰기)
        File finalOutputFile = new File(outputPath);
        Files.copy(mixedTempOutput, finalOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // 임시 파일 정리 (필요시)
        Files.deleteIfExists(source1);
        Files.deleteIfExists(source2);
        Files.deleteIfExists(mixedTempOutput);
        Files.deleteIfExists(tempDir);

        return finalOutputFile;
    }
}
