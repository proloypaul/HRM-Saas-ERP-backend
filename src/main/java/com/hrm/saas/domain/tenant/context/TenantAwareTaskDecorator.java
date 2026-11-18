package com.hrm.saas.domain.tenant.context;

import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        TenantContext.TenantInfo context = TenantContext.getTenant();
        return () -> {
            try {
                if (context != null) {
                    TenantContext.setTenant(context);
                }
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
