package in.campusoa.company;

import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.company.CompanyDtos.CompanyDetailDto;
import in.campusoa.company.CompanyDtos.CompanyRequest;
import in.campusoa.company.CompanyDtos.CompanySummaryDto;
import in.campusoa.company.CompanyDtos.InternshipStatDto;
import in.campusoa.company.CompanyDtos.InternshipStatRequest;
import in.campusoa.company.CompanyDtos.PlacementStatDto;
import in.campusoa.company.CompanyDtos.PlacementStatRequest;
import in.campusoa.company.CompanyDtos.SelectionStageDto;
import in.campusoa.company.CompanyDtos.StagesRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/** Public company pages plus admin editing (admin methods are guarded by @PreAuthorize). */
@RestController
@Tag(name = "Companies")
public class CompanyController {

    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @GetMapping("/api/companies")
    @Operation(summary = "List companies with latest CTC/offers (sort=ctc|name)")
    public PageResponse<CompanySummaryDto> list(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "ctc") String sort,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.list(q, sort, new PageParams(page, size));
    }

    @GetMapping("/api/companies/{slug}")
    @Operation(summary = "Company page: stats by year, stipends, selection process, prep guide")
    public CompanyDetailDto detail(@PathVariable String slug) {
        return service.detail(slug);
    }

    // ---------- admin ----------

    @PostMapping("/api/admin/companies")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a company", tags = "Admin: companies")
    public ResponseEntity<CompanyDetailDto> create(@Valid @RequestBody CompanyRequest body) {
        CompanyDetailDto created = service.create(body);
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/companies/{slug}").buildAndExpand(created.slug()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/api/admin/companies/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit name, website, aliases and the 'how to prepare' markdown", tags = "Admin: companies")
    public CompanyDetailDto update(@PathVariable long id, @Valid @RequestBody CompanyRequest body) {
        return service.update(id, body);
    }

    @PostMapping("/api/admin/companies/{id}/placements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(tags = "Admin: companies")
    public ResponseEntity<PlacementStatDto> addPlacement(@PathVariable long id,
                                                         @Valid @RequestBody PlacementStatRequest body) {
        return ResponseEntity.status(201).body(service.addPlacement(id, body));
    }

    @PutMapping("/api/admin/companies/{id}/placements/{statId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(tags = "Admin: companies")
    public PlacementStatDto updatePlacement(@PathVariable long id, @PathVariable long statId,
                                            @Valid @RequestBody PlacementStatRequest body) {
        return service.updatePlacement(id, statId, body);
    }

    @DeleteMapping("/api/admin/companies/{id}/placements/{statId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(tags = "Admin: companies")
    public ResponseEntity<Void> deletePlacement(@PathVariable long id, @PathVariable long statId) {
        service.deletePlacement(id, statId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/companies/{id}/internships")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create or replace the stipend for an academic year", tags = "Admin: companies")
    public InternshipStatDto putInternship(@PathVariable long id, @Valid @RequestBody InternshipStatRequest body) {
        return service.putInternship(id, body);
    }

    @PutMapping("/api/admin/companies/{id}/stages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replace the selection process (in order)", tags = "Admin: companies")
    public List<SelectionStageDto> replaceStages(@PathVariable long id, @Valid @RequestBody StagesRequest body) {
        return service.replaceStages(id, body);
    }
}
