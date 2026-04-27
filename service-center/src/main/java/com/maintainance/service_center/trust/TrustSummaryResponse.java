package com.maintainance.service_center.trust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrustSummaryResponse {

    private List<TrustBadgeResponse> badges;
}
