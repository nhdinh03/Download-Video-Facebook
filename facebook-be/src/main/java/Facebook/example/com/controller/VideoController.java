package Facebook.example.com.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VideoController {

    @PostMapping("/download")
    public ResponseEntity<InputStreamResource> downloadVideo(@RequestBody Map<String, String> payload) throws IOException, InterruptedException {
        String fbUrl = payload.get("url");
        String videoPath = FacebookVideoUtil.downloadVideoUsingYtDlp(fbUrl);

        File videoFile = new File(videoPath);
        videoFile.deleteOnExit(); // Đảm bảo file sẽ được xóa khi JVM kết thúc
        InputStreamResource resource = new InputStreamResource(new FileInputStream(videoFile));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=facebook_video.mp4")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(videoFile.length())
                .body(resource);
    }

    @PostMapping("/preview")
    public ResponseEntity<String> previewVideo(@RequestBody Map<String, String> payload) throws IOException, InterruptedException {
        String fbUrl = payload.get("url");
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "best", "-g", fbUrl);
        pb.redirectErrorStream(true);
        Process process = null;
        String directUrl = "";
        int exitCode = -1;
        try {
            process = pb.start();
            try (java.io.InputStream is = process.getInputStream()) {
                directUrl = new String(is.readAllBytes()).trim();
            }
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // re-interrupt thread
            return ResponseEntity.status(500).body("Quá trình lấy link video bị gián đoạn: " + e.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + ex.getMessage());
        } finally {
            if (process != null) process.destroyForcibly();
        }
        if (exitCode == 0 && directUrl.startsWith("http")) {
            return ResponseEntity.ok(directUrl);
        } else {
            return ResponseEntity.status(500).body("Không lấy được link video trực tiếp. Mã thoát: " + exitCode + ", output: " + directUrl);
        }
    }
}
