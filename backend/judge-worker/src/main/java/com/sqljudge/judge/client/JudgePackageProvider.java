package com.sqljudge.judge.client;

import com.sqljudge.judge.client.JudgePackageResponse.PackageTestcase;
import com.sqljudge.judge.compare.CompareOptions;
import com.sqljudge.judge.job.JudgePackage;
import com.sqljudge.judge.job.JudgePackage.JudgeCase;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JudgePackageProvider {

    private static final int CACHED_PACKAGES = 64;

    private final ProblemServiceClient problemServiceClient;
    private final Map<UUID, JudgePackage> cache = Collections.synchronizedMap(new BoundedCache());

    public JudgePackageProvider(ProblemServiceClient problemServiceClient) {
        this.problemServiceClient = problemServiceClient;
    }

    public JudgePackage packageOf(UUID revisionId) {
        JudgePackage cached = cache.get(revisionId);
        if (cached != null) {
            return cached;
        }
        JudgePackage fetched = toPackage(problemServiceClient.judgePackage(revisionId));
        cache.put(revisionId, fetched);
        return fetched;
    }

    private static JudgePackage toPackage(JudgePackageResponse response) {
        return new JudgePackage(
                response.datasetVersionId(),
                response.datasetChecksum(),
                new CompareOptions(
                        response.requiresOrder(),
                        response.compareColumnNames(),
                        response.floatTolerance() == null
                                ? CompareOptions.DEFAULT_TOLERANCE
                                : response.floatTolerance()),
                response.timeLimitMs(),
                response.allowedStatements(),
                response.testcases().stream().map(JudgePackageProvider::toCase).toList());
    }

    private static JudgeCase toCase(PackageTestcase testcase) {
        return new JudgeCase(
                testcase.index(),
                testcase.variantId(),
                testcase.isPublic(),
                ExpectedTables.decode(testcase.expected().columns(), testcase.expected().rows()));
    }

    private static final class BoundedCache extends LinkedHashMap<UUID, JudgePackage> {

        private BoundedCache() {
            super(CACHED_PACKAGES, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, JudgePackage> eldest) {
            return size() > CACHED_PACKAGES;
        }
    }
}
