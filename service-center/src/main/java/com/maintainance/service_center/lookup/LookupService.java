package com.maintainance.service_center.lookup;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LookupService {

    private final LookupRepository lookupRepository;
    private final LookupDetailRepository detailRepository;

    // ── Public reads ────────────────────────────────────────────────────────────

    @Cacheable(value = "lookups", key = "#code")
    @Transactional(readOnly = true)
    public LookupResponse getByCode(String code) {
        Lookup lookup = requireActiveByCode(code);
        return toResponse(lookup, true);
    }

    @Cacheable(value = "lookupDetails", key = "#lookupCode")
    @Transactional(readOnly = true)
    public List<LookupDetailResponse> getActiveDetails(String lookupCode) {
        requireActiveByCode(lookupCode);
        return detailRepository
                .findByLookupCodeAndIsActiveTrueOrderBySortOrderAscIdAsc(lookupCode)
                .stream().map(this::toDetailResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LookupDetailResponse getDetail(String lookupCode, String detailCode) {
        return detailRepository.findByLookupCodeAndCode(lookupCode, detailCode)
                .filter(d -> Boolean.TRUE.equals(d.getIsActive()))
                .map(this::toDetailResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lookup detail not found: " + lookupCode + "/" + detailCode));
    }

    @Transactional(readOnly = true)
    public List<LookupResponse> getAllActive() {
        return lookupRepository.findByIsActiveTrueOrderByCode().stream()
                .map(l -> toResponse(l, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, List<LookupDetailResponse>> getBulk(List<String> codes) {
        return lookupRepository.findByCodes(codes).stream()
                .collect(Collectors.toMap(
                        Lookup::getCode,
                        l -> detailRepository
                                .findByLookupCodeAndIsActiveTrueOrderBySortOrderAscIdAsc(l.getCode())
                                .stream().map(this::toDetailResponse).collect(Collectors.toList())
                ));
    }

    // ── Admin reads (includes inactive) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LookupResponse> getAll() {
        return lookupRepository.findAll().stream()
                .map(l -> toResponse(l, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LookupDetailResponse> getAllDetails(String lookupCode) {
        requireByCode(lookupCode);
        return detailRepository.findByLookupCodeOrderBySortOrderAscIdAsc(lookupCode)
                .stream().map(this::toDetailResponse).collect(Collectors.toList());
    }

    // ── Admin writes ─────────────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lookups",       allEntries = true),
            @CacheEvict(value = "lookupDetails", allEntries = true)
    })
    public LookupResponse createLookup(LookupRequest req) {
        if (lookupRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Lookup code already exists: " + req.getCode());
        }
        Lookup lookup = Lookup.builder()
                .code(req.getCode())
                .nameEn(req.getNameEn())
                .nameAr(req.getNameAr())
                .description(req.getDescription())
                .isActive(Boolean.TRUE.equals(req.getIsActive()))
                .isSystem(Boolean.TRUE.equals(req.getIsSystem()))
                .build();
        lookup = lookupRepository.save(lookup);
        log.info("Created lookup: {}", lookup.getCode());
        return toResponse(lookup, true);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lookups",       allEntries = true),
            @CacheEvict(value = "lookupDetails", allEntries = true)
    })
    public LookupResponse updateLookup(Long id, LookupRequest req) {
        Lookup lookup = lookupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lookup not found: " + id));

        if (!lookup.getCode().equals(req.getCode()) && lookupRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Lookup code already exists: " + req.getCode());
        }
        if (Boolean.TRUE.equals(lookup.getIsSystem()) && !lookup.getCode().equals(req.getCode())) {
            throw new IllegalStateException("Cannot rename a system lookup: " + lookup.getCode());
        }

        lookup.setCode(req.getCode());
        lookup.setNameEn(req.getNameEn());
        lookup.setNameAr(req.getNameAr());
        lookup.setDescription(req.getDescription());
        if (req.getIsActive() != null) lookup.setIsActive(req.getIsActive());

        lookup = lookupRepository.save(lookup);
        log.info("Updated lookup: {}", lookup.getCode());
        return toResponse(lookup, true);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lookups",       allEntries = true),
            @CacheEvict(value = "lookupDetails", allEntries = true)
    })
    public void deleteLookup(Long id) {
        Lookup lookup = lookupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lookup not found: " + id));
        if (Boolean.TRUE.equals(lookup.getIsSystem())) {
            throw new IllegalStateException("System lookup cannot be deleted: " + lookup.getCode());
        }
        lookup.setIsActive(false);
        lookupRepository.save(lookup);
        log.info("Soft-deleted lookup: {}", lookup.getCode());
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lookups",       allEntries = true),
            @CacheEvict(value = "lookupDetails", allEntries = true)
    })
    public LookupDetailResponse addDetail(String lookupCode, LookupDetailRequest req) {
        Lookup lookup = requireByCode(lookupCode);

        if (detailRepository.existsByLookupCodeAndCode(lookupCode, req.getCode())) {
            throw new IllegalArgumentException(
                    "Detail code already exists in " + lookupCode + ": " + req.getCode());
        }

        LookupDetail parent = resolveParent(req.getParentId());

        LookupDetail detail = LookupDetail.builder()
                .lookup(lookup)
                .code(req.getCode())
                .nameEn(req.getNameEn())
                .nameAr(req.getNameAr())
                .shortName(req.getShortName())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .isActive(Boolean.TRUE.equals(req.getIsActive()))
                .isSystem(Boolean.TRUE.equals(req.getIsSystem()))
                .parent(parent)
                .extraData(req.getExtraData())
                .build();

        detail = detailRepository.save(detail);
        log.info("Added detail {}/{}", lookupCode, detail.getCode());
        return toDetailResponse(detail);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lookups",       allEntries = true),
            @CacheEvict(value = "lookupDetails", allEntries = true)
    })
    public LookupDetailResponse updateDetail(Long id, LookupDetailRequest req) {
        LookupDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lookup detail not found: " + id));

        String lookupCode = detail.getLookup().getCode();

        if (Boolean.TRUE.equals(detail.getIsSystem()) && !detail.getCode().equals(req.getCode())) {
            throw new IllegalStateException("Cannot rename a system lookup detail: " + detail.getCode());
        }
        if (!detail.getCode().equals(req.getCode()) &&
                detailRepository.existsByLookupCodeAndCode(lookupCode, req.getCode())) {
            throw new IllegalArgumentException(
                    "Detail code already exists in " + lookupCode + ": " + req.getCode());
        }

        detail.setCode(req.getCode());
        detail.setNameEn(req.getNameEn());
        detail.setNameAr(req.getNameAr());
        detail.setShortName(req.getShortName());
        if (req.getSortOrder() != null) detail.setSortOrder(req.getSortOrder());
        if (req.getIsActive() != null) detail.setIsActive(req.getIsActive());
        detail.setParent(resolveParent(req.getParentId()));
        detail.setExtraData(req.getExtraData());

        detail = detailRepository.save(detail);
        log.info("Updated detail {}/{}", lookupCode, detail.getCode());
        return toDetailResponse(detail);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "lookups",       allEntries = true),
            @CacheEvict(value = "lookupDetails", allEntries = true)
    })
    public void deleteDetail(Long id) {
        LookupDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lookup detail not found: " + id));
        if (Boolean.TRUE.equals(detail.getIsSystem())) {
            throw new IllegalStateException("System lookup detail cannot be deleted: " + detail.getCode());
        }
        detail.setIsActive(false);
        detailRepository.save(detail);
        log.info("Soft-deleted detail {}/{}", detail.getLookup().getCode(), detail.getCode());
    }

    // ── Mapping ──────────────────────────────────────────────────────────────────

    LookupResponse toResponse(Lookup l, boolean includeDetails) {
        return LookupResponse.builder()
                .id(l.getId())
                .code(l.getCode())
                .nameEn(l.getNameEn())
                .nameAr(l.getNameAr())
                .description(l.getDescription())
                .isActive(l.getIsActive())
                .isSystem(l.getIsSystem())
                .version(l.getVersion())
                .details(includeDetails
                        ? l.getDetails().stream()
                                .map(this::toDetailResponse)
                                .collect(Collectors.toList())
                        : null)
                .createdDate(l.getCreatedDate())
                .createdBy(l.getCreatedBy())
                .updatedDate(l.getUpdatedDate())
                .updatedBy(l.getUpdatedBy())
                .build();
    }

    LookupDetailResponse toDetailResponse(LookupDetail d) {
        return LookupDetailResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .nameEn(d.getNameEn())
                .nameAr(d.getNameAr())
                .shortName(d.getShortName())
                .sortOrder(d.getSortOrder())
                .isActive(d.getIsActive())
                .isSystem(d.getIsSystem())
                .parentId(d.getParent() != null ? d.getParent().getId() : null)
                .parentCode(d.getParent() != null ? d.getParent().getCode() : null)
                .extraData(d.getExtraData())
                .createdDate(d.getCreatedDate())
                .createdBy(d.getCreatedBy())
                .updatedDate(d.getUpdatedDate())
                .updatedBy(d.getUpdatedBy())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private Lookup requireActiveByCode(String code) {
        return lookupRepository.findByCode(code)
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                .orElseThrow(() -> new EntityNotFoundException("Lookup not found: " + code));
    }

    private Lookup requireByCode(String code) {
        return lookupRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Lookup not found: " + code));
    }

    private LookupDetail resolveParent(Long parentId) {
        if (parentId == null) return null;
        return detailRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent detail not found: " + parentId));
    }
}
