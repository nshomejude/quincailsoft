export type Supplier={id:string;code:string;name:string;phone:string;email:string;active:boolean};
export type Purchase={id:string;supplierId:string;warehouseId:string;businessDate:string;status:string;version:number;totalXaf:string;lines:{id:string;productId:string;quantityMilli:string;receivedMilli:string;unitPriceXaf:string;totalXaf:string}[]};
export type Receipt={id:string;purchaseId:string;businessDate:string;totalXaf:string;lines:{id:string;productId:string;quantityMilli:string;valueXaf:string}[]};
export type CashSession={id:string;tillCode:string;businessDate:string;status:string;openingXaf:string;expectedXaf:string;countedXaf:string;version:number};
export type Deposit={id:string;customerId:string;originalXaf:string;allocatedXaf:string;refundedXaf:string;remainingXaf:string;version:number};
export type Customer = {id: string; name: string; active: boolean};
export type StoreView = {id: string; slug: string; displayName: string; enabled: boolean; version: string};
export type ListingView = {
  id: string;
  productId: string;
  title: string;
  description: string;
  unit: string;
  priceXaf: string;
  published: boolean;
  version: string;
};
export type Branch = {id: string; name: string; timezone: string};
export type Company = {id: string; name: string};
export type SyncDevice = {
  id: string;
  epoch: string;
  actorId: string;
  name: string;
  status: string;
  challenge: string;
  expiresAt: string;
  fingerprint: string;
  version: number;
  offlineSelling: boolean;
};
export type RecoveryReceipt = {
  evidenceId: string;
  sourceOperationId: string;
  status: string;
  payloadHash: string;
  receivedAt: string;
  businessPosted: boolean;
};
export type StockBalance = {
  warehouseId: string;
  productId: string;
  quantityMilli: string;
  reservedMilli: string;
  allocatedMilli: string;
  valueXaf: string;
  version: string;
};
export type ReportResult = {
  code: string;
  basis: string;
  generatedAt: string;
  columns: string[];
  rows: string[][];
  totals: Record<string, string>;
};
export type DocumentSnapshot={id:string;number:string;kind:string;sourceId:string;businessDate:string;totalXaf:string;sha256:string;content:unknown;issuedAt:string};
