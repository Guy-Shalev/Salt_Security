package com.guyshalev.Salt_security.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String method;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String jsonContent;

    public Model(String path, String method, String jsonContent) {
        this.path = path;
        this.method = method;
        this.jsonContent = jsonContent;
    }

}
