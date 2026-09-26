package com.example.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    LiveData<List<Project>> getAllProjects();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    Project getProjectById(int id);
}
