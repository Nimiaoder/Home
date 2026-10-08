package com.liu.dev.gameserver.minecraft.server;

import com.liu.dev.gameserver.minecraft.version.ServerType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 一個 Minecraft 伺服器（資料庫紀錄）。實際檔案放在 {base}/servers/{dirName}/。 */
@Entity
@Table(name = "mc_server")
public class McServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /** 在 servers/ 底下的資料夾名稱（建立後不可變）。 */
    @Column(name = "dir_name", nullable = false, unique = true, length = 100)
    private String dirName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerType type;

    @Column(name = "mc_version", nullable = false, length = 50)
    private String mcVersion;

    /** 建置 / Loader 版本（Paper build、Fabric loader、Forge 版本）。 */
    @Column(name = "build_id", length = 100)
    private String build;

    @Column(nullable = false)
    private int port = 25565;

    @Column(name = "memory_mb", nullable = false)
    private int memoryMb = 2048;

    /** 指定的 java 執行檔；空白代表依版本自動選擇。 */
    @Column(name = "java_path", length = 300)
    private String javaPath;

    @Column(name = "jvm_args", length = 1000)
    private String jvmArgs;

    @Column(name = "auto_start", nullable = false)
    private boolean autoStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "install_state", nullable = false, length = 20)
    private InstallState installState = InstallState.INSTALLING;

    @Column(name = "install_error", length = 1000)
    private String installError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDirName() { return dirName; }
    public void setDirName(String dirName) { this.dirName = dirName; }
    public ServerType getType() { return type; }
    public void setType(ServerType type) { this.type = type; }
    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }
    public String getBuild() { return build; }
    public void setBuild(String build) { this.build = build; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getMemoryMb() { return memoryMb; }
    public void setMemoryMb(int memoryMb) { this.memoryMb = memoryMb; }
    public String getJavaPath() { return javaPath; }
    public void setJavaPath(String javaPath) { this.javaPath = javaPath; }
    public String getJvmArgs() { return jvmArgs; }
    public void setJvmArgs(String jvmArgs) { this.jvmArgs = jvmArgs; }
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
    public InstallState getInstallState() { return installState; }
    public void setInstallState(InstallState installState) { this.installState = installState; }
    public String getInstallError() { return installError; }
    public void setInstallError(String installError) { this.installError = installError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
