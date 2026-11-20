export function text(fd:FormData,key:string,max=2000){const value=String(fd.get(key)??'').trim();if(!value||value.length>max||/[\u0000-\u001f]/.test(value))throw new Error(`${key}: enter a value within ${max} characters.`);return value;}
export function optionalText(fd:FormData,key:string,max=2000){const value=String(fd.get(key)??'').trim();if(value.length>max||/[\u0000-\u001f]/.test(value))throw new Error(`${key}: invalid text.`);return value;}
export function uuid(value:string){if(!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value))throw new Error('A valid record or operation ID is required.');return value;}
export function version(value:string){if(!/^(0|[1-9][0-9]*)$/.test(value)||!Number.isSafeInteger(Number(value)))throw new Error('Refresh this record before continuing.');return Number(value);}
export function money(value:string){if(!/^(0|[1-9][0-9]*)$/.test(value)||BigInt(value)>9223372036854775807n)throw new Error('Enter an exact nonnegative whole XAF amount.');return value;}
export function billingTotal(subtotal:string,discount:string,tax:string){const s=BigInt(money(subtotal)),d=BigInt(money(discount)),t=BigInt(money(tax));if(s<=d)throw new Error('Discount must be less than subtotal.');const total=s-d+t;if(total>9223372036854775807n)throw new Error('Invoice total exceeds the supported amount.');return total.toString();}
export function date(value:string){if(!/^\d{4}-\d{2}-\d{2}$/.test(value))throw new Error('Use a valid calendar date.');const d=new Date(value+'T00:00:00Z');if(!Number.isFinite(d.getTime())||d.toISOString().slice(0,10)!==value)throw new Error('Use a valid calendar date.');return value;}
export function xaf(value:string,locale:'en'|'fr'='en'){return new Intl.NumberFormat(locale==='fr'?'fr-CM':'en-CM',{maximumFractionDigits:0}).format(BigInt(money(value)))+' XAF';}
export function label(status:string){return status.toLowerCase().replaceAll('_',' ');}

export function sha256(value:string){if(!/^[a-fA-F0-9]{64}$/.test(value))throw new Error('Evidence requires a 64-character SHA-256 digest.');return value.toLowerCase();}
