package com.liu.dev.gameserver.minecraft.server;

import com.liu.dev.gameserver.minecraft.version.ServerType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 伺服器版本變更紀錄（版本控管）：每次安裝 / 切換成功都會記一筆。 */
@Entity
@Table(name = "mc_version_log")
public class McVersionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerType type;

    @Column(name = "mc_version", nullable = false, length = 50)
    private String mcVersion;

    @Column(name = "build_id", length = 100)
    private String build;

    /** INSTALL 首次安裝 / CHANGE 切換版本 */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public ServerType getType() { return type; }
    public void setType(ServerType type) { this.type = type; }
    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }
    public String getBuild() { return build; }
    public void setBuild(String build) { this.build = build; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
