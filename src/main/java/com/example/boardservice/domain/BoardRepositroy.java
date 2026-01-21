package com.example.boardservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepositroy extends JpaRepository<Board, Long> {
}
