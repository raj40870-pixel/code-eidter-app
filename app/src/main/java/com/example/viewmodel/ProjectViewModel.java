package com.example.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.data.Project;
import com.example.data.ProjectRepository;

import java.util.List;

public class ProjectViewModel extends AndroidViewModel {
    private ProjectRepository repository;
    private LiveData<List<Project>> allProjects;

    public ProjectViewModel(@NonNull Application application) {
        super(application);
        repository = new ProjectRepository(application);
        allProjects = repository.getAllProjects();
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }

    public void insert(Project project) {
        repository.insert(project);
    }

    public void delete(Project project) {
        repository.delete(project);
    }
}
