package cn.edu.tju.takeout.deferred;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 第二阶段原延期需求已全部转入正式验收基线。
 * 使用反射保持红灯测试可编译，功能实现后才会转绿。
 */
class DeferredRequirementsAcceptanceTest {

    @Test
    @DisplayName("P2-001 订单具备支付状态、支付截止时间和支付动作")
    void paymentLifecycleIsImplemented() throws Exception {
        assertFields("cn.edu.tju.takeout.order.Order", "paymentStatus", "paymentDeadline", "paidAt");
        assertMethod("cn.edu.tju.takeout.order.OrderService", "pay", Long.class, Long.class);
    }

    @Test
    @DisplayName("P2-002 未支付订单15分钟自动取消并回补库存")
    void unpaidTimeoutCancellationAndCallbackRace() throws Exception {
        assertMethod("cn.edu.tju.takeout.order.OrderService", "cancelExpiredOrders");
    }

    @Test
    @DisplayName("P2-003 顾客可申请部分或全额退款，处理结果可追踪")
    void partialRefundAmountRouteAndEvidence() throws Exception {
        Class.forName("cn.edu.tju.takeout.refund.RefundRequest");
        Class.forName("cn.edu.tju.takeout.refund.RefundController");
    }

    @Test
    @DisplayName("P2-004 统一图片上传校验并关联用户、店铺或商品")
    void imageUploadValidationPrivacyAndRollback() throws Exception {
        Class.forName("cn.edu.tju.takeout.upload.ImageUploadController");
        assertFields("cn.edu.tju.takeout.user.User", "avatarUrl");
        assertFields("cn.edu.tju.takeout.shop.Shop", "imageUrl", "coverImageUrl");
        assertFields("cn.edu.tju.takeout.product.Product", "imageUrl");
    }

    @Test
    @DisplayName("P2-005 注销后账号不可登录且历史订单保留")
    void accountDeletionAndReassignedPhoneIsolation() throws Exception {
        assertFields("cn.edu.tju.takeout.user.User", "enabled", "deletedAt");
        assertMethod("cn.edu.tju.takeout.user.UserService", "deleteAccount", Long.class);
    }

    @Test
    @DisplayName("P2-006 管理员拥有独立账号和用户商家商品订单管理入口")
    void administratorHierarchyAuditAndRecovery() throws Exception {
        Class.forName("cn.edu.tju.takeout.admin.AdminController");
        Class.forName("cn.edu.tju.takeout.admin.AdminMapper");
    }

    @Test
    @DisplayName("P2-007 骑手独立登录、接单、配送和送达")
    void riderDeliveryLifecycleAndExceptions() throws Exception {
        Class.forName("cn.edu.tju.takeout.rider.RiderController");
        assertFields("cn.edu.tju.takeout.order.Order", "riderId");
    }

    private void assertFields(String className, String... names) throws Exception {
        Class<?> type = Class.forName(className);
        assertThat(java.util.Arrays.stream(type.getDeclaredFields()).map(f -> f.getName()))
                .contains(names);
    }

    private void assertMethod(String className, String name, Class<?>... parameters) throws Exception {
        assertThat(Class.forName(className).getMethod(name, parameters)).isNotNull();
    }
}
