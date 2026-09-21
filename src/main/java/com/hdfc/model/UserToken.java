package com.hdfc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserToken {

    @Id
    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

}
