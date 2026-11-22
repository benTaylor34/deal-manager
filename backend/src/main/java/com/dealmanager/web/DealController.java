package com.dealmanager.web;

import com.dealmanager.domain.DealStatus;
import com.dealmanager.service.DealService;
import com.dealmanager.web.dto.AuditEventView;
import com.dealmanager.web.dto.CreateDealRequest;
import com.dealmanager.web.dto.DealView;
import com.dealmanager.web.dto.DecisionRequest;
import com.dealmanager.web.dto.PricingLineRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Deal API. Mutations take {@code If-Match: "<version>"} (the ETag from GET) so a
 * user acting on a stale screen gets 412 instead of overwriting someone else's change.
 */
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final DealService service;

    public DealController(DealService service) {
        this.service = service;
    }

    @GetMapping
    public List<DealView> list(@RequestParam(required = false) DealStatus status) {
        return service.list(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealView> get(@PathVariable long id) {
        return withETag(ResponseEntity.ok(), service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('RM')")
    public ResponseEntity<DealView> create(@Valid @RequestBody CreateDealRequest request, Authentication auth) {
        DealView created = service.create(request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return withETag(ResponseEntity.created(location), created);
    }

    @PostMapping("/{id}/pricing-lines")
    @PreAuthorize("hasRole('RM')")
    public ResponseEntity<DealView> addPricingLine(@PathVariable long id,
                                                   @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                                   @Valid @RequestBody PricingLineRequest request,
                                                   Authentication auth) {
        DealView deal = service.addPricingLine(id, ETags.parseVersion(ifMatch), request, auth.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/deals/{id}").buildAndExpand(id).toUri();
        return withETag(ResponseEntity.created(location), deal);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('RM')")
    public ResponseEntity<DealView> submit(@PathVariable long id,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                           @Valid @RequestBody(required = false) DecisionRequest request,
                                           Authentication auth) {
        return withETag(ResponseEntity.ok(),
                service.submit(id, ETags.parseVersion(ifMatch), comment(request), auth.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('PRICING_COMMITTEE')")
    public ResponseEntity<DealView> approve(@PathVariable long id,
                                            @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                            @Valid @RequestBody(required = false) DecisionRequest request,
                                            Authentication auth) {
        return withETag(ResponseEntity.ok(),
                service.approve(id, ETags.parseVersion(ifMatch), comment(request), auth.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('PRICING_COMMITTEE')")
    public ResponseEntity<DealView> reject(@PathVariable long id,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                           @Valid @RequestBody(required = false) DecisionRequest request,
                                           Authentication auth) {
        return withETag(ResponseEntity.ok(),
                service.reject(id, ETags.parseVersion(ifMatch), comment(request), auth.getName()));
    }

    @PostMapping("/{id}/revise")
    @PreAuthorize("hasRole('RM')")
    public ResponseEntity<DealView> revise(@PathVariable long id,
                                           @RequestHeader(value = "If-Match", required = false) String ifMatch,
                                           @Valid @RequestBody(required = false) DecisionRequest request,
                                           Authentication auth) {
        return withETag(ResponseEntity.ok(),
                service.revise(id, ETags.parseVersion(ifMatch), comment(request), auth.getName()));
    }

    @GetMapping("/{id}/audit")
    public List<AuditEventView> audit(@PathVariable long id) {
        return service.auditTrail(id);
    }

    private static String comment(DecisionRequest request) {
        return request == null ? null : request.comment();
    }

    private static ResponseEntity<DealView> withETag(ResponseEntity.BodyBuilder builder, DealView deal) {
        return builder.eTag(ETags.of(deal.version())).body(deal);
    }
}
