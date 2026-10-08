package com.liu.dev.user;

import com.liu.dev.common.BusinessException;
import com.liu.dev.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;   // JPA
    private final UserJdbcDao userJdbcDao;         // 直接下 SQL
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public UserService(UserRepository userRepository, UserJdbcDao userJdbcDao,
                       PasswordEncoder passwordEncoder, AppProperties props) {
        this.userRepository = userRepository;
        this.userJdbcDao = userJdbcDao;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    /** 以 JPA 查詢 */
    public Optional<UserAccount> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /** 以直接下 SQL 的方式查詢（範例，可依需求替換） */
    public Optional<UserAccount> findByUsernameBySql(String username) {
        return userJdbcDao.findByUsername(username);
    }

    /** 以 SQL 啟用/停用帳號（範例） */
    public void setEnabled(String username, boolean enabled) {
        if (userJdbcDao.updateEnabled(username, enabled) == 0) {
            throw new BusinessException("帳號不存在");
        }
    }

    /**
     * 啟動時依 .env 的 ADMIN_USERNAME / ADMIN_PASSWORD 建立初始管理員（已存在則略過）。
     * 想再建其他帳號：在這裡（或自訂 Runner）呼叫 register(...) 即可，並未對外開放任何 API。
     */
    @Override
    public void run(ApplicationArguments args) {
        var admin = props.admin();
        if (!StringUtils.hasText(admin.username()) || !StringUtils.hasText(admin.password())) return;
        if (userRepository.existsByUsername(admin.username())) return;
        register(admin.username(), admin.password(), admin.nickname(), "ADMIN");
        log.info("已建立初始管理員帳號：{}", admin.username());
    }

    /** 私有註冊方法：只提供內部呼叫，密碼以 BCrypt 雜湊後儲存。（單一 save，由 JPA 自帶交易） */
    private UserAccount register(String username, String rawPassword, String nickname, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("帳號已存在");
        }
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setNickname(nickname);
        u.setRole(role);
        u.setEnabled(true);
        return userRepository.save(u);   // createdAt 由 @PrePersist 自動帶入
    }
}
