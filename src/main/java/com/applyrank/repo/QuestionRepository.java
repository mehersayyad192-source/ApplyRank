package com.applyrank.repo;

import com.applyrank.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    /** Only ids are loaded; the random pick happens in Java, so it works on H2 and MySQL alike (no RAND()/RANDOM() dialect issue). */
    @Query("select q.id from Question q where q.domain = :domain")
    List<Long> idsByDomain(@Param("domain") String domain);
}
