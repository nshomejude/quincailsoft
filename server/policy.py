GROUPS={
'commerce':set('sale quote customer payment deposit order return fulfillment service project'.split()),
'inventory':set('product category unit price movement count transfer warehouse lot custody cut repack recall'.split()),
'purchasing':set('supplier purchase receipt supplier_return requisition rfq supplier_invoice landed_cost advance'.split()),
'finance':set('payment deposit cash expense period journal bank settlement valuation consolidation intercompany dispute'.split()),
'admin':set('company branch staff role tax template printer backup import audit policy device sync quota integration subscription support privacy incident industry content licence companion'.split()),
'platform':{'tenant','support','incident','subscription','recovery'},
}
ALL=set.union(*GROUPS.values())|{'identity','exception','exchange'}
ROLES={
'owner':{'label':'Business owner','read':ALL-{'tenant','recovery','identity'},'write':ALL-{'tenant','recovery','identity'},'approve':True,'export':True,'scope':'all'},
'manager':{'label':'Branch manager','read':GROUPS['commerce']|GROUPS['inventory']|GROUPS['purchasing']|GROUPS['finance']|{'staff','branch','company','audit','incident','exception','exchange'},'write':GROUPS['commerce']|GROUPS['inventory']|GROUPS['purchasing']|{'expense','cash','exception','exchange'},'approve':True,'export':True,'scope':'branch'},
'cashier':{'label':'Cashier','read':{'product','category','unit','price','sale','quote','customer','payment','deposit','cash','return','fulfillment','company','branch'},'write':{'sale','quote','customer','payment','deposit','cash'},'approve':False,'export':False,'scope':'branch'},
'inventory':{'label':'Inventory officer','read':GROUPS['inventory']|{'supplier','purchase','receipt','fulfillment','company','branch'},'write':GROUPS['inventory']|{'receipt','fulfillment'},'approve':False,'export':True,'scope':'branch'},
'purchasing':{'label':'Purchasing officer','read':GROUPS['purchasing']|{'product','warehouse','company','branch','price'},'write':GROUPS['purchasing'],'approve':False,'export':True,'scope':'branch'},
'finance':{'label':'Finance officer','read':GROUPS['finance']|{'sale','purchase','receipt','supplier_invoice','customer','supplier','company','branch','audit'},'write':GROUPS['finance'],'approve':True,'export':True,'scope':'branch'},
'auditor':{'label':'Read-only auditor','read':ALL-{'tenant','recovery','identity'},'write':set(),'approve':False,'export':True,'scope':'all'},
'platform_admin':{'label':'Platform administrator','read':GROUPS['platform'],'write':GROUPS['platform'],'approve':True,'export':True,'scope':'platform'},
'customer':{'label':'Customer portal','read':{'order','sale','fulfillment','return','customer'},'write':{'return'},'approve':False,'export':False,'scope':'customer'},
}
def allow(user,entity,operation='read'):
 p=ROLES[user['role']]
 if entity not in p['read']:return False
 if operation=='read':return True
 if operation=='export':return p['export']
 if entity not in p['write']:return False
 if operation=='approve':return p['approve']
 return True
def scoped(user,record,entity):
 if record.get('tenant_id','demo')!='demo':return False
 role=ROLES[user['role']]
 if role['scope']=='customer':
  return (entity=='customer' and record['id']==user.get('customer_id')) or record.get('customer')==user.get('customer_id')
 if entity=='branch' and role['scope']=='branch':return record['id']==user['branch_id']
 return role['scope'] in ('all','platform') or record.get('branch_id') in (None,user['branch_id'])
def public_policy(role):
 p=ROLES[role]
 return {k:sorted(v) if isinstance(v,set) else v for k,v in p.items()}
