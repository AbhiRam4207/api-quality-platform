package api_quality_platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ChangelogService {

    private static final Logger log = LoggerFactory.getLogger(ChangelogService.class);
    private static final String CHANGELOG_PATH = "changelog.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(String action, String details) {
        String entry = "[" + LocalDateTime.now().format(FORMATTER) + "] " + action + " | " + details;
        try (PrintWriter writer = new PrintWriter(new FileWriter(CHANGELOG_PATH, true))) {
            writer.println(entry);
        } catch (IOException e) {
            log.error("Failed to write to changelog: {}", e.getMessage());
        }
    }
}
