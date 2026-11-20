package in.campusoa.experience;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One round of an interview ("Round 1: Online assessment, 2 DSA questions in 90 min"). */
@Entity
@Table(name = "experience_round")
public class ExperienceRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private short position;

    @Column(nullable = false)
    private String name;

    private String description;

    protected ExperienceRound() {}

    public ExperienceRound(short position, String name, String description) {
        this.position = position;
        this.name = name;
        this.description = description;
    }

    public short getPosition() { return position; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
