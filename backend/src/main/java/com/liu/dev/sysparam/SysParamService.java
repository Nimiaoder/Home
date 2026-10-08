package com.liu.dev.sysparam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/** 系統參數（sys_param 資料表）。直接修改資料表即可生效，不需重啟。 */
@Service
public class SysParamService {

    private static final Logger log = LoggerFactory.getLogger(SysParamService.class);

    /** 是否開放註冊帳號：Y / N */
    public static final String REGISTER_ENABLED = "REGISTER_ENABLED";

    private final JdbcTemplate jdbc;

    public SysParamService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> get(String key) {
        try {
            List<String> v = jdbc.queryForList(
                    "SELECT param_value FROM `sys_param` WHERE param_key = ?", String.class, key);
            log.debug("讀取系統參數 {} = {}", key, v);
            return v.isEmpty() ? Optional.empty() : Optional.ofNullable(v.get(0));
        } catch (DataAccessException e) {
            log.warn("讀取系統參數 {} 失敗：{}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return get(key).map(String::trim)
                .map(v -> v.equalsIgnoreCase("Y") || v.equalsIgnoreCase("true") || v.equals("1"))
                .orElse(def);
    }
}
