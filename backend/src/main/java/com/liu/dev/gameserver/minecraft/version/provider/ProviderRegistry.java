package com.liu.dev.gameserver.minecraft.version.provider;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.ServerType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 依 ServerType 取得對應 Provider。新增 Provider 只要是 @Component 就會自動註冊。 */
@Component
public class ProviderRegistry {

    private final Map<ServerType, ServerProvider> byType = new EnumMap<>(ServerType.class);

    public ProviderRegistry(List<ServerProvider> providers) {
        for (ServerProvider p : providers) byType.put(p.type(), p);
    }

    public ServerProvider get(ServerType type) {
        ServerProvider p = byType.get(type);
        if (p == null) throw new BusinessException("不支援的伺服器類型：" + type);
        return p;
    }
}
