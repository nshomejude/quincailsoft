import {quantityMilli} from './input.ts';
import type {PickLine} from './fulfillment.ts';

export function dispatchLines(remaining: PickLine[], quantities: Record<string, string>): PickLine[] {
    const lines: PickLine[] = [];
    for (const line of remaining) {
        const value = (quantities[line.productId] ?? '').trim();
        if (!value || /^0(?:\.0{1,3})?$/.test(value)) continue;
        const quantity = quantityMilli(value);
        if (BigInt(quantity) > BigInt(line.quantityMilli))
            throw new Error('Dispatch quantity exceeds the remaining quantity.');
        lines.push({productId: line.productId, quantityMilli: quantity});
    }
    if (!lines.length) throw new Error('Enter a positive quantity for at least one product.');
    return lines;
}
