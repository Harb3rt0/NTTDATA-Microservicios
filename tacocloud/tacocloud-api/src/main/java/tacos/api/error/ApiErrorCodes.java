package tacos.api.error;

import tacos.security.RegistrationErrorCodes;

public final class ApiErrorCodes {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String INVALID_JSON = "INVALID_JSON";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String INGREDIENT_NOT_FOUND = "INGREDIENT_NOT_FOUND";
    public static final String TACO_NOT_FOUND = "TACO_NOT_FOUND";
    //TC-20 - Sin candidato valido para recomendacion
    public static final String TACO_OF_DAY_NOT_FOUND = "TACO_OF_DAY_NOT_FOUND";
    //Fin TC-20
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String INGREDIENT_ID_MISMATCH = "INGREDIENT_ID_MISMATCH";
    //TC-13 - Codigos estables de catalogo e inventario
    public static final String INVALID_INGREDIENT_CATALOG = "INVALID_INGREDIENT_CATALOG";
    public static final String NEGATIVE_STOCK_NOT_ALLOWED = "NEGATIVE_STOCK_NOT_ALLOWED";
    //Fin TC-13
    public static final String ORDER_USER_NOT_FOUND = "ORDER_USER_NOT_FOUND";
    public static final String ORDER_PAYMENT_METHOD_NOT_FOUND = "ORDER_PAYMENT_METHOD_NOT_FOUND";
    //TC-12 - Codigos estables para tokenizacion y ownership de pago
    public static final String PAYMENT_METHOD_NOT_FOUND = "PAYMENT_METHOD_NOT_FOUND";
    public static final String PAYMENT_TOKENIZATION_FAILED = "PAYMENT_TOKENIZATION_FAILED";
    //Fin TC-12
    public static final String ORDER_INGREDIENT_NOT_FOUND = "ORDER_INGREDIENT_NOT_FOUND";
    //TC-14 - Codigos estables para cantidades de pedido
    public static final String INVALID_ITEM_QUANTITY = "INVALID_ITEM_QUANTITY";
    public static final String ITEM_QUANTITY_LIMIT_EXCEEDED = "ITEM_QUANTITY_LIMIT_EXCEEDED";
    //Fin TC-14
    //TC-15 - Codigo externo comun para cupones no aplicables
    public static final String COUPON_NOT_APPLICABLE = "COUPON_NOT_APPLICABLE";
    //Fin TC-15
    //TC-18 - Codigo estable para disenos invalidos
    public static final String INVALID_TACO_DESIGN = "INVALID_TACO_DESIGN";
    //Fin TC-18
    //TC-16 - Error estable de reserva de inventario
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    //Fin TC-16
    //TC-24 - Conflicto transitorio de idempotencia
    public static final String REORDER_IN_PROGRESS = "REORDER_IN_PROGRESS";
    //Fin TC-24
    public static final String EMAIL_ORDER_REQUIRED = "EMAIL_ORDER_REQUIRED";
    public static final String EMAIL_TACOS_REQUIRED = "EMAIL_TACOS_REQUIRED";
    public static final String EMAIL_TACO_INGREDIENTS_REQUIRED = "EMAIL_TACO_INGREDIENTS_REQUIRED";
    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";
    //TC-10 - Códigos estables para conflictos conocidos de registro
    public static final String USERNAME_ALREADY_EXISTS = RegistrationErrorCodes.USERNAME_ALREADY_EXISTS;
    public static final String EMAIL_ALREADY_EXISTS = RegistrationErrorCodes.EMAIL_ALREADY_EXISTS;
    //Fin TC-10
    public static final String OPTIMISTIC_LOCK_CONFLICT = "OPTIMISTIC_LOCK_CONFLICT";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ApiErrorCodes() {
    }
}
