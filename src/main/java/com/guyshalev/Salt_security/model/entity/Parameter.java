package com.guyshalev.Salt_security.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "parameters")
@Getter
@Setter
public class Parameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "parameter_types",
            joinColumns = @JoinColumn(name = "parameter_id")
    )
    @Column(name = "type")
    private List<String> types;

    private boolean required;
}
