package expensetracker.handler;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import expensetracker.dao.SubscriptionDao;
import expensetracker.model.User;
import io.javalin.http.Context;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class PaymentHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentHandler.class);
    private static final int PREMIUM_AMOUNT_PAISE = 4900; // ₹49 in paise
    private static final double PREMIUM_AMOUNT = 49.0;

    private final RazorpayClient razorpayClient;
    private final String keyId;
    private final SubscriptionDao subscriptionDao = new SubscriptionDao();

    public PaymentHandler() {
        String keyId = System.getenv("RAZORPAY_KEY_ID");
        String keySecret = System.getenv("RAZORPAY_KEY_SECRET");
        if (keyId == null || keySecret == null) {
            log.warn("RAZORPAY_KEY_ID or RAZORPAY_KEY_SECRET not set. Payment features will not work.");
            this.razorpayClient = null;
            this.keyId = null;
        } else {
            try {
                this.razorpayClient = new RazorpayClient(keyId, keySecret);
                this.keyId = keyId;
                log.info("Razorpay client initialized (key: {}...)", keyId.substring(0, Math.min(12, keyId.length())));
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Razorpay client", e);
            }
        }
    }

    public void createOrder(Context ctx) {
        if (razorpayClient == null) {
            ctx.status(503).json(Map.of("error", "Payment service not configured"));
            return;
        }
        User user = ctx.attribute("user");
        try {
            JSONObject orderReq = new JSONObject();
            orderReq.put("amount", PREMIUM_AMOUNT_PAISE);
            orderReq.put("currency", "INR");
            orderReq.put("receipt", "premium_" + user.getId().substring(0, 8));
            orderReq.put("notes", new JSONObject().put("userId", user.getId()));

            Order order = razorpayClient.orders.create(orderReq);

            ctx.json(Map.of(
                "orderId", order.get("id"),
                "amount", PREMIUM_AMOUNT_PAISE,
                "currency", "INR",
                "keyId", keyId
            ));
        } catch (Exception e) {
            log.error("Failed to create Razorpay order", e);
            ctx.status(500).json(Map.of("error", "Failed to create payment order"));
        }
    }

    public void verifyPayment(Context ctx) {
        if (razorpayClient == null) {
            ctx.status(503).json(Map.of("error", "Payment service not configured"));
            return;
        }
        User user = ctx.attribute("user");
        try {
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String orderId = body.get("razorpay_order_id");
            String paymentId = body.get("razorpay_payment_id");
            String signature = body.get("razorpay_signature");

            if (orderId == null || paymentId == null || signature == null) {
                ctx.status(400).json(Map.of("error", "Missing payment details"));
                return;
            }

            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            boolean valid = Utils.verifyPaymentSignature(attributes, System.getenv("RAZORPAY_KEY_SECRET"));
            if (!valid) {
                ctx.status(400).json(Map.of("error", "Payment verification failed"));
                return;
            }

            LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
            String subId = UUID.randomUUID().toString();
            Map<String, Object> sub = subscriptionDao.insert(
                subId, user.getId(), orderId, paymentId,
                PREMIUM_AMOUNT, Timestamp.valueOf(expiresAt)
            );

            log.info("Premium subscription activated for user {} (payment: {})", user.getId(), paymentId);
            ctx.json(Map.of("success", true, "subscription", sub));
        } catch (Exception e) {
            log.error("Payment verification failed", e);
            ctx.status(500).json(Map.of("error", "Payment verification failed"));
        }
    }

    public void status(Context ctx) {
        User user = ctx.attribute("user");
        Map<String, Object> active = subscriptionDao.findActiveByUserId(user.getId());
        if (active != null) {
            ctx.json(Map.of("active", true, "subscription", active));
        } else {
            ctx.json(Map.of("active", false));
        }
    }
}
