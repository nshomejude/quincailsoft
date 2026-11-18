package com.quincaisoft.commerce.api;import java.util.*;
public interface CommerceService {
record StoreView(UUID id,String slug,String displayName,boolean enabled,String version){}
record ListingView(UUID id,UUID productId,String title,String description,String unit,String priceXaf,boolean published,String version){}
record PublicItem(UUID id,String title,String description,String unit,String priceXaf){}
record PublicStore(String slug,String displayName,List<PublicItem> items,String availability,String checkoutStatus){}
record Configure(UUID operationId,String slug,String displayName,boolean enabled,long version,String reason){}
record Publish(UUID operationId,UUID productId,String title,String description,boolean published,long version,String reason){}
StoreView store();@com.quincaisoft.shared.TenantMutation StoreView configure(Configure command);List<ListingView> listings();@com.quincaisoft.shared.TenantMutation ListingView publish(Publish command);PublicStore publicStore(String slug);
}