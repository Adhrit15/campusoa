package in.campusoa.drive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A scheduled step of a drive (PPT on Monday 10:00, OA on Wednesday ...). startsAt may be unknown. */
@Entity
@Table(name = "drive_stage")
public class DriveStage {

    public enum Mode { ONLINE, OFFLINE, HYBRID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drive_id", nullable = false)
    private Long driveId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false)
    private StageType stageType;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    private Mode mode;

    private String notes;

    protected DriveStage() {}

    public DriveStage(Long driveId, StageType stageType, Instant startsAt, Mode mode, String notes) {
        this.driveId = driveId;
        this.stageType = stageType;
        this.startsAt = startsAt;
        this.mode = mode;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getDriveId() { return driveId; }
    public void moveTo(long driveId) { this.driveId = driveId; }
    public StageType getStageType() { return stageType; }
    public Instant getStartsAt() { return startsAt; }
    public Mode getMode() { return mode; }
    public String getNotes() { return notes; }
}
