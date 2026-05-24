package com.seoyeon.creatorsettlement.domain.settlement.dto;

import com.seoyeon.creatorsettlement.domain.settlement.SettlementResult;

import java.util.List;

public record AdminSettlementResponse(
        String from,
        String to,
        List<CreatorSettlementSummary> creators,
        Totals total
) {
    public record CreatorSettlementSummary(
            String creatorId,
            String creatorName,
            long totalSalesAmount,
            long totalRefundAmount,
            long netSalesAmount,
            long commissionAmount,
            long payoutAmount,
            int salesCount,
            int cancelCount
    ) {
        public static CreatorSettlementSummary of(String creatorId, String creatorName, SettlementResult r) {
            return new CreatorSettlementSummary(
                    creatorId, creatorName,
                    r.totalSalesAmount(), r.totalRefundAmount(), r.netSalesAmount(),
                    r.commissionAmount(), r.payoutAmount(), r.salesCount(), r.cancelCount());
        }
    }

    public record Totals(
            int creatorCount,
            long totalSalesAmount,
            long totalRefundAmount,
            long netSalesAmount,
            long commissionAmount,
            long payoutAmount,
            int salesCount,
            int cancelCount
    ) {}

    public static AdminSettlementResponse of(String from, String to, List<CreatorSettlementSummary> creators) {
        Totals total = new Totals(
                creators.size(),
                creators.stream().mapToLong(CreatorSettlementSummary::totalSalesAmount).sum(),
                creators.stream().mapToLong(CreatorSettlementSummary::totalRefundAmount).sum(),
                creators.stream().mapToLong(CreatorSettlementSummary::netSalesAmount).sum(),
                creators.stream().mapToLong(CreatorSettlementSummary::commissionAmount).sum(),
                creators.stream().mapToLong(CreatorSettlementSummary::payoutAmount).sum(),
                creators.stream().mapToInt(CreatorSettlementSummary::salesCount).sum(),
                creators.stream().mapToInt(CreatorSettlementSummary::cancelCount).sum()
        );
        return new AdminSettlementResponse(from, to, creators, total);
    }
}
