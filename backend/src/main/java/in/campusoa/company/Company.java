package in.campusoa.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A recruiter. Child rows (stats, stipends, stages, aliases) reference it by id and are loaded
 * with their own repositories instead of JPA collections: that keeps loading explicit and avoids
 * the classic "N+1 lazy-loading" trap. Compare with InterviewExperience -> rounds, where the
 * children really are part of the parent and are mapped as a @OneToMany.
 */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, unique = true)
    private String name;

    private String website;

    @Column(name = "prep_markdown", nullable = false)
    private String prepMarkdown = "";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Company() {}

    public Company(String slug, String name) {
        this.slug = slug;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getPrepMarkdown() { return prepMarkdown; }
    public void setPrepMarkdown(String md) { this.prepMarkdown = md == null ? "" : md; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = Instant.now(); }
}
