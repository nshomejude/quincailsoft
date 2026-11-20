export const options=(rows:{id:string;name?:string;sku?:string}[])=>rows.map(r=>({value:r.id,label:r.sku?r.sku+' · '+r.name:r.name??r.id}));
