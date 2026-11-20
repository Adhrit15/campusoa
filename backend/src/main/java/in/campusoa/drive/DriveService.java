package in.campusoa.drive;

import in.campusoa.common.ApiException;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.company.Company;
import in.campusoa.company.CompanyRepository;
import in.campusoa.config.AppProperties;
import in.campusoa.drive.DriveDtos.CalendarEventDto;
import in.campusoa.drive.DriveDtos.CompanyRef;
import in.campusoa.drive.DriveDtos.CriteriaDto;
import in.campusoa.drive.DriveDtos.CriteriaRequest;
import in.campusoa.drive.DriveDtos.DriveDetailDto;
import in.campusoa.drive.DriveDtos.DriveRequest;
import in.campusoa.drive.DriveDtos.DriveStageDto;
import in.campusoa.drive.DriveDtos.DriveSummaryDto;
import in.campusoa.drive.DriveDtos.EligibilityDto;
import in.campusoa.drive.DriveDtos.StagesRequest;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import in.campusoa.user.BranchRepository;
import in.campusoa.user.PrivateProfile;
import in.campusoa.user.PrivateProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DriveService {

    private static final int MAX_CALENDAR_DAYS = 93;

    private final DriveRepository drives;
    private final DriveStageRepository stages;
    private final DriveCriteriaRepository criteria;
    private final DriveQueries queries;
    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final PrivateProfileRepository profiles;
    private final BranchRepository branches;
    private final AppProperties props;

    public DriveService(DriveRepository drives, DriveStageRepository stages, DriveCriteriaRepository criteria,
                        DriveQueries queries, CompanyRepository companies, AppUserRepository users,
                        PrivateProfileRepository profiles, BranchRepository branches, AppProperties props) {
        this.drives = drives;
        this.stages = stages;
        this.criteria = criteria;
        this.queries = queries;
        this.companies = companies;
        this.users = users;
        this.profiles = profiles;
        this.branches = branches;
        this.props = props;
    }

    // ---------- reads ----------

    /**
     * Two-step read: (1) SQL gives one page of drive rows; (2) we batch-load stages and criteria for
     * just those ids with "WHERE drive_id IN (...)". That's 3 queries per page, however many drives:
     * the fix for the "N+1 queries" problem.
     */
    @Transactional(readOnly = true)
    public PageResponse<DriveSummaryDto> list(DriveQueries.Filter filter, PageParams page, Long viewerId) {
        List<DriveQueries.Row> rows = queries.list(filter, page);
        List<Long> ids = rows.stream().map(DriveQueries.Row::id).toList();
        Map<Long, List<DriveStage>> stagesByDrive = stages.findByDriveIdIn(ids).stream()
                .collect(Collectors.groupingBy(DriveStage::getDriveId));
        Map<Long, DriveCriteria> criteriaByDrive = criteriaFor(ids);
        Eligibility.Student student = viewerId == null ? null : studentSnapshot(viewerId);

        List<DriveSummaryDto> content = rows.stream().map(r -> new DriveSummaryDto(
                r.id(), r.title(), r.noticeDate(), r.pdfUrl(), r.source(),
                r.companyId() == null ? null : new CompanyRef(r.companyId(), r.companySlug(), r.companyName()),
                r.companyGuess(), r.roleTitle(), r.ctcText(),
                stagesByDrive.getOrDefault(r.id(), List.of()).stream().map(DriveStage::getStageType).distinct().toList(),
                criteriaByDrive.containsKey(r.id()),
                eligibility(criteriaByDrive.get(r.id()), student))).toList();
        return PageResponse.of(content, page.page(), page.size(), queries.count(filter));
    }

    @Transactional(readOnly = true)
    public DriveDetailDto detail(long id, Long viewerId) {
        Drive d = require(id);
        if (d.isHidden() && d.getMergedIntoId() == null) throw ApiException.notFound("Drive");
        DriveCriteria c = criteria.findById(id).orElse(null);
        Company company = d.getCompanyId() == null ? null : companies.findById(d.getCompanyId()).orElse(null);
        return new DriveDetailDto(d.getId(), d.getTitle(), d.getNoticeDate(), d.getPdfUrl(), d.getSource(),
                company == null ? null : new CompanyRef(company.getId(), company.getSlug(), company.getName()),
                d.getCompanyGuess(), d.getRoleTitle(), d.getCtcText(),
                stages.findByDriveIdOrderByStartsAtAscIdAsc(id).stream().map(DriveStageDto::of).toList(),
                c == null ? null : CriteriaDto.of(c),
                eligibility(c, viewerId == null ? null : studentSnapshot(viewerId)),
                d.getMergedIntoId(), d.isHidden());
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> events(LocalDate from, LocalDate to, Long viewerId) {
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_DAYS) {
            throw ApiException.badRequest("BAD_RANGE", "Calendar range must be 0–" + MAX_CALENDAR_DAYS + " days");
        }
        var rows = queries.events(from, to, props.timezone().getId());
        Map<Long, DriveCriteria> crit = criteriaFor(rows.stream().map(DriveQueries.EventRow::driveId).distinct().toList());
        Eligibility.Student student = viewerId == null ? null : studentSnapshot(viewerId);
        return rows.stream().map(r -> new CalendarEventDto(r.driveId(), r.title(), r.companyName(), r.companySlug(),
                r.kind(), r.date(), r.startsAt(),
                student == null ? null : evaluate(crit.get(r.driveId()), student).status())).toList();
    }

    // ---------- admin writes ----------

    @Transactional
    public DriveDetailDto create(DriveRequest req) {
        Drive d = new Drive(Drive.Source.MANUAL, req.title().trim(), req.noticeDate());
        apply(d, req);
        drives.save(d);
        return detail(d.getId(), null);
    }

    @Transactional
    public DriveDetailDto update(long id, DriveRequest req) {
        Drive d = require(id);
        d.setTitle(req.title().trim());
        d.setNoticeDate(req.noticeDate());
        apply(d, req);
        d.touch();
        return detail(id, null);
    }

    /**
     * Merge {@code sourceId} into {@code targetId}: stages move over, criteria are copied if the
     * target has none, and the source is hidden with a pointer to the target (so old links redirect).
     */
    @Transactional
    public DriveDetailDto merge(long sourceId, long targetId) {
        if (sourceId == targetId) throw ApiException.badRequest("SELF_MERGE", "Can't merge a drive into itself");
        Drive source = require(sourceId);
        Drive target = require(targetId);
        if (target.getMergedIntoId() != null) {
            throw ApiException.badRequest("TARGET_MERGED", "Target drive is itself merged; merge into its target");
        }
        stages.findByDriveId(sourceId).forEach(s -> s.moveTo(targetId));
        criteria.findById(sourceId).ifPresent(sc -> {
            if (!criteria.existsById(targetId)) {
                DriveCriteria tc = new DriveCriteria(targetId);
                tc.update(sc.getMinCgpa(), sc.getMaxBacklogs(), sc.getGradYear(), sc.getEligibleBranches(), sc.getNotes());
                criteria.save(tc);
            }
            criteria.delete(sc);
        });
        if (target.getCompanyId() == null) target.setCompanyId(source.getCompanyId());
        target.touch();
        // The source keeps its pdf_url: it's the scraper's dedupe key, so the circular won't be re-imported.
        source.mergeInto(targetId);
        return detail(targetId, null);
    }

    @Transactional
    public CriteriaDto putCriteria(long driveId, CriteriaRequest req) {
        require(driveId);
        Set<String> wanted = req.branches() == null ? Set.of() : req.branches();
        for (String b : wanted) {
            if (!branches.existsById(b)) throw ApiException.badRequest("UNKNOWN_BRANCH", "Unknown branch: " + b);
        }
        DriveCriteria c = criteria.findById(driveId).orElseGet(() -> new DriveCriteria(driveId));
        c.update(req.minCgpa(), req.maxBacklogs(), req.gradYear(), wanted, req.notes());
        return CriteriaDto.of(criteria.save(c));
    }

    @Transactional
    public void deleteCriteria(long driveId) {
        criteria.findById(driveId).ifPresent(criteria::delete);
    }

    @Transactional
    public List<DriveStageDto> replaceStages(long driveId, StagesRequest req) {
        require(driveId);
        stages.deleteAll(stages.findByDriveId(driveId));
        req.stages().forEach(s -> stages.save(new DriveStage(driveId, s.stageType(), s.startsAt(), s.mode(), s.notes())));
        return stages.findByDriveIdOrderByStartsAtAscIdAsc(driveId).stream().map(DriveStageDto::of).toList();
    }

    // ---------- used by the scraper ----------

    public record ScrapedDrive(String title, LocalDate noticeDate, String pdfUrl, Long companyId,
                               String companyGuess, Set<StageType> stageTypes) {}

    /** Insert if the PDF URL is new; returns true if a row was created. Existing rows are left alone (admins may have edited them). */
    @Transactional
    public boolean upsertScraped(ScrapedDrive s) {
        if (drives.existsByPdfUrl(s.pdfUrl())) return false;
        Drive d = new Drive(Drive.Source.SCRAPED, truncate(s.title(), 300), s.noticeDate());
        d.setPdfUrl(s.pdfUrl());
        d.setCompanyId(s.companyId());
        d.setCompanyGuess(s.companyId() == null ? truncate(s.companyGuess(), 120) : null);
        drives.save(d);
        s.stageTypes().forEach(t -> stages.save(new DriveStage(d.getId(), t, null, null, "Detected from circular title")));
        return true;
    }

    // ---------- helpers ----------

    private Drive require(long id) {
        return drives.findById(id).orElseThrow(() -> ApiException.notFound("Drive"));
    }

    private void apply(Drive d, DriveRequest req) {
        if (req.companyId() != null && !companies.existsById(req.companyId())) {
            throw ApiException.badRequest("UNKNOWN_COMPANY", "Unknown company id " + req.companyId());
        }
        d.setCompanyId(req.companyId());
        if (req.companyId() != null) d.setCompanyGuess(null);
        if (req.pdfUrl() != null) d.setPdfUrl(req.pdfUrl());
        d.setRoleTitle(req.roleTitle());
        d.setCtcText(req.ctcText());
        d.setHidden(req.hidden());
    }

    private Map<Long, DriveCriteria> criteriaFor(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return criteria.findByDriveIdIn(ids).stream().collect(Collectors.toMap(DriveCriteria::getDriveId, Function.identity()));
    }

    Eligibility.Student studentSnapshot(long userId) {
        AppUser u = users.findById(userId).orElse(null);
        if (u == null) return null;
        PrivateProfile p = profiles.findById(userId).orElse(null);
        return new Eligibility.Student(u.getBranchCode(), u.getGradYear(),
                p == null ? null : p.getCgpa(), p == null ? null : p.getActiveBacklogs());
    }

    private static EligibilityDto eligibility(DriveCriteria c, Eligibility.Student student) {
        return student == null ? null : EligibilityDto.of(evaluate(c, student));
    }

    private static Eligibility.Result evaluate(DriveCriteria c, Eligibility.Student student) {
        return Eligibility.evaluate(c == null ? null : c.toRules(), student);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
