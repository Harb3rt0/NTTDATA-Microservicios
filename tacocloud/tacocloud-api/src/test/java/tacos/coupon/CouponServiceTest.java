package tacos.coupon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import tacos.api.error.ApiErrorCodes;
import tacos.api.error.BusinessRuleException;

//TC-15 - Pruebas focalizadas del motor de cupones
public class CouponServiceTest {
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    public void shouldApplyPercentageCoupon() {
        CouponService service = service(coupon("SAVE10", CouponType.PERCENTAGE, "10",
            "2026-01-01", "2026-01-31", "5.00", null));

        CouponApplication result = service.apply("SAVE10", new BigDecimal("25.00"));

        assertEquals(new BigDecimal("2.50"), result.getDiscountAmount());
        assertEquals(new BigDecimal("22.50"), result.getTotal());
    }

    @Test
    public void shouldApplyFixedCouponAndNeverProduceNegativeTotal() {
        CouponService service = service(coupon("LESS30", CouponType.FIXED, "30",
            "2026-01-01", "2026-01-31", null, null));

        CouponApplication result = service.apply("LESS30", new BigDecimal("12.00"));

        assertEquals(new BigDecimal("12.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("0.00"), result.getTotal());
    }

    @Test
    public void shouldRespectMinimumPurchase() {
        CouponService service = service(coupon("SAVE10", CouponType.PERCENTAGE, "10",
            "2026-01-01", "2026-01-31", "20.00", null));

        expectNotApplicable(service, "SAVE10", "19.99");
    }

    @Test
    public void shouldRejectCouponOutsideValidityWithoutEnumeration() {
        CouponService service = service(
            coupon("FUTURE", CouponType.FIXED, "1", "2026-01-16", "2026-01-31", null, null),
            coupon("EXPIRED", CouponType.FIXED, "1", "2026-01-01", "2026-01-14", null, null));

        expectNotApplicable(service, "FUTURE", "10.00");
        expectNotApplicable(service, "EXPIRED", "10.00");
        expectNotApplicable(service, "UNKNOWN", "10.00");
    }

    @Test
    public void shouldCapPercentageDiscountAtMaximumDiscount() {
        CouponService service = service(coupon("HALF", CouponType.PERCENTAGE, "50",
            "2026-01-15", "2026-01-15", null, "5.00"));

        CouponApplication result = service.apply("HALF", new BigDecimal("40.00"));

        assertEquals(new BigDecimal("5.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("35.00"), result.getTotal());
    }

    @Test
    public void shouldTreatCouponCodeCaseInsensitiveAtDateBoundary() {
        CouponService service = service(coupon("SAVE10", CouponType.PERCENTAGE, "10",
            "2026-01-15", "2026-01-15", null, null));

        CouponApplication result = service.apply("  save10 ", new BigDecimal("10.00"));

        assertEquals("SAVE10", result.getCouponCode());
        assertEquals(new BigDecimal("9.00"), result.getTotal());
    }

    private CouponService service(CouponProperties.Definition... definitions) {
        CouponProperties properties = new CouponProperties();
        properties.setDefinitions(Arrays.asList(definitions));
        return new CouponService(properties, CLOCK);
    }

    private CouponProperties.Definition coupon(String code, CouponType type, String value,
            String validFrom, String validUntil, String minimum, String maximum) {
        CouponProperties.Definition coupon = new CouponProperties.Definition();
        coupon.setCode(code);
        coupon.setType(type);
        coupon.setValue(new BigDecimal(value));
        coupon.setValidFrom(LocalDate.parse(validFrom));
        coupon.setValidUntil(LocalDate.parse(validUntil));
        coupon.setMinimumPurchase(minimum == null ? null : new BigDecimal(minimum));
        coupon.setMaximumDiscount(maximum == null ? null : new BigDecimal(maximum));
        return coupon;
    }

    private void expectNotApplicable(CouponService service, String code, String subtotal) {
        StepVerifier.create(reactor.core.publisher.Mono.fromCallable(
                () -> service.apply(code, new BigDecimal(subtotal))))
            .expectErrorMatches(error -> error instanceof BusinessRuleException
                && ApiErrorCodes.COUPON_NOT_APPLICABLE.equals(
                    ((BusinessRuleException) error).getCode())
                && "The coupon cannot be applied to this order.".equals(error.getMessage()))
            .verify();
    }
}
//Fin TC-15
