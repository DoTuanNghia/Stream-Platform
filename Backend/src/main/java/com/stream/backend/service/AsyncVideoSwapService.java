package com.stream.backend.service;

import com.stream.backend.entity.Stream;
import com.stream.backend.entity.StreamSession;
import com.stream.backend.repository.StreamRepository;
import com.stream.backend.repository.StreamSessionRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service xử lý bất đồng bộ: tải video mới về VPS rồi Hot-Swap FFmpeg
 * cho luồng đang ACTIVE mà không làm treo UI.
 */
@Slf4j
@Service
public class AsyncVideoSwapService {

    private final FfmpegService ffmpegService;
    private final StreamRepository streamRepository;
    private final StreamSessionRepository streamSessionRepository;

    @Value("${stream.video.dir:D:\\\\videos}")
    private String videoDir;

    public AsyncVideoSwapService(
            FfmpegService ffmpegService,
            StreamRepository streamRepository,
            StreamSessionRepository streamSessionRepository) {
        this.ffmpegService = ffmpegService;
        this.streamRepository = streamRepository;
        this.streamSessionRepository = streamSessionRepository;
    }

    /**
     * Tải video mới về VPS rồi tự động swap FFmpeg cho luồng đang ACTIVE.
     * Chạy hoàn toàn trên thread nền (@Async), không block caller.
     *
     * @param streamId    ID của Stream cần swap
     * @param rawVideoUrl Link video mới (Google Drive, NAS, hoặc URL HTTP)
     */
    @Async
    public void downloadAndSwapVideo(Integer streamId, String rawVideoUrl) {
        log.info("[ASYNC-SWAP] Bắt đầu tải video mới cho streamId={}", streamId);

        try {
            // 1. Re-read stream từ DB (vì @Async chạy trên thread khác, entity cũ detached)
            Stream stream = streamRepository.findById(streamId).orElse(null);
            if (stream == null) {
                log.error("[ASYNC-SWAP] Stream không tồn tại: streamId={}", streamId);
                return;
            }

            // 2. Kiểm tra session vẫn đang ACTIVE
            StreamSession session = streamSessionRepository
                    .findTopByStreamIdOrderByIdDesc(streamId).orElse(null);
            if (session == null || !"ACTIVE".equalsIgnoreCase(session.getStatus())) {
                log.warn("[ASYNC-SWAP] Stream {} không còn ACTIVE, hủy swap", streamId);
                return;
            }

            String streamKey = stream.getKeyStream();
            if (streamKey == null || streamKey.isBlank()) {
                log.error("[ASYNC-SWAP] Stream key trống cho streamId={}", streamId);
                return;
            }

            // 3. Chuẩn hóa link (Google Drive → direct download URL)
            String downloadUrl = normalizeVideoSource(rawVideoUrl);

            // 4. Xác định video mới là file local sẵn có hay cần tải về
            String newLocalPath;

            if (isLocalFile(downloadUrl)) {
                // File local hoặc NAS path → kiểm tra tồn tại
                File localFile = new File(downloadUrl);
                if (!localFile.exists()) {
                    log.error("[ASYNC-SWAP] File local không tồn tại: {}", downloadUrl);
                    return;
                }
                newLocalPath = localFile.getAbsolutePath();
                log.info("[ASYNC-SWAP] Sử dụng file local có sẵn: {}", newLocalPath);
            } else {
                // URL → tải về thư mục D:\videos
                String fileName = generateAutoFileName();
                File destFile = new File(videoDir, fileName);

                // Đảm bảo thư mục tồn tại
                File dir = new File(videoDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                log.info("[ASYNC-SWAP] Đang tải từ {} về {}", downloadUrl, destFile.getAbsolutePath());
                downloadFile(downloadUrl, destFile);
                log.info("[ASYNC-SWAP] Tải hoàn tất: {} ({} MB)",
                        destFile.getAbsolutePath(), destFile.length() / (1024 * 1024));

                newLocalPath = destFile.getAbsolutePath();
            }

            // 5. Kiểm tra lại session vẫn ACTIVE trước khi swap (tránh race condition)
            session = streamSessionRepository.findTopByStreamIdOrderByIdDesc(streamId).orElse(null);
            if (session == null || !"ACTIVE".equalsIgnoreCase(session.getStatus())) {
                log.warn("[ASYNC-SWAP] Stream {} không còn ACTIVE sau khi tải xong, hủy swap", streamId);
                return;
            }

            // 6. Lưu đường dẫn video mới vào Database
            // Xóa video cũ trước nếu là file local khác file mới
            String oldVideoList = stream.getVideoList();
            if (oldVideoList != null && !oldVideoList.isBlank()
                    && !oldVideoList.equals(newLocalPath) && isLocalFile(oldVideoList)) {
                try {
                    File oldFile = new File(oldVideoList.trim());
                    if (oldFile.exists()) {
                        oldFile.delete();
                        log.info("[ASYNC-SWAP] Đã xóa video cũ: {}", oldVideoList);
                    }
                } catch (Exception e) {
                    log.warn("[ASYNC-SWAP] Lỗi xóa video cũ: {}", e.getMessage());
                }
            }

            stream.setVideoList(newLocalPath);
            streamRepository.save(stream);

            // 7. HOT-SWAP: Kill FFmpeg cũ → Start FFmpeg mới
            log.info("[ASYNC-SWAP] Đang Hot-Swap FFmpeg cho streamKey={}", streamKey);

            ffmpegService.stopStream(streamKey);

            // Đợi 2 giây để process cũ giải phóng hoàn toàn
            Thread.sleep(2000);

            ffmpegService.startStream(newLocalPath, null, streamKey);

            log.info("[ASYNC-SWAP] ✅ Hot-Swap thành công! streamId={}, video={}", streamId, newLocalPath);

        } catch (Exception e) {
            log.error("[ASYNC-SWAP] ❌ Lỗi trong quá trình tải/swap video cho streamId={}: {}",
                    streamId, e.getMessage(), e);
        }
    }

    // ======================== HELPER METHODS ========================

    /**
     * Tải file từ URL về đĩa
     */
    private void downloadFile(String fileUrl, File destFile) throws Exception {
        if (isLocalFile(fileUrl)) {
            // NAS path → copy file
            java.nio.file.Files.copy(
                    new File(fileUrl).toPath(),
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        URL url = new URL(fileUrl);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(300000); // 5 phút cho file lớn

        try (InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            long lastLogTime = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                // Log tiến trình mỗi 10 giây
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 10000) {
                    log.info("[ASYNC-SWAP] Đang tải... {} MB", totalRead / (1024 * 1024));
                    lastLogTime = now;
                }
            }
        }
    }

    /**
     * Chuẩn hóa link Google Drive thành direct download URL
     */
    private String normalizeVideoSource(String raw) {
        if (raw == null)
            return null;
        raw = raw.trim();

        Pattern p = Pattern.compile("https?://drive\\.google\\.com/file/d/([^/]+)/view.*");
        Matcher m = p.matcher(raw);
        if (m.matches()) {
            return "https://drive.google.com/uc?export=download&id=" + m.group(1);
        }
        return raw;
    }

    /**
     * Kiểm tra xem đường dẫn có phải file local (không phải URL)
     */
    private boolean isLocalFile(String path) {
        if (path == null || path.isBlank())
            return false;
        return !path.startsWith("http://") && !path.startsWith("https://");
    }

    /**
     * Tự sinh tên file tránh trùng: swap_HHmmss_ddMMyy.mp4
     */
    private String generateAutoFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HHmmss_ddMMyy");
        return "swap_" + now.format(fmt) + ".mp4";
    }
}
