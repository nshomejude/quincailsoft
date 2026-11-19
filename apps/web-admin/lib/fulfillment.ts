export type DeliveryDetails = {
    method: 'PICKUP' | 'DELIVERY' | null;
    scheduledDate: string | null;
    address: string | null;
    recipientName: string | null;
    recipientPhone: string | null;
    notes: string | null;
};

export type Fulfillment = {
    id: string;
    saleId: string;
    customerId: string;
    status: 'PENDING' | 'PARTIAL' | 'DISPATCHED' | 'COMPLETED';
    businessDate: string;
    proofReference: string | null;
    version: string;
    delivery: DeliveryDetails | null;
};

export type PickLine = {productId: string; quantityMilli: string};
export type PickList = {fulfillmentId: string; saleId: string; warehouseId: string; status: string; lines: PickLine[]};
export type DispatchEntry = {operationId: string; productId: string; quantityMilli: string; dispatchedAt: string; dispatchedBy: string};
export type FulfillmentPage = {
    items: {fulfillment: Fulfillment; customerName: string; warehouseId: string}[];
    nextCursor: string | null;
};

export const fulfillmentTabs = [
    {status: 'PENDING', label: 'Awaiting pickup'},
    {status: 'PARTIAL', label: 'Partial'},
    {status: 'DISPATCHED', label: 'Dispatched'},
    {status: 'COMPLETED', label: 'Completed'},
    {status: 'ALL', label: 'All'},
] as const;
