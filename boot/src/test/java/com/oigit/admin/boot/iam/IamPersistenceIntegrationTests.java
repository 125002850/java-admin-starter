package com.oigit.admin.boot.iam;

import static org.assertj.core.api.Assertions.assertThat;

import com.oigit.admin.iam.domain.model.IamDept;
import com.oigit.admin.iam.domain.model.IamMenu;
import com.oigit.admin.iam.domain.model.IamRefreshToken;
import com.oigit.admin.iam.domain.model.IamRole;
import com.oigit.admin.iam.domain.model.IamStaff;
import com.oigit.admin.iam.domain.repository.DeptRepository;
import com.oigit.admin.iam.domain.repository.MenuRepository;
import com.oigit.admin.iam.domain.repository.RefreshTokenRepository;
import com.oigit.admin.iam.domain.repository.RoleRepository;
import com.oigit.admin.iam.domain.repository.StaffRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IamPersistenceIntegrationTests {

    @Autowired
    private StaffRepository staffs;
    @Autowired
    private DeptRepository depts;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private MenuRepository menus;
    @Autowired
    private RefreshTokenRepository refreshTokens;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void staffUpdatesRetainOptimisticLockingAcrossDomainConversions() {
        Long id = jdbcTemplate.queryForObject("select id from sys_staff where deleted = 0 order by id limit 1", Long.class);
        assertOptimisticUpdates(() -> staffs.findById(id), staffs::save,
                IamStaff::setRemark, IamStaff::getRemark, IamStaff::getVersion);
    }

    @Test
    void departmentUpdatesRetainOptimisticLockingAcrossDomainConversions() {
        Long id = jdbcTemplate.queryForObject("select id from sys_dept where deleted = 0 order by id limit 1", Long.class);
        assertOptimisticUpdates(() -> depts.findById(id), depts::save,
                IamDept::setRemark, IamDept::getRemark, IamDept::getVersion);
    }

    @Test
    void roleUpdatesRetainOptimisticLockingAcrossDomainConversions() {
        Long id = jdbcTemplate.queryForObject("select id from sys_role where deleted = 0 order by id limit 1", Long.class);
        assertOptimisticUpdates(() -> roles.findById(id), roles::save,
                IamRole::setRemark, IamRole::getRemark, IamRole::getVersion);
    }

    @Test
    void menuUpdatesRetainOptimisticLockingAcrossDomainConversions() {
        Long id = jdbcTemplate.queryForObject("select id from sys_menu where deleted = 0 order by id limit 1", Long.class);
        assertOptimisticUpdates(() -> menus.findById(id), menus::save,
                IamMenu::setRemark, IamMenu::getRemark, IamMenu::getVersion);
    }

    @Test
    void refreshTokenUpdatesRetainOptimisticLockingAcrossDomainConversions() {
        IamRefreshToken token = new IamRefreshToken();
        token.setStaffId(1L);
        token.setTokenHash(UUID.randomUUID().toString());
        token.setSessionId(UUID.randomUUID().toString());
        token.setDeviceId(UUID.randomUUID().toString());
        token.setIssuedTime(LocalDateTime.now());
        token.setExpireTime(LocalDateTime.now().plusDays(1));
        refreshTokens.save(token);

        assertOptimisticUpdates(() -> refreshTokens.findByHash(token.getTokenHash()), refreshTokens::save,
                IamRefreshToken::setUserAgent, IamRefreshToken::getUserAgent, IamRefreshToken::getVersion);
    }

    private <T> void assertOptimisticUpdates(
            Supplier<T> load,
            Consumer<T> save,
            BiConsumer<T, String> change,
            Function<T, String> value,
            Function<T, Integer> version
    ) {
        T current = load.get();
        T stale = load.get();
        int originalVersion = version.apply(current);

        change.accept(current, "first update");
        save.accept(current);
        assertThat(version.apply(current)).isEqualTo(originalVersion + 1);

        change.accept(current, "second update");
        save.accept(current);
        assertThat(version.apply(current)).isEqualTo(originalVersion + 2);

        change.accept(stale, "stale overwrite");
        save.accept(stale);

        T persisted = load.get();
        assertThat(value.apply(persisted)).isEqualTo("second update");
        assertThat(version.apply(persisted)).isEqualTo(originalVersion + 2);
    }
}
