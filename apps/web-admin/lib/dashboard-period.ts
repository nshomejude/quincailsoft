export const dashboardPeriods = ['today', 'week', 'month'] as const;
export type DashboardPeriod = typeof dashboardPeriods[number];

export function dashboardPeriod(value?: string): DashboardPeriod {
    return dashboardPeriods.find(period => period === value) ?? 'today';
}

export function dashboardDate(value: string | undefined, fallback: string): string {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value) || value.startsWith('0000')) return fallback;
    const time = Date.parse(value);
    return Number.isFinite(time) && new Date(time).toISOString().slice(0, 10) === value ? value : fallback;
}

export function dashboardRange(date: string, period: DashboardPeriod) {
    const start = new Date(date + 'T00:00:00Z');
    const end = new Date(start);
    if (period === 'week') {
        start.setUTCDate(start.getUTCDate() - (start.getUTCDay() + 6) % 7);
        end.setTime(start.getTime());
        end.setUTCDate(end.getUTCDate() + 6);
    }
    if (period === 'month') {
        start.setUTCDate(1);
        end.setTime(start.getTime());
        end.setUTCMonth(end.getUTCMonth() + 1, 0);
    }
    const iso = (value: Date) => value.toISOString().slice(0, 10);
    return {from: iso(start), to: iso(end)};
}
