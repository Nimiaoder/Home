package com.liu.dev.user;

import com.liu.dev.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;   // JPA
    private final UserJdbcDao userJdbcDao;         // 直接下 SQL
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserJdbcDao userJdbcDao,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userJdbcDao = userJdbcDao;
        this.passwordEncoder = passwordEncoder;
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

    /** 一般使用者自行註冊：角色固定為 USER，不接受前端指定角色。 */
    public UserAccount registerUser(String username, String rawPassword, String nickname) {
        String nick = (nickname == null || nickname.isBlank()) ? username : nickname.trim();
        UserAccount u = register(username, rawPassword, nick, "USER");
        log.info("新使用者註冊成功：{}", username);
        return u;
    }

    /** 註冊方法：密碼以 BCrypt 雜湊後儲存。（單一 save，由 JPA 自帶交易） */
    private UserAccount register(String username, String rawPassword, String nickname, String role) {
        log.debug("*****UserService.register***** username={}, role={}", username, role);
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
