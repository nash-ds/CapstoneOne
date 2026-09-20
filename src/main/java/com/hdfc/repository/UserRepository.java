package com.hdfc.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hdfc.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>{

    User findByEmail(String email);
}

