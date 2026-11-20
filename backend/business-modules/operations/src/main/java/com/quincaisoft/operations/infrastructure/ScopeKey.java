package com.quincaisoft.operations.infrastructure;
import java.io.Serializable;import java.util.*;
public class ScopeKey implements Serializable {public UUID tenantId,companyId,id;public ScopeKey(){}public ScopeKey(UUID t,UUID c,UUID i){tenantId=t;companyId=c;id=i;}public boolean equals(Object o){return o instanceof ScopeKey k&&Objects.equals(tenantId,k.tenantId)&&Objects.equals(companyId,k.companyId)&&Objects.equals(id,k.id);}public int hashCode(){return Objects.hash(tenantId,companyId,id);}}
