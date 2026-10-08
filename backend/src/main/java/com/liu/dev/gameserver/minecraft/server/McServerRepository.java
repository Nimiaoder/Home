package com.liu.dev.gameserver.minecraft.server;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface McServerRepository extends JpaRepository<McServer, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByPort(int port);

    List<McServer> findByAutoStartTrue();
}
