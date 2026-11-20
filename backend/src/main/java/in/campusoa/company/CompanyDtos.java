package in.campusoa.company;

import in.campusoa.drive.StageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class CompanyDtos {

    private CompanyDtos() {}

    static final String YEAR_REGEX = "^20[0-9]{2}-[0-9]{2}$";
    static final String URL_REGEX = "^https?://.+";

    public record CompanySummaryDto(long id, String slug, String name, String latestYear, BigDecimal latestCtcLpa,
                                    Integer latestOffers, BigDecimal bestCtcLpa, long experienceCount) {}

    public record PlacementStatDto(long id, String academicYear, String roleTitle, BigDecimal ctcMinLpa,
                                   BigDecimal ctcMaxLpa, Integer offers, String ctcLabel, String sourceUrl,
                                   String sourceNote) {
        static PlacementStatDto of(PlacementStat s) {
            return new PlacementStatDto(s.getId(), s.getAcademicYear(), s.getRoleTitle(), s.getCtcMinLpa(),
                    s.getCtcMaxLpa(), s.getOffers(), CompanyDtos.ctcLabel(s.getCtcMinLpa(), s.getCtcMaxLpa()),
                    s.getSourceUrl(), s.getSourceNote());
        }
    }

    public record InternshipStatDto(long id, String academicYear, int stipendInrMonth, String sourceUrl,
                                    String sourceNote) {
        static InternshipStatDto of(InternshipStat s) {
            return new InternshipStatDto(s.getId(), s.getAcademicYear(), s.getStipendInrMonth(), s.getSourceUrl(),
                    s.getSourceNote());
        }
    }

    public record SelectionStageDto(int position, StageType stageType, String description) {
        static SelectionStageDto of(SelectionStage s) {
            return new SelectionStageDto(s.getPosition(), s.getStageType(), s.getDescription());
        }
    }

    public record CompanyDetailDto(long id, String slug, String name, String website, String prepMarkdown,
                                   List<String> aliases, List<PlacementStatDto> placements,
                                   List<InternshipStatDto> internships, List<SelectionStageDto> selectionProcess,
                                   Instant updatedAt) {}

    // ----- Admin requests -----

    public record CompanyRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 80) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "lowercase words joined by '-'")
            String slug,
            @Size(max = 300) @Pattern(regexp = URL_REGEX, message = "must start with http(s)://") String website,
            @Size(max = 20000) String prepMarkdown,
            @Size(max = 20) List<@NotBlank @Size(max = 120) String> aliases) {}

    public record PlacementStatRequest(
            @NotBlank @Pattern(regexp = YEAR_REGEX, message = "format 2024-25") String academicYear,
            @Size(max = 120) String roleTitle,
            @Positive BigDecimal ctcMinLpa,
            @NotNull @Positive BigDecimal ctcMaxLpa,
            @PositiveOrZero Integer offers,
            @Size(max = 300) @Pattern(regexp = URL_REGEX) String sourceUrl,
            @Size(max = 200) String sourceNote) {}

    public record InternshipStatRequest(
            @NotBlank @Pattern(regexp = YEAR_REGEX, message = "format 2024-25") String academicYear,
            @NotNull @Positive Integer stipendInrMonth,
            @Size(max = 300) @Pattern(regexp = URL_REGEX) String sourceUrl,
            @Size(max = 200) String sourceNote) {}

    public record StageRequest(@NotNull StageType stageType, @Size(max = 300) String description) {}

    public record StagesRequest(@NotNull @Size(max = 12) List<@Valid StageRequest> stages) {}

    /** "51 LPA", "4.5–6.5 LPA", "up to 9.5 LPA". */
    static String ctcLabel(BigDecimal min, BigDecimal max) {
        String hi = max.stripTrailingZeros().toPlainString();
        if (min == null) return "up to " + hi + " LPA";
        if (min.compareTo(max) == 0) return hi + " LPA";
        return min.stripTrailingZeros().toPlainString() + "–" + hi + " LPA";
    }
}
