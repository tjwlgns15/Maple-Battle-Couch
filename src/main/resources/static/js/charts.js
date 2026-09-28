/**
 * 기록 화면 공통 차트. replay-detail.js, compare.js 에서 쓴다.
 * window.BattleCoachCharts = { renderShareChart, renderTimelineChart, renderComparisonTimeline, resizeOnWindowChange, readJson }
 */
(function () {
    'use strict';

    const TOP_SKILL_COUNT = 20;
    const ROW_HEIGHT = 24;
    const MIN_SEGMENT_MS = 300; // 시전 1건짜리 시퀀스도 음영이 보이도록 최소 폭을 준다.
    const TICK_INTERVALS = [1, 2, 5, 10, 15, 30, 60]; // 초. 보이는 범위에 눈금이 10개 안팎이 되도록 고른다.
    const TARGET_TICKS = 10;

    const css = getComputedStyle(document.documentElement);
    const color = (name) => css.getPropertyValue(name).trim();

    const HEXA_COLORS = {
        ORIGIN: color('--origin'),
        ASCENT: color('--ascent'),
    };

    /** 점유율 상위 N개 + 나머지는 "기타"로 묶은 가로 막대 */
    function renderShareChart(el, stats) {
        const sorted = [...stats].sort((a, b) => b.damagePercent - a.damagePercent);
        const top = sorted.slice(0, TOP_SKILL_COUNT);
        const rest = sorted.slice(TOP_SKILL_COUNT);
        const rows = top.map((s) => ({
            name: s.skillName,
            value: Number(s.damagePercent),
            damage: s.damage,
            useCount: s.useCount,
        }));
        if (rest.length > 0) {
            rows.push({
                name: `기타 (${rest.length}개)`,
                value: round2(rest.reduce((sum, s) => sum + Number(s.damagePercent), 0)),
                damage: rest.reduce((sum, s) => sum + s.damage, 0),
                useCount: null,
            });
        }

        el.style.height = `${rows.length * 28 + 40}px`;
        const chart = echarts.init(el);
        chart.setOption({
            grid: { left: 8, right: 56, top: 8, bottom: 8, containLabel: true },
            tooltip: {
                trigger: 'item',
                formatter: (p) => {
                    const d = p.data;
                    const lines = [`<b>${escapeHtml(d.name)}</b>`, `점유율 ${d.value}%`, `데미지 ${d.damage.toLocaleString()}`];
                    if (d.useCount !== null) {
                        lines.push(`발동 ${d.useCount.toLocaleString()}회`);
                    }
                    return lines.join('<br>');
                },
            },
            xAxis: { type: 'value', axisLabel: { formatter: '{value}%' }, splitLine: { lineStyle: { type: 'dashed' } } },
            yAxis: { type: 'category', inverse: true, data: rows.map((r) => r.name), axisTick: { show: false } },
            series: [{
                type: 'bar',
                data: rows,
                barMaxWidth: 18,
                itemStyle: { color: color('--accent'), borderRadius: [0, 3, 3, 0] },
                label: { show: true, position: 'right', formatter: '{c}%' },
            }],
        });
        return chart;
    }

    /**
     * 스킬별 한 줄에 시전 시작 시각을 점으로 찍고, 시퀀스 실행 구간과 극딜 구간을 음영으로 표시한다.
     * @param options.maxSec   X축 끝(초). 비교 화면에서 두 차트의 축을 맞출 때 쓴다.
     * @param options.rowOrder 줄 순서(baseName 배열). 없는 스킬은 뒤에 붙는다.
     */
    function renderTimelineChart(el, casts, segments, burstWindows, options = {}) {
        // 시퀀스 안에서는 elapseMs 와 기록 순서가 역전되므로 시간순으로 정렬하되 같은 시각은 기록 순서를 따른다.
        const ordered = [...casts].sort((a, b) => a.elapseMs - b.elapseMs || a.recordedOrder - b.recordedOrder);

        const rowIndex = new Map();
        (options.rowOrder || []).forEach((name) => rowIndex.set(name, rowIndex.size));
        ordered.forEach((c) => {
            if (!rowIndex.has(c.baseName)) {
                rowIndex.set(c.baseName, rowIndex.size);
            }
        });
        const rowNames = [...rowIndex.keys()];

        const points = ordered.map((c) => ({
            value: [c.elapseMs / 1000, rowIndex.get(c.baseName)],
            cast: c,
            itemStyle: HEXA_COLORS[c.hexaType] ? { color: HEXA_COLORS[c.hexaType] } : undefined,
        }));

        // 극딜 구간을 먼저 넣어 시퀀스 음영이 위에 그려지게 한다.
        const areas = [
            ...burstWindows.map((b) => [
                { name: `극딜 구간 (${((b.endMs - b.startMs) / 1000).toFixed(0)}초)`, xAxis: b.startMs / 1000, itemStyle: { color: color('--burst-fill'), borderWidth: 0 } },
                { xAxis: b.endMs / 1000 },
            ]),
            ...segments.map((s) => [
                { name: s.sequenceName || s.sequenceKey, xAxis: s.startMs / 1000 },
                { xAxis: Math.max(s.endMs, s.startMs + MIN_SEGMENT_MS) / 1000 },
            ]),
        ];

        const maxSec = options.maxSec || Math.ceil(Math.max(0, ...ordered.map((c) => c.elapseMs)) / 1000);

        el.style.height = `${rowNames.length * ROW_HEIGHT + 110}px`;
        const chart = echarts.init(el);
        chart.setOption({
            grid: { left: 8, right: 24, top: 24, bottom: 56, containLabel: true },
            tooltip: {
                trigger: 'item',
                formatter: (p) => {
                    if (!p.data || !p.data.cast) {
                        return escapeHtml(p.name || '');
                    }
                    const c = p.data.cast;
                    const lines = [`<b>${escapeHtml(c.skillName)}</b>`, `${(c.elapseMs / 1000).toFixed(2)}초`];
                    if (c.sequenceKey) {
                        lines.push(`시퀀스: ${escapeHtml(c.sequenceName || c.sequenceKey)}`);
                    }
                    return lines.join('<br>');
                },
            },
            xAxis: {
                type: 'value',
                min: 0,
                max: maxSec,
                interval: tickInterval(maxSec),
                axisLabel: { formatter: formatSeconds },
                splitLine: { lineStyle: { type: 'dashed' } },
            },
            yAxis: {
                type: 'category',
                inverse: true,
                data: rowNames,
                axisTick: { show: false },
                splitLine: { show: true, lineStyle: { opacity: 0.4 } },
            },
            dataZoom: [
                { type: 'slider', xAxisIndex: 0, height: 20, bottom: 12, labelFormatter: formatSeconds },
                { type: 'inside', xAxisIndex: 0, zoomOnMouseWheel: 'ctrl', moveOnMouseWheel: false },
            ],
            series: [{
                type: 'scatter',
                symbolSize: 7,
                itemStyle: { color: color('--accent') },
                data: points,
                markArea: {
                    silent: false,
                    itemStyle: { color: 'rgba(216, 69, 59, 0.10)', borderColor: color('--sequence'), borderWidth: 0.5 },
                    label: { show: false },
                    data: areas,
                },
            }],
        });
        chart.on('datazoom', () => {
            const zoom = chart.getOption().dataZoom[0];
            const visibleSec = maxSec * (zoom.end - zoom.start) / 100;
            chart.setOption({ xAxis: { interval: tickInterval(visibleSec) } });
        });
        return chart;
    }

    /**
     * 두 기록을 한 차트에 겹쳐 그린다. 스킬마다 한 줄을 쓰고, 내 기록은 줄 위쪽·기준 기록은 줄 아래쪽에 찍어
     * 같은 스킬을 언제 썼는지 세로로 바로 비교할 수 있게 한다. 극딜 구간은 기록별 색의 옅은 음영으로 표시한다.
     * @param base   { label, casts, bursts }
     * @param target { label, casts, bursts }
     */
    function renderComparisonTimeline(el, base, target, options = {}) {
        const OFFSET = 0.2;
        const sortCasts = (casts) => [...casts].sort((a, b) => a.elapseMs - b.elapseMs || a.recordedOrder - b.recordedOrder);
        const baseCasts = sortCasts(base.casts);
        const targetCasts = sortCasts(target.casts);

        // 줄 순서: 내 기록의 첫 시전 순서, 그다음 기준 기록에만 있는 스킬
        const rowIndex = new Map();
        const rowNames = [];
        [...baseCasts, ...targetCasts].forEach((c) => {
            if (!rowIndex.has(c.baseName)) {
                rowIndex.set(c.baseName, rowNames.length);
                rowNames.push(c.skillName);
            }
        });

        const toPoints = (casts, offset, label) => casts.map((c) => ({
            value: [c.elapseMs / 1000, rowIndex.get(c.baseName) + 0.5 + offset],
            cast: c,
            record: label,
        }));
        const burstAreas = (windows, label, fill) => windows.map((b) => [
            { name: `${label} 극딜 구간 (${((b.endMs - b.startMs) / 1000).toFixed(0)}초)`, xAxis: b.startMs / 1000, itemStyle: { color: fill } },
            { xAxis: b.endMs / 1000 },
        ]);
        // 스킬 줄을 번갈아 옅게 칠해 줄 경계를 보이게 한다.
        const rowBands = rowNames
            .map((_, i) => i)
            .filter((i) => i % 2 === 0)
            .map((i) => [{ yAxis: i, itemStyle: { color: color('--row-band') } }, { yAxis: i + 1 }]);

        const maxSec = options.maxSec
            || Math.ceil(Math.max(0, ...[...baseCasts, ...targetCasts].map((c) => c.elapseMs)) / 1000);

        el.style.height = `${rowNames.length * 34 + 130}px`;
        const chart = echarts.init(el);
        chart.setOption({
            grid: { left: 8, right: 24, top: 40, bottom: 56, containLabel: true },
            legend: { top: 4, data: [base.label, target.label] },
            tooltip: {
                trigger: 'item',
                formatter: (p) => {
                    if (!p.data || !p.data.cast) {
                        return escapeHtml(p.name || '');
                    }
                    const c = p.data.cast;
                    const lines = [`<b>${escapeHtml(c.skillName)}</b>`, `${escapeHtml(p.data.record)} · ${(c.elapseMs / 1000).toFixed(2)}초`];
                    if (c.sequenceKey) {
                        lines.push(`시퀀스: ${escapeHtml(c.sequenceName || c.sequenceKey)}`);
                    }
                    return lines.join('<br>');
                },
            },
            xAxis: {
                type: 'value',
                min: 0,
                max: maxSec,
                interval: tickInterval(maxSec),
                axisLine: { onZero: false }, // Y축을 뒤집어도 X축은 아래에 둔다.
                axisLabel: { formatter: formatSeconds },
                splitLine: { lineStyle: { type: 'dashed' } },
            },
            // 값 축(점 위치와 줄 경계선)과 카테고리 축(줄 가운데 스킬 이름)을 겹쳐 쓴다. 둘 다 같은 높이를 n등분한다.
            yAxis: [
                {
                    type: 'value',
                    inverse: true,
                    min: 0,
                    max: rowNames.length,
                    interval: 1,
                    axisLabel: { show: false },
                    axisTick: { show: false },
                    axisLine: { show: false },
                    splitLine: { show: true, lineStyle: { opacity: 0.35 } },
                },
                {
                    type: 'category',
                    inverse: true,
                    position: 'left',
                    data: rowNames,
                    axisTick: { show: false },
                    axisLine: { show: false },
                    splitLine: { show: false },
                },
            ],
            dataZoom: [
                { type: 'slider', xAxisIndex: 0, height: 20, bottom: 12, labelFormatter: formatSeconds },
                { type: 'inside', xAxisIndex: 0, zoomOnMouseWheel: 'ctrl', moveOnMouseWheel: false },
            ],
            series: [
                {
                    name: '_rows',
                    type: 'scatter',
                    data: [],
                    silent: true,
                    markArea: { silent: true, label: { show: false }, data: rowBands },
                },
                {
                    name: base.label,
                    type: 'scatter',
                    symbolSize: 7,
                    itemStyle: { color: color('--accent') },
                    data: toPoints(baseCasts, -OFFSET, base.label),
                    markArea: { silent: false, label: { show: false }, itemStyle: { borderWidth: 0 },
                        data: burstAreas(base.bursts, base.label, color('--burst-fill')) },
                },
                {
                    name: target.label,
                    type: 'scatter',
                    symbol: 'diamond',
                    symbolSize: 8,
                    itemStyle: { color: color('--compare') },
                    data: toPoints(targetCasts, OFFSET, target.label),
                    markArea: { silent: false, label: { show: false }, itemStyle: { borderWidth: 0 },
                        data: burstAreas(target.bursts, target.label, color('--compare-fill')) },
                },
            ],
        });
        chart.on('datazoom', () => {
            const zoom = chart.getOption().dataZoom[0];
            const visibleSec = maxSec * (zoom.end - zoom.start) / 100;
            chart.setOption({ xAxis: { interval: tickInterval(visibleSec) } });
        });
        return chart;
    }

    function tickInterval(visibleSec) {
        return TICK_INTERVALS.find((sec) => visibleSec / sec <= TARGET_TICKS) || 60;
    }

    function formatSeconds(value) {
        const sec = Math.round(value);
        return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;
    }

    function round2(n) {
        return Math.round(n * 100) / 100;
    }

    function escapeHtml(s) {
        return String(s).replace(/[&<>"']/g, (ch) => ({
            '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
        })[ch]);
    }
    function resizeOnWindowChange(charts) {
        window.addEventListener('resize', () => charts.forEach((chart) => chart.resize()));
    }

    function readJson(id) {
        return JSON.parse(document.getElementById(id).textContent);
    }

    window.BattleCoachCharts = { renderShareChart, renderTimelineChart, renderComparisonTimeline, resizeOnWindowChange, readJson };
})();
