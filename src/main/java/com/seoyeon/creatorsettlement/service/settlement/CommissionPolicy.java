package com.seoyeon.creatorsettlement.service.settlement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 적용 수수료율을 제공한다.
 * 현재는 설정값 기반 단일 고정율이지만, 향후 기간/크리에이터별 차등이나
 * 수수료율 이력 적용으로 확장할 때 이 지점만 바꾸면 된다.
 */
@Component
public class CommissionPolicy {

    private final BigDecimal rate;

    public CommissionPolicy(@Value("${settlement.commission-rate:0.20}") BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal currentRate() {
        return rate;
    }
}
