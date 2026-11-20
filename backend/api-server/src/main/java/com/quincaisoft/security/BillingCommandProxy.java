package com.quincaisoft.security;

import com.quincaisoft.shared.NewBusinessCommand;
import com.quincaisoft.shared.TenantMutation;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import java.lang.reflect.Method;
import java.util.*;

/** Service-level enforcement also applies to non-HTTP callers using the public API bean. */
@Component
public final class BillingCommandProxy implements BeanPostProcessor {
    private final ObjectProvider<BillingCommandGate> gate;
    public BillingCommandProxy(ObjectProvider<BillingCommandGate> gate) { this.gate=gate; }
    @Override public Object postProcessAfterInitialization(Object bean,String name) {
        Set<String> commands=new HashSet<>(),mutations=new HashSet<>(),replays=new HashSet<>();
        for(Class<?> api:ClassUtils.getAllInterfacesForClass(bean.getClass()))
            for(Method method:api.getMethods()) {
                boolean business=method.isAnnotationPresent(NewBusinessCommand.class);
                boolean mutation=method.isAnnotationPresent(TenantMutation.class);
                if(business)commands.add(key(method));
                if(mutation)mutations.add(key(method));
                if((business||mutation)&&commonReceipt(api,method))replays.add(key(method));
            }
        if(commands.isEmpty()&&mutations.isEmpty())return bean;
        ProxyFactory factory=new ProxyFactory(bean);
        factory.addAdvice((MethodInterceptor) invocation -> {
            boolean billing=commands.contains(key(invocation.getMethod())),tenant=mutations.contains(key(invocation.getMethod()))||billing;
            if(!billing&&!tenant)return invocation.proceed();
            return gate.getObject().execute(invocation.getArguments(),billing,tenant,replays.contains(key(invocation.getMethod())),()-> {
                try {return invocation.proceed();}
                catch(RuntimeException|Error e){throw e;}
                catch(Throwable e){throw new IllegalStateException("Economic command failed",e);}
            });
        });
        return factory.getProxy();
    }
    // Reviewed helpers validate global receipt hash, source, actor and status before mutation.
    // New annotations do not automatically gain a replay exemption.
    private static boolean commonReceipt(Class<?> api,Method method) {
        return (api.getName().equals("com.quincaisoft.sales.api.SalesService") && Set.of("draft","post").contains(method.getName()))
            || (api.getName().equals("com.quincaisoft.purchasing.api.PurchasingService") && Set.of("draft","submit","approve").contains(method.getName()))
            || (api.getName().equals("com.quincaisoft.sales.api.FulfillmentService") && Set.of("draft","dispatch","dispatchLines","complete").contains(method.getName()));
    }
    private static String key(Method method){return method.getName()+Arrays.toString(method.getParameterTypes());}
}
