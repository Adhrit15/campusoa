package in.campusoa.drive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public final class DriveDtos {

    private DriveDtos() {}

    public record CompanyRef(long id, String slug, String name) {}

    public record EligibilityDto(Eligibility.Status status, List<String> reasons) {
        static EligibilityDto of(Eligibility.Result r) {
            return new EligibilityDto(r.status(), r.reasons());
        }
    }

    public record DriveSummaryDto(long id, String title, LocalDate noticeDate, String pdfUrl, Drive.Source source,
                                  CompanyRef company, String companyGuess, String roleTitle, String ctcText,
                                  List<StageType> stageTypes, boolean hasCriteria,
                                  EligibilityDto eligibility) {}     // null when signed out

    public record DriveStageDto(long id, StageType stageType, Instant startsAt, DriveStage.Mode mode, String notes) {
        static DriveStageDto of(DriveStage s) {
            return new DriveStageDto(s.getId(), s.getStageType(), s.getStartsAt(), s.getMode(), s.getNotes());
        }
    }

    public record CriteriaDto(BigDecimal minCgpa, Short maxBacklogs, Short gradYear, Set<String> branches,
                              String notes) {
        static CriteriaDto of(DriveCriteria c) {
            return new CriteriaDto(c.getMinCgpa(), c.getMaxBacklogs(), c.getGradYear(),
                    new java.util.TreeSet<>(c.getEligibleBranches()), c.getNotes());
        }
    }

    public record DriveDetailDto(long id, String title, LocalDate noticeDate, String pdfUrl, Drive.Source source,
                                 CompanyRef company, String companyGuess, String roleTitle, String ctcText,
                                 List<DriveStageDto> stages, CriteriaDto criteria, EligibilityDto eligibility,
                                 Long mergedIntoId, boolean hidden) {}

    /** One dot on the calendar: either the circular's date or a scheduled stage. */
    public record CalendarEventDto(long driveId, String title, String companyName, String companySlug,
                                   String kind, LocalDate date, Instant startsAt,
                                   Eligibility.Status eligibility) {}

    // ----- admin requests -----

    public record DriveRequest(
            Long companyId,
            @NotBlank @Size(max = 300) String title,
            @NotNull LocalDate noticeDate,
            @Size(max = 500) @Pattern(regexp = "^https?://.+") String pdfUrl,
            @Size(max = 200) String roleTitle,
            @Size(max = 100) String ctcText,
            boolean hidden) {}

    public record MergeRequest(@NotNull @Positive Long targetDriveId) {}

    public record CriteriaRequest(
            @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal minCgpa,
            @Min(0) @Max(60) Short maxBacklogs,
            @Min(2020) @Max(2040) Short gradYear,
            @Size(max = 12) Set<@NotBlank String> branches,
            @Size(max = 500) String notes) {}

    public record StageRequest(@NotNull StageType stageType, Instant startsAt, DriveStage.Mode mode,
                               @Size(max = 500) String notes) {}

    public record StagesRequest(@NotNull @Size(max = 10) List<@Valid StageRequest> stages) {}
}
