package com.applyrank.web;

import com.applyrank.dto.Dtos.ResumeAnalysisDto;
import com.applyrank.model.*;
import com.applyrank.repo.ResumeAnalysisRepository;
import com.applyrank.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {
    private final ResumeAnalyzer analyzer;
    private final ResumeAnalysisRepository repo;
    private final CurrentUser current;
    private final NotificationService notes;
    private final Path dir;

    public ResumeController(ResumeAnalyzer analyzer, ResumeAnalysisRepository repo, CurrentUser current,
                            NotificationService notes, @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.analyzer = analyzer; this.repo = repo; this.current = current; this.notes = notes;
        this.dir = Path.of(uploadDir);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeAnalysisDto upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "track", defaultValue = "fullstack") String track) throws IOException {
        if (file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a file.");
        User u = current.get();
        byte[] data = file.getBytes();
        String original = file.getOriginalFilename() == null ? "resume" : Path.of(file.getOriginalFilename()).getFileName().toString();

        ResumeAnalyzer.Analysis a;
        try {
            a = analyzer.analyze(analyzer.extractText(data, original), track);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read this file. Is it a valid PDF/DOCX?");
        }

        Files.createDirectories(dir);
        String ext = original.toLowerCase().endsWith(".docx") ? ".docx" : ".pdf";
        Path stored = dir.resolve(UUID.randomUUID() + ext);          // never use the client's file name on disk
        Files.write(stored, data);

        ResumeAnalysis r = new ResumeAnalysis();
        r.setUser(u);
        r.setFilename(original);
        r.setStoredPath(stored.toString());
        r.setTrack(track.toLowerCase());
        r.setScore(a.score());
        r.setMatched(Mapper.join(a.matched()));
        r.setMissing(Mapper.join(a.missing()));
        r.setTips(String.join("|", a.tips()));
        repo.save(r);
        notes.push(u, "updates", "Resume checked", "Your resume scored " + a.score() + "/100 for the " + track + " track.",
                "View", "link", "resume.html");
        return dto(r);
    }

    @GetMapping("/latest")
    public ResumeAnalysisDto latest() {
        return repo.findFirstByUserOrderByCreatedAtDesc(current.get()).map(this::dto)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No resume uploaded yet."));
    }

    private ResumeAnalysisDto dto(ResumeAnalysis r) {
        return new ResumeAnalysisDto(r.getId(), r.getFilename(), r.getTrack(), r.getScore(), Mapper.split(r.getMatched()),
                Mapper.split(r.getMissing()),
                r.getTips() == null || r.getTips().isBlank() ? java.util.List.of() : java.util.List.of(r.getTips().split("\\|")),
                r.getCreatedAt().toString());
    }
}
