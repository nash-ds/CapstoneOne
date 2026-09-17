package com.hdfc.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder 
@ToString 
public class User {
    private int userId;
    private String email;
    private String password;
    private List<String> roles;

}
