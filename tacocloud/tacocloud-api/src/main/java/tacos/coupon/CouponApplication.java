package tacos.coupon;

import java.math.BigDecimal;

import lombok.Value;

//TC-15 - Resultado economico de aplicar un cupon
@Value
public class CouponApplication {
    String couponCode;
    BigDecimal subtotalBeforeDiscount;
    BigDecimal discountAmount;
    BigDecimal total;
}
//Fin TC-15
