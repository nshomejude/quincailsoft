package com.quincaisoft.security;

import com.quincaisoft.shared.NewBusinessCommand;
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
        Set<String> commands=new HashSet<>();
        for(Class<?> api:ClassUtils.getAllInterfacesForClass(bean.getClass()))
            for(Method method:api.getMethods()) if(method.isAnnotationPresent(NewBusinessCommand.class)) commands.add(key(method));
        if(commands.isEmpty())return bean;
        ProxyFactory factory=new ProxyFactory(bean);
        factory.addAdvice((MethodInterceptor) invocation -> {
            if(!commands.contains(key(invocation.getMethod())))return invocation.proceed();
            return gate.getObject().execute(invocation.getArguments(),()-> {
                try {return invocation.proceed();}
                catch(RuntimeException|Error e){throw e;}
                catch(Throwable e){throw new IllegalStateException("Economic command failed",e);}
            });
        });
        return factory.getProxy();
    }
    private static String key(Method method){return method.getName()+Arrays.toString(method.getParameterTypes());}
}
