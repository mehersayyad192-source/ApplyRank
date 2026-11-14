package com.applyrank.service;

import com.applyrank.model.Job;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the database query for the Job Corner search box and filters.
 *  q          every word must appear somewhere (title, company, skills, description, location, track)  e.g. "java pune"
 *  track      one track key (backend, frontend, ...)
 *  locations  ANY of these cities (multi-select)
 *  remote     only jobs whose location says "remote"
 *  maxMinRank only jobs whose rank bar is at most this number ("jobs I qualify for")
 */
public final class JobSearch {
    private JobSearch() { }

    /** Turns user text into a safe LIKE pattern (so a typed % or _ is treated as a normal character). */
    static String like(String s) {
        return "%" + s.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public static Specification<Job> of(String q, String track, List<String> locations, boolean remote, Integer maxMinRank) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            all.add(cb.isTrue(root.<Boolean>get("active")));

            if (track != null && !track.isBlank())
                all.add(cb.equal(cb.lower(root.<String>get("track")), track.trim().toLowerCase()));

            if (q != null && !q.isBlank()) {
                String[] words = q.trim().split("\\s+");
                for (int i = 0; i < words.length && i < 6; i++) {
                    String p = like(words[i]);
                    all.add(cb.or(
                            cb.like(cb.lower(root.<String>get("title")), p, '\\'),
                            cb.like(cb.lower(root.<String>get("company")), p, '\\'),
                            cb.like(cb.lower(root.<String>get("requirements")), p, '\\'),
                            cb.like(cb.lower(root.<String>get("description")), p, '\\'),
                            cb.like(cb.lower(root.<String>get("location")), p, '\\'),
                            cb.like(cb.lower(root.<String>get("track")), p, '\\')));
                }
            }

            if (locations != null) {
                List<Predicate> anyCity = new ArrayList<>();
                for (String loc : locations) {
                    if (loc != null && !loc.isBlank() && anyCity.size() < 10)
                        anyCity.add(cb.like(cb.lower(root.<String>get("location")), like(loc), '\\'));
                }
                if (!anyCity.isEmpty()) all.add(cb.or(anyCity.toArray(new Predicate[0])));
            }

            if (remote)
                all.add(cb.like(cb.lower(root.<String>get("location")), "%remote%", '\\'));

            if (maxMinRank != null)
                all.add(cb.le(root.<Integer>get("minRank"), maxMinRank));

            return cb.and(all.toArray(new Predicate[0]));
        };
    }
}
