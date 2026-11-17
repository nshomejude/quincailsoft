import json,re,pathlib
p=pathlib.Path(__file__).parent
r=json.load(open(p/'source-register.json'))
schemas={}
def schema(key,title,fields,detail=None,editor=None):
 out=[]
 for item in fields.split('|'):
  bits=item.split(':');name=bits[0];kind=bits[1] if len(bits)>1 else 'text';required=name.endswith('*');name=name.rstrip('*')
  field={'key':re.sub(r'[^a-z0-9]+','_',name.lower()).strip('_'),'label':name,'type':kind,'required':required,'origin':'UI_IMPLEMENTATION_FIELD'}
  if kind.startswith('@'):field.update(type='relation',entity=kind[1:])
  if kind.startswith('='):field.update(type='select',options=kind[1:].split('/'))
  out.append(field)
 schemas[key]={'title':title,'fields':out,'detail':detail,'editor':editor}
schema('product','Products','SKU*:text|Name*:text|Category:@category|Brand:text|Barcode:text|Base unit*: =piece/bag/metre/litre/box|Pack size:decimal|Selling price*:money|Purchase cost:money|Reorder level:decimal|Tax rate:decimal|Track stock:checkbox|Track serial:checkbox|Track expiry:checkbox|Description:textarea|Status:=Active/Inactive','S11','S12')
schema('category','Categories','Name*:text|Parent category:@category|Brand:text|Description:textarea|Status:=Active/Inactive','S13','S13')
schema('unit','Units & conversions','Name*:text|Symbol*:text|Base unit*:text|Conversion factor*:decimal|Rounding precision:number|Status:=Active/Inactive','S14','S14')
schema('price','Price rules','Product*:@product|Price tier*:=Retail/Wholesale/Contractor|Price*:money|Minimum quantity:decimal|Effective from:date|Effective to:date|Reason:textarea','S15','S15')
schema('customer','Customers','Name*:text|Type:=Individual/Contractor/Company|Phone:tel|Email:email|Address:textarea|Tax reference:text|Credit limit:money|Payment terms days:number|Consent:checkbox|Status:=Active/Inactive','S33','S32')
schema('supplier','Suppliers','Name*:text|Contact person:text|Phone:tel|Email:email|Address:textarea|Tax reference:text|Payment terms days:number|Bank reference:text|Status:=Active/Inactive','S30','S29')
schema('sale','Sales','Customer:@customer|Business date*:date|Channel:=Counter/Online/Mobile|Fulfillment:=Immediate/Customer custody/Delivery|Discount:money|Notes:textarea','S06','S04')
schema('quote','Quotations','Customer*:@customer|Business date*:date|Valid until*:date|Delivery charge:money|Discount:money|Terms:textarea|Notes:textarea','S39','S38')
schema('purchase','Purchase orders','Supplier*:@supplier|Business date*:date|Expected delivery:date|Warehouse*:@warehouse|Currency:=XAF|Supplier reference:text|Notes:textarea','S26','S25')
schema('receipt','Goods receipts','Purchase order*:@purchase|Supplier*:@supplier|Warehouse*:@warehouse|Business date*:date|Supplier delivery reference:text|Exception reason:textarea','S27','S27')
schema('return','Returns','Original sale*:@sale|Customer:@customer|Business date*:date|Reason*:textarea|Condition:=Sellable/Damaged/Quarantine|Refund method:=Cash/Mobile money/Bank/Customer credit','S08','S08')
schema('supplier_return','Supplier returns','Goods receipt*:@receipt|Supplier*:@supplier|Business date*:date|Reason*:textarea|Disposition:=Return to supplier/Quarantine|Amount:money','S28','S28')
schema('payment','Payments','Customer:@customer|Supplier:@supplier|Sale:@sale|Purchase order:@purchase|Business date*:date|Amount*:money|Tender*:=Cash/Mobile money/Bank/Deposit|External reference:text|Allocation amount:money|Reason:textarea','S09','S35')
schema('deposit','Customer deposits','Customer*:@customer|Business date*:date|Amount*:money|Allocated amount:money|Tender:=Cash/Mobile money/Bank|Reference:text|Notes:textarea','S77','S76')
schema('movement','Stock movements','Product*:@product|Warehouse*:@warehouse|Business date*:date|Quantity*:decimal|Direction:=Receipt/Issue/Adjustment|Source reference:text|Reason*:textarea','S18','S20')
schema('count','Stock counts','Warehouse*:@warehouse|Business date*:date|Assigned operator:@staff|Freeze strategy:=Snapshot/Freeze location|Notes:textarea','S22','S21')
schema('transfer','Stock transfers','Origin warehouse*:@warehouse|Destination warehouse*:@warehouse|Business date*:date|Expected arrival:date|Carrier:text|Discrepancy reason:textarea','N06','N06')
schema('fulfillment','Pickup & delivery','Sale*:@sale|Customer*:@customer|Warehouse:@warehouse|Method:=Pickup/Delivery|Scheduled date:date|Address:textarea|Recipient name:text|Recipient phone:tel|Proof reference:text|Notes:textarea','S75','S74')
schema('expense','Expenses','Category*:text|Payee*:text|Business date*:date|Amount*:money|Tender:=Cash/Mobile money/Bank|Description*:textarea|Evidence:file|Approval reason:textarea','S44','S84')
schema('cash','Cash sessions','Register*:text|Operator:@staff|Business date*:date|Opening amount*:money|Expected amount:money|Counted amount:money|Variance reason:textarea','S42','S40')
schema('company','Companies','Legal name*:text|Trading name:text|Registration number:text|Tax reference:text|Phone:tel|Email:email|Address:textarea|Timezone:=Africa/Douala|Currency:=XAF|Locale:=English/French|Logo:file','S57','S57')
schema('branch','Branches','Name*:text|Company*:@company|Address:textarea|Phone:tel|Timezone:=Africa/Douala|Business day cut off:time|Status:=Active/Inactive','N04','N04')
schema('warehouse','Warehouses','Name*:text|Branch*:@branch|Company*:@company|Bin code:text|Storage condition:=Ambient/Dry/Cold/Quarantine|Status:=Active/Inactive','N05','N05')
schema('staff','Staff','Name*:text|Email*:email|Phone:tel|Role:@role|Branch:@branch|Company:@company|Invitation expires:date|Status:=Invited/Active/Inactive','S55','S55')
schema('role','Roles','Name*:text|Description:textarea|Read records:checkbox|Create drafts:checkbox|Approve:checkbox|Export:checkbox|Manage configuration:checkbox|Branch:@branch','S56','S56')
schema('identity','Identity','Email*:email|Password*:password|Confirm password:password|Verification code:text|Recovery method:=Email/Recovery code|Locale:=English/French','S71','S68')
schema('tax','Tax & numbering','Tax name*:text|Tax rate*:decimal|Price includes tax:checkbox|Company*:@company|Document type:=Invoice/Receipt/Credit note|Number prefix*:text|Next sequence:number|Effective from:date|Policy reason:textarea','S58','S58')
schema('template','Document templates','Name*:text|Document family*:text|Version*:text|Locale:=English/French|Paper:=A4/58mm/80mm|Footer:textarea|Show logo:checkbox|Copy marker:checkbox','S59','S59')
schema('printer','Printers','Name*:text|Printer profile:=A4/58mm/80mm|Connection:=Browser/Windows agent|Destination:text|Margins mm:decimal|Copies:number|Default printer:checkbox','S60','S60')
schema('backup','Backups & recovery','Name*:text|Backup reference:text|Recovery point:date|Destination:text|Retention days:number|Checksum:text|Manifest:file|Reason*:textarea','S62','S61')
schema('import','Import & export jobs','Name*:text|Dataset:=Products/Opening stock/Customer balances/Supplier balances|Input file:file|Encoding:=UTF-8|Delimiter:=Comma/Semicolon|Mapping:textarea|Validation notes:textarea','S63','S63')
schema('audit','Audit events','Event type:text|Source reference:text|Actor:text|Business date:date|Operation ID:text|Before:textarea|After:textarea|Reason:textarea','S64',None)
schema('policy','Configuration','Name*:text|Scope:=Company/Branch/Device|Company:@company|Branch:@branch|Effective from:date|Enabled:checkbox|Threshold:money|Policy value:textarea|Reason*:textarea','S81','S81')
schema('period','Financial periods','Name*:text|Company*:@company|Start date*:date|End date*:date|Opening balance:money|Close reason:textarea','S82','S82')
schema('device','Devices','Name*:text|Branch*:@branch|Platform:=Windows/Android/iOS|Fingerprint:text|Epoch:number|Last acknowledged sequence:number|Pairing challenge:text|Expires:date|Reason:textarea','N08','N14')
schema('sync','Synchronization','Device*:@device|Operation ID*:text|Source reference:text|Expected version:number|Sequence:number|Receipt digest:text|Evidence:file|Resolution reason:textarea','N15','N15')
schema('quota','Offline allocations','Device*:@device|Product*:@product|Granted quantity*:decimal|Consumed quantity:decimal|Expiry:date|Grant reference:text|Reason*:textarea','N16','N16')
schema('integration','Integrations','Name*:text|Provider:=MTN MoMo/Orange Money/Bank/Other|Merchant account:text|Environment:=Sandbox/Live|Secret reference:text|Callback URL:url|Enabled:checkbox','N09','N09')
schema('settlement','Provider settlements','Provider*:text|Provider reference*:text|Payment:@payment|Gross amount*:money|Fees:money|Net amount:money|Bank reference:text|Business date*:date|Resolution reason:textarea','N10','N10')
schema('subscription','Subscriptions','Tenant*:text|Plan:=Starter/Business/Enterprise|Billing cycle:=Monthly/Annual|Effective date:date|Amount:money|Seats:number|Branches:number|Invoice reference:text|Reason:textarea','N02','N03')
schema('support','Support grants','Tenant*:text|Requested scope*:textarea|Requester:text|Approver:text|Expires*:date|Reason*:textarea','N13','N13')
schema('tenant','Tenants','Name*:text|Slug*:text|Region*:text|Isolation:=Shared/Dedicated|Plan:@subscription|Company:@company|Release cohort:text|Reason:textarea','N12','N12')
schema('order','Commerce orders','Customer*:@customer|Business date*:date|Delivery address:textarea|Fulfillment branch:@branch|Delivery estimate:money|Payment reference:text|Return reason:textarea','N27','N26')
schema('content','Public content','Title*:text|Slug*:text|Locale:=English/French|Body:textarea|Meta title:text|Meta description:textarea|Domain:text|Policy version:text','N36','N36')
schema('industry','Industry profiles','Name*:text|Enable serials:checkbox|Enable expiry:checkbox|Enable cutting:checkbox|Enable repack:checkbox|Enable services:checkbox|Migration notes:textarea','N30','N30')
schema('lot','Serials & batches','Product*:@product|Serial number:text|Batch number:text|Expiry:date|Warehouse*:@warehouse|Quantity*:decimal|Parent lot:text|Condition:=Available/Quarantine/Recalled','N31','N31')
schema('privacy','Privacy requests','Requester*:text|Verified identity:checkbox|Request type:=Access/Rectify/Restrict/Erase|Received date*:date|Due date*:date|Legal hold:checkbox|Systems affected:textarea|Outcome:textarea','N33','N33')
schema('incident','Incidents','Title*:text|Severity:=Low/Medium/High/Critical|Client version:text|Server version:text|Source reference:text|Redacted diagnostics:textarea|Assigned to:@staff|Resolution:textarea','N34','N34')
schema('requisition','Requisitions','Requester*:@staff|Desired date*:date|Budget*:money|Warehouse:@warehouse|Justification*:textarea','X02','X02')
schema('rfq','RFQ comparison','Supplier*:@supplier|Product*:@product|Quantity*:decimal|Unit price*:money|Freight:money|Terms:textarea|Offer expiry:date|Award reason:textarea','X03','X03')
schema('supplier_invoice','Supplier invoice matching','Supplier*:@supplier|Supplier reference*:text|Purchase order*:@purchase|Goods receipt*:@receipt|Invoice date*:date|Amount*:money|Tax amount:money|Discrepancy amount:money|Exception reason:textarea','X04','X04')
schema('landed_cost','Landed costs','Goods receipt*:@receipt|Cost type:=Freight/Customs/Insurance/Other|Amount*:money|Allocation driver:=Quantity/Value/Weight|Business date*:date|Reason*:textarea','X05','X05')
schema('valuation','Valuation adjustments','Product*:@product|Original cost*:money|Revised cost*:money|Remaining quantity*:decimal|Issued quantity:decimal|Period:@period|Reason*:textarea','X06','X06')
schema('advance','Supplier advances','Supplier*:@supplier|Amount*:money|Allocated amount:money|Business date*:date|Payment reference:text|Reason:textarea','X07','X07')
schema('journal','Operational journals','Batch reference*:text|Account*:text|Debit:money|Credit:money|Source reference*:text|Period:@period|Business date*:date|Reversal reason:textarea','X08','X08')
schema('bank','Bank reconciliation','Statement reference*:text|Business date*:date|Bank amount*:money|Matched amount:money|Payment:@payment|Fee amount:money|Variance reason:textarea','X09','X09')
schema('dispute','Disputes & chargebacks','Provider reference*:text|Payment*:@payment|Amount*:money|Evidence:file|Deadline*:date|Assigned to:@staff|Resolution:textarea','X10','X10')
schema('custody','Customer custody','Sale*:@sale|Customer*:@customer|Product*:@product|Owned quantity*:decimal|Collected quantity:decimal|Warehouse*:@warehouse|Collection date:date|Loss reason:textarea','X11','X11')
schema('exception','Offline completed exceptions','Device*:@device|Grant reference*:text|Source receipt*:text|Cash amount:money|Goods proof:textarea|Evidence:file|Investigator:@staff|Resolution*:textarea','X12','X12')
schema('intercompany','Intercompany trade','Seller company*:@company|Buyer company*:@company|Order reference*:text|Tax amount:money|Dispatch reference:text|Receipt reference:text|Difference reason:textarea','X14','X14')
schema('consolidation','Consolidation','Company*:@company|Period*:@period|Pair reference:text|Elimination debit:money|Elimination credit:money|Unmatched amount:money|Reason:textarea','X15','X15')
schema('cut','Cut-to-size jobs','Parent product*:@product|Parent length*:decimal|Output length*:decimal|Output count*:number|Kerf*:decimal|Waste:decimal|Unit:=metre/millimetre|Reason:textarea','X16','X16')
schema('repack','Repack & kits','Component product*:@product|Component quantity*:decimal|Output product*:@product|Output quantity*:decimal|Loss quantity:decimal|Reason:textarea','X17','X17')
schema('project','Contractor projects','Name*:text|Customer*:@customer|Budget*:money|Credit limit:money|Start date:date|End date:date|Site address:textarea|Notes:textarea','X18','X18')
schema('service','Services','Service code*:text|Name*:text|Price*:money|Tax rate:decimal|Completion date:date|Description:textarea','X19','X19')
schema('exchange','Exchanges','Original sale*:@sale|Return*:@return|Replacement sale:@sale|Credit amount*:money|Replacement amount*:money|Balance payment:money|Reason*:textarea','X20','X20')
schema('recall','Recall & quarantine','Lot*:@lot|Supplier:@supplier|Affected quantity*:decimal|Reason*:textarea|Notice:textarea|Disposition:=Block/Recover/Dispose|Evidence:file','X21','X21')
schema('recovery','Tenant recovery','Tenant*:@tenant|Recovery point*:date|Manifest:file|Expected records:number|Recovered records:number|Difference explanation:textarea|Approval reason*:textarea','X22','X22')
schema('licence','Perpetual deployments','Company*:@company|Version*:text|Deployment:=Standalone/On premises/Hosted|Signed scope reference:text|Maintenance expiry:date|Backup reference:text|Host fingerprint:text','X23','X23')
schema('companion','Mobile companion','Device*:@device|Pairing reference*:text|Snapshot date:date|Package file:file|Draft count:number|Approval reason:textarea','X24','X24')
# Normalize accidental whitespace in field type.
for sch in schemas.values():
 for f in sch['fields']:
  f['type']=f['type'].strip()
  if f['type'].startswith('='):f['options']=f['type'][1:].split('/');f['type']='select'
S='dashboard audit policy sale sale sale sale return payment product product product category unit price product movement movement movement movement count count product purchase purchase purchase receipt supplier_return supplier supplier payment customer customer customer payment customer quote quote quote cash cash cash expense expense report report report report report report report report document document staff role company tax template printer backup backup import audit incident company company identity import printer identity identity identity fulfillment fulfillment deposit deposit payment backup incident policy period policy expense expense policy policy policy'.split()
N='identity subscription subscription branch warehouse transfer staff device integration settlement support tenant support device sync quota device sale count sync fulfillment expense product product order order order content order industry lot privacy privacy incident subscription content customer fulfillment device consolidation'.split()
X='company requisition rfq supplier_invoice landed_cost valuation advance journal bank dispute custody exception device intercompany consolidation cut repack project service exchange recall recovery licence companion'.split()
assert len(S)==88,len(S)
assert len(N)==40
assert len(X)==24
list_ids='S02 S03 S05 S07 S09 S10 S13 S14 S15 S16 S17 S19 S21 S23 S24 S28 S29 S32 S34 S37 S41 S43 S53 S55 S56 S61 S64 S74 S76 S80 S85 S86 N02 N04 N05 N07 N08 N12 N13 N14 N15 N16 N19 N20 N21 N22 N29 N31 N32 N33 N34 N35 N37 N38 X02 X03 X08 X10 X11 X12 X13 X14 X15 X18 X21 X22 X23 X24'.split()
detail_ids='S06 S11 S18 S22 S26 S30 S33 S39 S44 S54 S75 S77 N03 N11 N24 N27 N40'.split()
report_ids='S45 S46 S47 S48 S49 S50 S51 S52 S36 N40'.split()
auth_ids='S68 S71 S72 S73 N01'.split()
line_entities={'sale','quote','purchase','receipt','count','transfer','return','supplier_return','fulfillment','order','requisition'}
# Explicit list-to-detail/editor register; every entity has addressable detail even where source has no dedicated screen.
for s in r['screens']:
 id=s['id'];e=({'S':S,'N':N,'X':X}[id[0]])[int(id[1:])-1]
 kind='list' if id in list_ids else 'detail' if id in detail_ids else 'form'
 if id in report_ids:kind='reports'
 if id in ['S01']:kind='dashboard'
 if id in ['S04','N18']:kind='pos'
 if id in auth_ids:kind='identity'
 if id=='S53':kind='documents'
 if id=='S54':kind='document'
 if id in ['N23','N24','N25','N26']:kind='commerce'
 tabs=[v.strip() for v in s['fields'].split(',')] if id[0]=='S' else ['Overview','Related records','History']
 path=s['route'].split('?')[0];path=re.sub(r'\[[^]]+\]','demo',path)
 s.update(entity=e,kind=kind,tabs=tabs,canonical_route=s['route'],path=path,has_lines=e in line_entities)
 if e in schemas:s['form_fields']=schemas[e]['fields']
 else:s['form_fields']=[]
 s['status']='LOCAL_UI_IMPLEMENTATION'
# Detailed report inventory preserving every source family/submetric and added reports.
reports=[]
source=json.load(open('scaffold/contracts/architecture.json'))
text=next(s['markdown'] for s in source['sections'] if s['section_id']=='16')
mapping={'Executive':('sale','S06'),'Sales':('sale','S06'),'Stock':('movement','S18'),'Purchasing':('purchase','S26'),'Customer':('customer','S33'),'Supplier':('supplier','S30'),'Finance':('payment','S09'),'Commerce':('order','N27'),'Operations':('sync','N15'),'Compliance':('audit','S64')}
for family,items in re.findall(r'^- ([^:]+): (.+)$',text,re.M):
 for item in items.rstrip('.').split(','):
  name=item.strip()
  e,target=mapping[family]
  reports.append({'id':'R'+str(len(reports)+1).zfill(3),'name':name[0].upper()+name[1:],'family':family,'entity':e,'target':target,'columns':['Reference','Business date','Status','Source total'],'grain':'Source record','date_basis':'business_date','currency':'XAF','freshness':'Local browser state','metric_status':'SOURCE_ROWS_ONLY','reconciliation':'Totals reconcile to displayed source records; named analytical metric requires its dedicated production calculation.'})
extra=[('Physical-versus-owned stock bridge','custody','X11'),('Customer custody aging','custody','X11'),('Offline exposure and exception aging','exception','X12'),('GRNI aging','receipt','S27'),('Receipt-to-invoice variance','supplier_invoice','X04'),('Landed-cost adjustment bridge','landed_cost','X05'),('Inventory value rollforward','valuation','X06'),('Operational trial balance','journal','X08'),('Bank/provider clearing aging','bank','X09'),('Intercompany unmatched balances','intercompany','X14'),('Project exposure','project','X18'),('Cutting yield/waste','cut','X16'),('Recall reach','recall','X21'),('Recovery reconciliation','recovery','X22')]
for name,e,target in extra:reports.append({'id':'R'+str(len(reports)+1).zfill(3),'name':name,'family':'Extended operations','entity':e,'target':target,'grain':'Source record','date_basis':'business_date','currency':'XAF','freshness':'Local browser state','metric_status':'SOURCE_ROWS_ONLY'})
docdefs=[
('Sales receipt','sale','receipt'),('Sales invoice / commercial invoice','sale','commercial'),('Quotation','quote','commercial'),('Proforma invoice','quote','commercial'),('Purchase order','purchase','commercial'),('Goods received note','receipt','quantity'),('Supplier return note','supplier_return','quantity'),('Customer account statement','customer','statement'),('Customer payment/deposit receipt','payment','payment'),('Credit note','return','commercial'),('Refund voucher','return','payment'),('Delivery/handover note','fulfillment','quantity'),('Stock count/adjustment sheet','count','count'),('Expense voucher','expense','payment'),('End-of-day closing report','cash','cash'),('Transfer dispatch/receiving note','transfer','quantity'),('Subscription invoice/receipt','subscription','payment'),('Customer custody/pickup balance note','custody','custody'),('Supplier invoice-match exception approval','supplier_invoice','approval'),('Landed-cost/valuation adjustment voucher','landed_cost','allocation'),('Operational journal/reversal voucher','journal','journal'),('Intercompany reconciliation statement','intercompany','statement'),('Offline exception resolution','exception','approval'),('Cut/repack production label','cut','label'),('Recall notice','recall','notice'),('Deployment/restore acceptance report','recovery','acceptance')]
docs=[{'id':'DOC'+str(i+1).zfill(2),'title':n,'entity':e,'layout':l,'paper':'80mm' if i==0 else 'A4','source_screen':schemas[e]['detail'],'fields':schemas[e]['fields'],'immutable_snapshot':'LOCAL_SNAPSHOT_ONLY','production_certification':'NOT_VERIFIED'} for i,(n,e,l) in enumerate(docdefs)]
out={'version':3,'screens':r['screens'],'schemas':schemas,'reports':reports,'documents':docs,'modules':r['modules'],'limitations':['No production backend or authorization','Analytical reports show source datasets pending certified metric formulas','Architecture fields described only at group level have authored UI fields, not independently certified completeness']}
(p/'contracts.json').write_text(json.dumps(out,indent=2))
(p/'contracts.js').write_text('window.APP_CONTRACTS='+json.dumps(out)+';')
print(len(r['screens']),len(schemas),sum(len(s['form_fields']) for s in r['screens']),len(reports),len(docs))
