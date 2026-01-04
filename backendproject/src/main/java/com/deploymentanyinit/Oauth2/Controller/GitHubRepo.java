package com.deploymentanyinit.Oauth2.Controller;


import com.fasterxml.jackson.annotation.JsonProperty;

public class GitHubRepo {
    private Long id;
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("clone_url")
    private String cloneUrl;

    @JsonProperty("default_branch")
    private String defaultBranch;

    private boolean isPrivate;

    @JsonProperty("owner")
    private Owner owner;

    // Constructeurs
    public GitHubRepo() {}

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public String getCloneUrl() { return cloneUrl; }
    public void setCloneUrl(String cloneUrl) { this.cloneUrl = cloneUrl; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    @JsonProperty("private")
    public boolean isPrivate() { return isPrivate; }

    @JsonProperty("private")
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }

    // Classe interne pour Owner
    public static class Owner {
        private String login;
        private Long id;

        @JsonProperty("avatar_url")
        private String avatarUrl;

        // Getters et Setters
        public String getLogin() { return login; }
        public void setLogin(String login) { this.login = login; }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    }
}