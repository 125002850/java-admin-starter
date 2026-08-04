package com.oigit.admin.file.domain.service;

import com.oigit.admin.core.exception.BizException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileObjectKeyPolicyTests {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-04T08:00:00Z"), ZONE_ID);

    private final FileObjectKeyPolicy policy = new FileObjectKeyPolicy(ZONE_ID, FIXED_CLOCK);

    @Test
    void should_generate_date_partitioned_object_key_and_preserve_safe_extension() {
        String objectKey = policy.resolveObjectKey("order/export", null, "订单.xlsx");

        assertThat(objectKey)
                .startsWith("order/export/2026/08/04/")
                .endsWith(".xlsx");
    }

    @Test
    void should_use_explicit_object_key_after_validation() {
        assertThat(policy.resolveObjectKey("order/export", "manual/order-1.xlsx", "ignored.csv"))
                .isEqualTo("manual/order-1.xlsx");
    }

    @Test
    void should_reject_path_traversal_and_unsafe_extensions() {
        assertThatThrownBy(() -> policy.normalizeObjectKey("../secret.txt"))
                .isInstanceOf(BizException.class)
                .hasMessage("对象键格式非法");
        assertThatThrownBy(() -> policy.resolveObjectKey("order/export", null, "invoice.a/b"))
                .isInstanceOf(BizException.class)
                .hasMessage("对象键格式非法");
    }

    @Test
    void should_reject_invalid_zone_configuration() {
        assertThatThrownBy(() -> FileObjectKeyPolicy.resolveZoneId("Mars/ERP"))
                .isInstanceOf(BizException.class)
                .hasMessage("文件存储配置非法");
    }
}
