package com.nets150.recommender.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A movie node in the recommendation graph.
 */
public final class Movie implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String title;
    private final Set<String> genres;
    private final Set<String> actors;
    private final String director;

    public Movie(int id, String title, Set<String> genres, Set<String> actors, String director) {
        this.id = id;
        this.title = Objects.requireNonNull(title);
        this.genres = Collections.unmodifiableSet(new LinkedHashSet<>(genres));
        this.actors = Collections.unmodifiableSet(new LinkedHashSet<>(actors));
        this.director = director == null ? "" : director;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getGenres() {
        return genres;
    }

    public Set<String> getActors() {
        return actors;
    }

    public String getDirector() {
        return director;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Movie other)) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return title + " (" + id + ")";
    }
}
