package com.work.matmode.processor.repository;


import com.work.matmode.processor.model.Result;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResultRepository extends MongoRepository<Result, String > {
}
