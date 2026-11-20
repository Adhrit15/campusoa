package in.campusoa.company;

import in.campusoa.common.ApiException;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.common.Slugs;
import in.campusoa.company.CompanyDtos.CompanyDetailDto;
import in.campusoa.company.CompanyDtos.CompanyRequest;
import in.campusoa.company.CompanyDtos.CompanySummaryDto;
import in.campusoa.company.CompanyDtos.InternshipStatDto;
import in.campusoa.company.CompanyDtos.InternshipStatRequest;
import in.campusoa.company.CompanyDtos.PlacementStatDto;
import in.campusoa.company.CompanyDtos.PlacementStatRequest;
import in.campusoa.company.CompanyDtos.SelectionStageDto;
import in.campusoa.company.CompanyDtos.StagesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyService {

    private final CompanyRepository companies;
    private final CompanyAliasRepository aliases;
    private final PlacementStatRepository placements;
    private final InternshipStatRepository internships;
    private final SelectionStageRepository stages;
    private final CompanyQueries queries;

    public CompanyService(CompanyRepository companies, CompanyAliasRepository aliases,
                          PlacementStatRepository placements, InternshipStatRepository internships,
                          SelectionStageRepository stages, CompanyQueries queries) {
        this.companies = companies;
        this.aliases = aliases;
        this.placements = placements;
        this.internships = internships;
        this.stages = stages;
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanySummaryDto> list(String q, String sort, PageParams page) {
        return PageResponse.of(queries.list(q, sort, page), page.page(), page.size(), queries.count(q));
    }

    @Transactional(readOnly = true)
    public CompanyDetailDto detail(String slug) {
        Company c = companies.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Company"));
        return toDetail(c);
    }

    public Company require(long id) {
        return companies.findById(id).orElseThrow(() -> ApiException.notFound("Company"));
    }

    /**
     * Every company name and alias, longest first: used by the scraper to recognise companies in
     * circular titles ("Electronic Arts" must win over "EA").
     */
    @Transactional(readOnly = true)
    public Map<String, Long> namesAndAliasesLongestFirst() {
        Map<String, Long> all = new java.util.HashMap<>();
        companies.findAll().forEach(c -> all.put(c.getName(), c.getId()));
        aliases.findAll().forEach(a -> all.put(a.getAlias(), a.getCompanyId()));
        Map<String, Long> sorted = new LinkedHashMap<>();
        all.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // ----- admin -----

    @Transactional
    public CompanyDetailDto create(CompanyRequest req) {
        String slug = req.slug() != null && !req.slug().isBlank() ? req.slug() : Slugs.of(req.name());
        if (companies.existsBySlug(slug) || companies.existsByNameIgnoreCase(req.name().trim())) {
            throw ApiException.conflict("COMPANY_EXISTS", "A company with that name or slug already exists");
        }
        Company c = new Company(slug, req.name().trim());
        apply(c, req);
        companies.save(c);
        replaceAliases(c.getId(), req.aliases());
        return toDetail(c);
    }

    @Transactional
    public CompanyDetailDto update(long id, CompanyRequest req) {
        Company c = require(id);
        c.setName(req.name().trim());
        if (req.slug() != null && !req.slug().isBlank()) c.setSlug(req.slug());
        apply(c, req);
        if (req.aliases() != null) replaceAliases(id, req.aliases());
        return toDetail(c);
    }

    @Transactional
    public PlacementStatDto addPlacement(long companyId, PlacementStatRequest r) {
        require(companyId);
        validateRange(r);
        PlacementStat s = new PlacementStat(companyId);
        s.update(r.academicYear(), r.roleTitle(), r.ctcMinLpa(), r.ctcMaxLpa(), r.offers(), r.sourceUrl(), r.sourceNote());
        return PlacementStatDto.of(placements.save(s));
    }

    @Transactional
    public PlacementStatDto updatePlacement(long companyId, long statId, PlacementStatRequest r) {
        PlacementStat s = placements.findById(statId)
                .filter(p -> p.getCompanyId() == companyId)
                .orElseThrow(() -> ApiException.notFound("Placement stat"));
        validateRange(r);
        s.update(r.academicYear(), r.roleTitle(), r.ctcMinLpa(), r.ctcMaxLpa(), r.offers(), r.sourceUrl(), r.sourceNote());
        return PlacementStatDto.of(s);
    }

    @Transactional
    public void deletePlacement(long companyId, long statId) {
        PlacementStat s = placements.findById(statId)
                .filter(p -> p.getCompanyId() == companyId)
                .orElseThrow(() -> ApiException.notFound("Placement stat"));
        placements.delete(s);
    }

    /** Upsert: one stipend row per company per year. */
    @Transactional
    public InternshipStatDto putInternship(long companyId, InternshipStatRequest r) {
        require(companyId);
        InternshipStat s = internships.findByCompanyIdAndAcademicYear(companyId, r.academicYear())
                .orElseGet(() -> new InternshipStat(companyId, r.academicYear()));
        s.update(r.stipendInrMonth(), r.sourceUrl(), r.sourceNote());
        return InternshipStatDto.of(internships.save(s));
    }

    @Transactional
    public List<SelectionStageDto> replaceStages(long companyId, StagesRequest req) {
        require(companyId);
        stages.deleteByCompanyId(companyId);
        stages.flush();   // make sure the DELETE runs before the INSERTs (unique position constraint)
        short pos = 1;
        for (var s : req.stages()) {
            stages.save(new SelectionStage(companyId, pos++, s.stageType(), s.description()));
        }
        return stages.findByCompanyIdOrderByPosition(companyId).stream().map(SelectionStageDto::of).toList();
    }

    // ----- helpers -----

    private void apply(Company c, CompanyRequest req) {
        c.setWebsite(req.website());
        if (req.prepMarkdown() != null) c.setPrepMarkdown(req.prepMarkdown());
        c.touch();
    }

    private void replaceAliases(long companyId, List<String> names) {
        aliases.deleteByCompanyId(companyId);
        aliases.flush();
        if (names == null) return;
        names.stream().map(String::trim).distinct().forEach(a -> aliases.save(new CompanyAlias(companyId, a)));
    }

    private static void validateRange(PlacementStatRequest r) {
        if (r.ctcMinLpa() != null && r.ctcMinLpa().compareTo(r.ctcMaxLpa()) > 0) {
            throw ApiException.badRequest("BAD_CTC_RANGE", "ctcMinLpa must be ≤ ctcMaxLpa");
        }
    }

    private CompanyDetailDto toDetail(Company c) {
        return new CompanyDetailDto(c.getId(), c.getSlug(), c.getName(), c.getWebsite(), c.getPrepMarkdown(),
                aliases.findByCompanyId(c.getId()).stream().map(CompanyAlias::getAlias).sorted().toList(),
                placements.findByCompanyIdOrderByAcademicYearDescCtcMaxLpaDesc(c.getId()).stream()
                        .map(PlacementStatDto::of).toList(),
                internships.findByCompanyIdOrderByAcademicYearDesc(c.getId()).stream()
                        .map(InternshipStatDto::of).toList(),
                stages.findByCompanyIdOrderByPosition(c.getId()).stream().map(SelectionStageDto::of).toList(),
                c.getUpdatedAt());
    }
}
