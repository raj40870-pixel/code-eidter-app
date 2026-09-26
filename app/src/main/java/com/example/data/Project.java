package com.example.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String language; // C++, Python, Java, etc.
    public String path;     // Local path to project folder
    public long createdAt;

    public Project(String name, String language, String path) {
        this.name = name;
        this.language = language;
        this.path = path;
        this.createdAt = System.currentTimeMillis();
    }
}
