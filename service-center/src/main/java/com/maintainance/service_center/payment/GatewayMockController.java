package com.maintainance.service_center.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * Spec 007 — the (mock) MyFatoorah hosted-checkout page + its callback. Stands in for the external
 * provider page the customer's WebView opens: it shows the amount and offers "pay" / "cancel", then
 * confirms capture through {@link PaymentService#confirmGatewayCallback} (the webhook equivalent) and
 * redirects back to the app's return URL. Only loaded when {@code payment.gateway.mock-checkout=true};
 * the routes are permitAll (the WebView carries no app JWT, like a real gateway page).
 */
@RestController
@RequestMapping("/gateway/mock")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payment.gateway.mock-checkout", havingValue = "true")
public class GatewayMockController {

    /** Must match {@code PaymentService.RETURN_URL_PREFIX} — the app intercepts this in the WebView. */
    private static final String RETURN_URL_PREFIX = "https://app.maintenance.example/payment-return";

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @GetMapping(value = "/checkout/{reference}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkoutPage(@PathVariable String reference) {
        Payment p = paymentRepository.findByGatewayReference(reference).orElse(null);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(page("Unknown payment", "<p>This checkout link is not valid.</p>"));
        }
        BigDecimal amount = paymentService.externalAmountOf(p);
        boolean settled = p.getStatus() != PaymentStatus.PENDING;
        String body = settled
                ? "<p class=\"done\">This payment is already <b>" + p.getStatus() + "</b>.</p>"
                : """
                  <div class="amount">%s KD</div>
                  <p class="muted">Booking #%d · ref %s</p>
                  <div class="methods"><span>KNET</span><span>VISA</span><span>mada</span></div>
                  <form method="post" action="%s/complete">
                    <button class="pay" name="result" value="success" type="submit">Pay %s KD</button>
                    <button class="cancel" name="result" value="fail" type="submit">Cancel payment</button>
                  </form>
                  """.formatted(amount.toPlainString(), p.getBooking().getId(), reference,
                                reference, amount.toPlainString());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page("Secure checkout", body));
    }

    @PostMapping("/checkout/{reference}/complete")
    public ResponseEntity<Void> complete(@PathVariable String reference,
                                         @RequestParam(defaultValue = "success") String result) {
        boolean success = "success".equalsIgnoreCase(result);
        PaymentStatus status = paymentService.confirmGatewayCallback(reference, success);
        String location = RETURN_URL_PREFIX
                + "?reference=" + enc(reference)
                + "&status=" + enc(status.name())
                + "&result=" + (success ? "success" : "fail");
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location)
                .build();
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    /** Minimal self-contained MyFatoorah-ish page shell (no external assets — works inside a WebView). */
    private static String page(String title, String body) {
        return """
               <!DOCTYPE html><html lang="en"><head><meta charset="utf-8">
               <meta name="viewport" content="width=device-width, initial-scale=1">
               <title>%s</title>
               <style>
                 body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;background:#0f2a43;margin:0;
                      display:flex;align-items:center;justify-content:center;min-height:100vh;color:#1a1a2e}
                 .card{background:#fff;border-radius:16px;padding:28px 24px;width:88%%;max-width:380px;
                       box-shadow:0 12px 40px rgba(0,0,0,.25);text-align:center}
                 .brand{font-weight:800;color:#0f2a43;letter-spacing:.5px;margin-bottom:18px;font-size:18px}
                 .amount{font-size:34px;font-weight:800;margin:6px 0}
                 .muted{color:#78909c;font-size:13px;margin:0 0 16px}
                 .methods{display:flex;gap:8px;justify-content:center;margin-bottom:20px}
                 .methods span{background:#eef3f7;color:#37474f;border-radius:8px;padding:4px 10px;font-size:12px;font-weight:600}
                 button{display:block;width:100%%;border:0;border-radius:10px;padding:14px;font-size:15px;
                        font-weight:700;margin-top:10px;cursor:pointer}
                 .pay{background:#1565c0;color:#fff} .cancel{background:#f1f3f5;color:#607d8b}
                 .done{color:#2e7d32;font-weight:600}
               </style></head>
               <body><div class="card"><div class="brand">🔒 PAYMENT GATEWAY (demo)</div>%s</div></body></html>
               """.formatted(title, body);
    }
}
