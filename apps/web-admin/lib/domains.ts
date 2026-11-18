export type Supplier={id:string;code:string;name:string;phone:string;email:string;active:boolean};
export type Purchase={id:string;supplierId:string;warehouseId:string;businessDate:string;status:string;version:number;totalXaf:string;lines:{id:string;productId:string;quantityMilli:string;receivedMilli:string;unitPriceXaf:string;totalXaf:string}[]};
export type Receipt={id:string;purchaseId:string;businessDate:string;totalXaf:string;lines:{id:string;productId:string;quantityMilli:string;valueXaf:string}[]};
export type CashSession={id:string;tillCode:string;businessDate:string;status:string;openingXaf:string;expectedXaf:string;countedXaf:string;version:number};
export type Deposit={id:string;customerId:string;originalXaf:string;allocatedXaf:string;refundedXaf:string;remainingXaf:string;version:number};
export type DocumentSnapshot={id:string;number:string;kind:string;sourceId:string;businessDate:string;totalXaf:string;sha256:string;content:unknown;issuedAt:string};
