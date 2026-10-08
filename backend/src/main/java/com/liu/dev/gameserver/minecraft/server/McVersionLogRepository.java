package com.liu.dev.gameserver.minecraft.server;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface McVersionLogRepository extends JpaRepository<McVersionLog, Long> {
    List<McVersionLog> findTop50ByServerIdOrderByIdDesc(Long serverId);

    /** 衍生的刪除查詢需要交易，這裡自行宣告。 */
    @Transactional
    void deleteByServerId(Long serverId);
}
