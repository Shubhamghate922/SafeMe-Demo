package com.safeme.backend.repository;

import com.safeme.backend.model.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByEmailIgnoreCase(String email);
    Optional<UserDocument> findByMobileNumber(String mobileNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobileNumber(String mobileNumber);
}
