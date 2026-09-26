package com.example.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ProjectRepository {
    private ProjectDao projectDao;
    private LiveData<List<Project>> allProjects;

    public ProjectRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        projectDao = db.projectDao();
        allProjects = projectDao.getAllProjects();
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }

    public void insert(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            projectDao.insert(project);
        });
    }

    public void delete(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            projectDao.delete(project);
        });
    }
}
