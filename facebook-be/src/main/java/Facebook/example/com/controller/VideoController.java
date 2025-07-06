package Facebook.example.com.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VideoController {

    @GetMapping(value = "/download/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDownload(@RequestParam String url) {
        SseEmitter emitter = new SseEmitter(300_000L);

        new Thread(() -> {
            try {
                String filename = FacebookVideoUtil.downloadVideoUsingYtDlp(url, progress -> {
                    try {
                        emitter.send(SseEmitter.event().data(progress));
                    } catch (IOException e) {
                        System.err.println("Client disconnected: " + e.getMessage());
                    }
                });

                emitter.send(SseEmitter.event().data("DONE_" + filename));
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("ERROR_" + e.getMessage()));
                } catch (IOException ignored) {}
            } finally {
                emitter.complete();
            }
        }).start();

        return emitter;
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadVideo(@RequestParam String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) return ResponseEntity.notFound().build();

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/preview")
    public ResponseEntity<String> previewVideo(@RequestBody Map<String, String> payload) throws IOException {
        String fbUrl = payload.get("url");
        if (fbUrl == null || !fbUrl.matches("https?://(www\\.)?(facebook\\.com|fb\\.watch|fb\\.com)/.*")) {
            return ResponseEntity.badRequest().body("URL không hợp lệ.");
        }

        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "b", "-g", fbUrl);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String directUrl = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("http")) {
                    directUrl = line.trim();
                    break;
                }
            }
        }

        try {
            int exit = process.waitFor();
            if (exit != 0 || directUrl == null || !directUrl.contains(".mp4")) {
                return ResponseEntity.status(500).body("Không thể lấy link xem trước.");
            }
            return ResponseEntity.ok(directUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Lỗi hệ thống.");
        }
    }
}
