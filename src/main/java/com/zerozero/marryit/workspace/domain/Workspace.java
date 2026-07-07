package com.zerozero.marryit.workspace.domain;

import com.zerozero.marryit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Workspace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    protected Workspace() {
    }

    private Workspace(String name) {
        this.name = name;
    }

    public static Workspace createPersonal(String ownerName) {
        return new Workspace(ownerName + "의 Workspace");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
