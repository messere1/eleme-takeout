package cn.edu.tju.takeout.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.order.OrderMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.refund.CreateRefundRequest;
import cn.edu.tju.takeout.refund.RefundController;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FiftyConcurrentRequestsIntegrationTest {
    private static final int CONCURRENCY = 50;
    private static final long USER_ID = 910001L;
    private static final long MERCHANT_ID = 910002L;
    private static final long SHOP_ID = 910003L;
    private static final long CATEGORY_ID = 910004L;
    private static final long PRODUCT_ID = 910005L;
    private static final long PAYMENT_RACE_ORDER_ID = 910010L;
    private static final long RIDER_RACE_ORDER_ID = 910011L;
    private static final long REFUND_RACE_ORDER_ID = 910012L;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProductMapper products;
    @Autowired private OrderMapper orders;
    @Autowired private RefundController refunds;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO users(id, username, phone, password_hash, nickname, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, USER_ID, "concurrency-user", "13700000001", "x".repeat(60),
                "并发测试用户", Timestamp.valueOf(now));
        jdbc.update("""
                INSERT INTO merchants(id, merchant_name, phone, password_hash, business_scope, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, MERCHANT_ID, "并发测试商家", "13700000002", "x".repeat(60),
                "并发测试", Timestamp.valueOf(now));
        jdbc.update("""
                INSERT INTO shops(id, merchant_id, shop_name, status)
                VALUES (?, ?, ?, 'OPEN')
                """, SHOP_ID, MERCHANT_ID, "并发测试店铺");
        jdbc.update("""
                INSERT INTO categories(id, shop_id, name, sort_order)
                VALUES (?, ?, ?, 910000)
                """, CATEGORY_ID, SHOP_ID, "并发测试分类");
        jdbc.update("""
                INSERT INTO products(id, shop_id, category_id, name, price, stock, status, deleted)
                VALUES (?, ?, ?, ?, 10.00, 10, 'ON_SALE', FALSE)
                """, PRODUCT_ID, SHOP_ID, CATEGORY_ID, "并发测试商品");
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void fiftyConcurrentStockDeductionsNeverOversell() throws Exception {
        List<Boolean> results = runConcurrently(
                ignored -> products.decreaseStockIfAvailable(PRODUCT_ID, 1) == 1);

        assertThat(results).containsExactlyInAnyOrderElementsOf(expectedResults(10));
        assertThat(jdbc.queryForObject(
                "SELECT stock FROM products WHERE id = ?", Integer.class, PRODUCT_ID)).isZero();
    }

    @Test
    void fiftyConcurrentPaymentAndTimeoutCancellationProduceOneTerminalWinner() throws Exception {
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(5);
        insertOrder(PAYMENT_RACE_ORDER_ID, "CONCURRENCY-PAY-CANCEL", "CREATED", "UNPAID", deadline);

        List<Boolean> results = runConcurrently(index -> index % 2 == 0
                ? orders.markPaid(PAYMENT_RACE_ORDER_ID, USER_ID, deadline.minusNanos(1)) == 1
                : orders.cancelExpired(PAYMENT_RACE_ORDER_ID, deadline) == 1);

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        String state = jdbc.queryForObject("""
                SELECT status || '/' || payment_status FROM orders WHERE id = ?
                """, String.class, PAYMENT_RACE_ORDER_ID);
        assertThat(state).isIn("CREATED/PAID", "CANCELLED/UNPAID");
    }

    @Test
    void fiftyRidersCompetingForOneOrderProduceOneOwner() throws Exception {
        insertOrder(RIDER_RACE_ORDER_ID, "CONCURRENCY-RIDER", "ACCEPTED", "PAID",
                LocalDateTime.now().plusMinutes(5));

        List<Boolean> results = runConcurrently(index ->
                orders.claimForDelivery(RIDER_RACE_ORDER_ID, 920000L + index) == 1);

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, RIDER_RACE_ORDER_ID))
                .isEqualTo("DELIVERING");
        assertThat(jdbc.queryForObject(
                "SELECT rider_id FROM orders WHERE id = ?", Long.class, RIDER_RACE_ORDER_ID))
                .isBetween(920000L, 920049L);
    }

    @Test
    void fiftyConcurrentRefundRequestsNeverReserveMoreThanOrderTotal() throws Exception {
        insertOrder(REFUND_RACE_ORDER_ID, "CONCURRENCY-REFUND", "COMPLETED", "PAID",
                LocalDateTime.now().plusMinutes(5));
        UserPrincipal customer = new UserPrincipal(USER_ID, "CUSTOMER");
        CreateRefundRequest request = new CreateRefundRequest(
                new BigDecimal("1.00"), "并发退款测试", List.of());

        List<Boolean> results = runConcurrently(ignored -> {
            try {
                refunds.create(customer, REFUND_RACE_ORDER_ID, request);
                return true;
            } catch (BusinessException exception) {
                assertThat(exception.code()).isEqualTo("BUSINESS_CONFLICT");
                return false;
            }
        });

        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(10);
        assertThat(jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount), 0) FROM refund_requests
                WHERE order_id = ? AND status IN ('PENDING', 'APPROVED')
                """, BigDecimal.class, REFUND_RACE_ORDER_ID)).isEqualByComparingTo("10.00");
    }

    private List<Boolean> runConcurrently(IntFunction<Boolean> action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < CONCURRENCY; index++) {
                int requestIndex = index;
                Callable<Boolean> task = () -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("并发请求未能同时开始");
                    }
                    return action.apply(requestIndex);
                };
                futures.add(executor.submit(task));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private List<Boolean> expectedResults(int successCount) {
        List<Boolean> expected = new ArrayList<>();
        for (int index = 0; index < CONCURRENCY; index++) {
            expected.add(index < successCount);
        }
        return expected;
    }

    private void insertOrder(
            long id, String orderNo, String status, String paymentStatus, LocalDateTime deadline) {
        jdbc.update("""
                INSERT INTO orders(
                    id, order_no, user_id, shop_id, total_amount, status,
                    payment_status, payment_deadline, paid_at, created_at)
                VALUES (?, ?, ?, ?, 10.00, ?, ?, ?, ?, ?)
                """, id, orderNo, USER_ID, SHOP_ID, status, paymentStatus,
                Timestamp.valueOf(deadline),
                "PAID".equals(paymentStatus) ? Timestamp.valueOf(deadline.minusMinutes(1)) : null,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    private void cleanFixtures() {
        jdbc.update("DELETE FROM refund_requests WHERE order_id >= 910000");
        jdbc.update("DELETE FROM order_items WHERE order_id >= 910000");
        jdbc.update("DELETE FROM orders WHERE id >= 910000");
        jdbc.update("DELETE FROM products WHERE id >= 910000");
        jdbc.update("DELETE FROM categories WHERE id >= 910000");
        jdbc.update("DELETE FROM shops WHERE id >= 910000");
        jdbc.update("DELETE FROM merchants WHERE id >= 910000");
        jdbc.update("DELETE FROM users WHERE id >= 910000");
    }
}
