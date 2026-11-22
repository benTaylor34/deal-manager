package com.dealmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String segment;

    protected Client() {
    }

    public Client(String name, String segment) {
        this.name = name;
        this.segment = segment;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSegment() {
        return segment;
    }
}
