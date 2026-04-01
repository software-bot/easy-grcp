package com.local.tools.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MongoConfig {
    @Bean
    public GridFSBucket gridFSBucket(MongoClient mongoClient) {
        var db = mongoClient.getDatabase("ft_db");
        return GridFSBuckets.create(db, "ft_b");
    }
}
