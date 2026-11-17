export function quantityMilli(value:string):string{
 if(!/^\d+(\.\d{1,3})?$/.test(value))throw new Error('Quantity must be positive with at most three decimal places.');
 const [whole,part='']=value.split('.');const quantity=BigInt(whole)*1000n+BigInt(part.padEnd(3,'0'));
 if(quantity<=0n||quantity>9223372036854775807n)throw new Error('Quantity is outside the supported range.');
 return String(quantity);
}
