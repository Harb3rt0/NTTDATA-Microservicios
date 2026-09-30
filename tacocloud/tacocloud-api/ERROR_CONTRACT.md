# TC-09 - HTTP error contract

Every controlled API error uses `Content-Type: application/problem+json` and this shape:

```json
{
  "type": "urn:tacocloud:problem:validation",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/orders",
  "code": "VALIDATION_FAILED",
  "violations": [
    { "field": "deliveryName", "message": "Delivery name is required" }
  ]
}
```

The `code` values are public and stable. They are declared in `ApiErrorCodes`.

## Status policy

- `400 Bad Request`: malformed JSON, invalid fields, formats, sizes, path values, or parameters.
- `401 Unauthorized`: authentication is required or failed.
- `403 Forbidden`: an authenticated caller is not allowed to perform the operation.
- `404 Not Found`: the resource identified by the requested path does not exist.
- `409 Conflict`: uniqueness, duplicate resource, optimistic locking, or current-state conflicts.
- `422 Unprocessable Entity`: the input is structurally valid but violates a business rule.
- `500 Internal Server Error`: unexpected failures. The response never exposes stack traces, exception names, database driver messages, or internal implementation details.

## Stable codes

| Code | HTTP status | Meaning |
| --- | ---: | --- |
| `VALIDATION_FAILED` | 400 | One or more validated values are invalid. |
| `INVALID_JSON` | 400 | The JSON body cannot be parsed. |
| `BAD_REQUEST` | 400 | A parameter or path value is invalid. |
| `INGREDIENT_ID_MISMATCH` | 400 | Body and path ingredient identifiers differ. |
| `AUTHENTICATION_REQUIRED` | 401 | Authentication is required. |
| `ACCESS_DENIED` | 403 | The caller lacks permission. |
| `RESOURCE_NOT_FOUND` | 404 | A route or generic resource does not exist. |
| `INGREDIENT_NOT_FOUND` | 404 | The requested ingredient does not exist. |
| `TACO_NOT_FOUND` | 404 | The requested taco does not exist. |
| `ORDER_NOT_FOUND` | 404 | The requested order does not exist. |
| `DUPLICATE_RESOURCE` | 409 | A unique value already exists. |
| `USERNAME_ALREADY_EXISTS` | 409 | The requested username is already registered. |
| `EMAIL_ALREADY_EXISTS` | 409 | The requested email is already registered. |
| `OPTIMISTIC_LOCK_CONFLICT` | 409 | The resource was concurrently modified. |
| `ORDER_USER_NOT_FOUND` | 422 | An email order has no matching customer. |
| `ORDER_PAYMENT_METHOD_NOT_FOUND` | 422 | The customer has no usable payment method. |
| `ORDER_INGREDIENT_NOT_FOUND` | 422 | An order references an unavailable ingredient. |
| `EMAIL_ORDER_REQUIRED` | 400 | The email order body is missing. |
| `EMAIL_TACOS_REQUIRED` | 422 | An email order contains no tacos. |
| `EMAIL_TACO_INGREDIENTS_REQUIRED` | 422 | An email taco contains no ingredients. |
| `METHOD_NOT_ALLOWED` | 405 | The HTTP method is unsupported for the route. |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | The request content type is unsupported. |
| `INTERNAL_ERROR` | 500 | An unexpected, sanitized server failure occurred. |

Correlation identifiers are intentionally deferred until TC-31, as required by the challenge dependency.
