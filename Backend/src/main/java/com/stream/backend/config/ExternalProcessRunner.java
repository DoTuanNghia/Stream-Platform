package com.stream.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Component để chạy file EXE bên ngoài cùng với Backend.
 * - Khi Backend khởi động: EXE sẽ được chạy
 * - Khi Backend dừng (Ctrl+C): EXE sẽ tự động bị tắt
 */
@Component
@Slf4j
public class ExternalProcessRunner {

    // Đường dẫn đến file EXE cần chạy
    private static final String EXE_PATH = "D:\\TCGVPSdownload-v1.exe";
    private static final String EXE_NAME = "TCGVPSdownload-v1.exe";

    @PostConstruct
    public void startExternalProcess() {
        try {
            log.info("Đang khởi động external process: {}", EXE_PATH);

            // Mở EXE với cửa sổ hiển thị
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd", "/c", "start", "", EXE_PATH);
            processBuilder.start();

            log.info("External process đã được khởi động thành công");

            // Đăng ký shutdown hook để tắt EXE khi Ctrl+C
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    log.info("Đang dừng external process...");
                    new ProcessBuilder("taskkill", "/F", "/IM", EXE_NAME).start().waitFor();
                    log.info("External process đã dừng");
                } catch (Exception e) {
                    log.error("Lỗi khi dừng external process: {}", e.getMessage());
                }
            }));

        } catch (IOException e) {
            log.error("Không thể khởi động external process: {}", e.getMessage());
        }
    }
}
