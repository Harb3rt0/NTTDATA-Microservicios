package tacos.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.stereotype.Service;

import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;

//TC-15 - Aplica cupones sobre importes calculados por el servidor
@Service
public class CouponService {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CouponProperties properties;
    private final Clock clock;

    public CouponService(CouponProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public CouponApplication apply(String rawCode, BigDecimal rawSubtotal) {
        BigDecimal subtotal = money(rawSubtotal);
        String code = normalize(rawCode);
        if (code == null) {
            return new CouponApplication(null, subtotal, money(BigDecimal.ZERO), subtotal);
        }

        CouponProperties.Definition coupon = properties.getDefinitions().stream()
            .filter(candidate -> code.equals(normalize(candidate.getCode())))
            .findFirst()
            .orElseThrow(this::notApplicable);
        LocalDate today = LocalDate.now(clock);
        if ((coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom()))
                || (coupon.getValidUntil() != null && today.isAfter(coupon.getValidUntil()))
                || (coupon.getMinimumPurchase() != null
                    && subtotal.compareTo(coupon.getMinimumPurchase()) < 0)) {
            throw notApplicable();
        }

        BigDecimal calculated = calculate(coupon, subtotal);
        if (coupon.getMaximumDiscount() != null) {
            calculated = calculated.min(coupon.getMaximumDiscount());
        }
        BigDecimal discount = money(calculated.max(BigDecimal.ZERO).min(subtotal));
        BigDecimal total = money(subtotal.subtract(discount).max(BigDecimal.ZERO));
        return new CouponApplication(code, subtotal, discount, total);
    }

    private BigDecimal calculate(CouponProperties.Definition coupon, BigDecimal subtotal) {
        if (coupon.getType() == CouponType.PERCENTAGE) {
            return subtotal.multiply(coupon.getValue())
                .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
        }
        if (coupon.getType() == CouponType.FIXED) {
            return coupon.getValue();
        }
        throw notApplicable();
    }

    private String normalize(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BusinessRuleException notApplicable() {
        return new BusinessRuleException(ApiErrorCodes.COUPON_NOT_APPLICABLE,
            "The coupon cannot be applied to this order.");
    }
}
//Fin TC-15
