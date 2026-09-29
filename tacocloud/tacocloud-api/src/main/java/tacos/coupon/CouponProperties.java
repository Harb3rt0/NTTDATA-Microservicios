package tacos.coupon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

//TC-15 - Configuracion externa de cupones
@Data
@Component
@ConfigurationProperties(prefix = "tacocloud.coupons")
public class CouponProperties {
    private List<Definition> definitions = new ArrayList<>();

    @Data
    public static class Definition {
        private String code;
        private CouponType type;
        private BigDecimal value;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate validFrom;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate validUntil;
        private BigDecimal minimumPurchase;
        private BigDecimal maximumDiscount;
    }
}
//Fin TC-15
