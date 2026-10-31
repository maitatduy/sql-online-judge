package com.sqljudge.problem.service;

import com.sqljudge.problem.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailCache {

    @CacheEvict(cacheNames = CacheConfig.PROBLEM_DETAIL, key = "#slug")
    public void evict(String slug) {}
}
