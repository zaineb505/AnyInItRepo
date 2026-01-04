package com.deploymentanyinit.Oauth2.Service;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProgressMonitor implements ProgressMonitor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingProgressMonitor.class);
    private String currentTask;
    private int totalWork;
    private int completed;
    private boolean showDuration = true; // Nouveau champ

    @Override
    public void start(int totalTasks) {
        logger.info("[Git] Début de l'opération ({} tâches au total)", totalTasks);
    }

    @Override
    public void beginTask(String title, int totalWork) {
        this.currentTask = title;
        this.totalWork = totalWork;
        this.completed = 0;
        logger.info("[Git] Tâche démarrée : {} ({} unités)", title, totalWork);
    }

    @Override
    public void update(int completed) {
        this.completed += completed;
        if (totalWork > 0) {
            int percent = this.completed * 100 / totalWork;
            logger.debug("[Git] {} : {}% ({}/{})",
                    currentTask, percent, this.completed, totalWork);
        }
    }

    @Override
    public void endTask() {
        logger.info("[Git] Tâche terminée : {}", currentTask);
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    // Nouvelle méthode requise
    @Override
    public void showDuration(boolean enable) {
        this.showDuration = enable;
        logger.debug("[Git] Affichage de la durée : {}", enable);
    }
}