package com.example.springrag.infrastructure.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSessionStateRepository extends JpaRepository<SessionStateEntity, String> {
}
