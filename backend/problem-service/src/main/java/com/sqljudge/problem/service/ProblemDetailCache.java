package com.sqljudge.problem.service;

import com.sqljudge.problem.config.CacheConfig;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailCache {

    @Caching(
            evict = {
                @CacheEvict(cacheNames = CacheConfig.PROBLEM_DETAIL, key = "#slug"),
                @CacheEvict(cacheNames = CacheConfig.CONTEST_STATEMENT, key = "#problemId")
            })
    public void evict(String slug, UUID problemId) {}
}
