package com.applyrank.web;

import com.applyrank.dto.Dtos.ProfileDto;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private static final int MAX_PHOTO_CHARS = 400_000;     // ~300 KB image as base64

    private final ProfileRepository profiles;
    private final UserRepository users;
    private final CurrentUser current;
    private final Mapper mapper;
    private final ObjectMapper json;

    public ProfileController(ProfileRepository profiles, UserRepository users, CurrentUser current, Mapper mapper, ObjectMapper json) {
        this.profiles = profiles; this.users = users; this.current = current; this.mapper = mapper; this.json = json;
    }

    private Profile load(User u) {
        return profiles.findByUser(u).orElseGet(() -> {
            Profile p = new Profile();
            p.setUser(u);
            return profiles.save(p);
        });
    }

    @GetMapping
    public ProfileDto get() {
        User u = current.get();
        return mapper.profile(u, load(u));
    }

    @PutMapping
    public ProfileDto save(@RequestBody ProfileDto d) {
        User u = current.get();
        Profile p = load(u);
        if (d.photo() != null && d.photo().length() > MAX_PHOTO_CHARS)
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Profile photo is too large (max ~300 KB).");
        if (d.name() != null && !d.name().isBlank()) { u.setName(d.name().trim()); users.save(u); }
        p.setHeadline(d.headline());
        p.setPhone(d.phone());
        p.setLocation(d.location());
        p.setAbout(d.about());
        p.setStatus("experienced".equals(d.status()) ? "experienced" : "fresher");
        p.setExpYears(d.expYears() == null ? 0.0 : d.expYears());
        p.setCompany(d.company());
        p.setJobRole(d.role());
        p.setExpDesc(d.expDesc());
        p.setDegree(d.degree());
        p.setUniversity(d.university());
        p.setGradYear(d.gradYear());
        p.setCgpa(d.cgpa());
        p.setSkills(d.skills());
        p.setGithub(d.github());
        p.setLinkedin(d.linkedin());
        p.setPhoto(d.photo());
        List<Integer> self = d.self();
        if (self != null) p.setSelfRatings(String.join(",", self.stream().map(i -> String.valueOf(Math.max(0, Math.min(100, i == null ? 0 : i)))).toList()));
        p.setProjectsJson(d.projects() == null ? "[]" : d.projects().toString());
        p.setCertsJson(d.certs() == null ? "[]" : d.certs().toString());
        profiles.save(p);
        return mapper.profile(u, p);
    }
}
