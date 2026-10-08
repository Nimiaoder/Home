package com.liu.dev.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** 方式二：直接下 SQL（JdbcTemplate）。複雜查詢、報表、批次作業可用這種方式。 */
@Repository
public class UserJdbcDao {

    private static final RowMapper<UserAccount> MAPPER = (rs, i) -> {
        UserAccount u = new UserAccount();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setNickname(rs.getString("nickname"));
        u.setRole(rs.getString("role"));
        u.setEnabled(rs.getBoolean("enabled"));
        Timestamp t = rs.getTimestamp("created_at");
        if (t != null) u.setCreatedAt(t.toLocalDateTime());
        return u;
    };

    private final JdbcTemplate jdbc;

    public UserJdbcDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findByUsername(String username) {
        List<UserAccount> list = jdbc.query("SELECT * FROM `USER` WHERE username = ?", MAPPER, username);
        return list.stream().findFirst();
    }

    public List<UserAccount> findAll() {
        return jdbc.query("SELECT * FROM `USER` ORDER BY id", MAPPER);
    }

    public int updateEnabled(String username, boolean enabled) {
        return jdbc.update("UPDATE `USER` SET enabled = ? WHERE username = ?", enabled, username);
    }
}
