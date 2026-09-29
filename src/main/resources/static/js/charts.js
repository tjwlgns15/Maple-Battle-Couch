/**
 * 기록 화면 공통 차트. replay-detail.js, compare.js 에서 쓴다.
 * window.BattleCoachCharts 로 차트 함수를 내보낸다(파일 끝 참고).
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

    // 어두운 천공 테마(app.css 토큰)에 맞춘 ECharts 테마. 모든 차트를 이 테마로 만든다.
    const THEME = 'battle-coach';
    const axisStyle = {
        axisLine: { lineStyle: { color: color('--axis-line') } },
        axisTick: { lineStyle: { color: color('--axis-line') } },
        axisLabel: { color: color('--muted') },
        splitLine: { lineStyle: { color: color('--grid-line') } },
    };
    echarts.registerTheme(THEME, {
        backgroundColor: 'transparent',
        textStyle: { color: color('--text'), fontFamily: getComputedStyle(document.body).fontFamily },
        legend: { textStyle: { color: color('--text') }, inactiveColor: color('--muted') },
        tooltip: {
            backgroundColor: 'rgba(10, 15, 36, 0.95)',
            borderColor: color('--border-strong'),
            textStyle: { color: color('--text') },
        },
        categoryAxis: axisStyle,
        valueAxis: axisStyle,
        dataZoom: {
            borderColor: color('--border'),
            fillerColor: color('--accent-weak'),
            textStyle: { color: color('--muted') },
            handleStyle: { color: color('--accent'), borderColor: color('--accent') },
            moveHandleStyle: { color: color('--accent') },
            dataBackground: {
                lineStyle: { color: color('--grid-line') },
                areaStyle: { color: color('--grid-line') },
            },
            selectedDataBackground: {
                lineStyle: { color: color('--accent') },
                areaStyle: { color: color('--accent-weak') },
            },
        },
    });

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
        const chart = echarts.init(el, THEME);
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
                // 청록 빛기둥처럼 오른쪽으로 밝아지는 막대
                itemStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                        { offset: 0, color: color('--accent-strong') },
                        { offset: 1, color: color('--accent') },
                    ]),
                    borderRadius: [0, 4, 4, 0],
                    shadowBlur: 8,
                    shadowColor: color('--accent-weak'),
                },
                label: { show: true, position: 'right', formatter: '{c}%', color: color('--text') },
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
        const chart = echarts.init(el, THEME);
        chart.setOption({
            animation: false, // 확대·이동할 때마다 점 수백 개가 전환 애니메이션으로 따라오며 느려진다
            grid: { left: 20, right: 24, top: 24, bottom: 56, containLabel: true },
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
                    itemStyle: { color: color('--sequence-fill'), borderColor: color('--sequence'), borderWidth: 0.5 },
                    label: { show: false },
                    data: areas,
                },
            }],
        });
        adjustTicksOnZoom(chart, maxSec);
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
        const chart = echarts.init(el, THEME);
        chart.setOption({
            animation: false, // 확대·이동할 때마다 점 수백 개가 전환 애니메이션으로 따라오며 느려진다
            grid: { left: 20, right: 24, top: 40, bottom: 56, containLabel: true },
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
        adjustTicksOnZoom(chart, maxSec);
        return chart;
    }

    /**
     * 시전 횟수 차이. 스킬마다 기준 기록(전투 시간 보정) 대비 몇 % 더·덜 썼는지를 0을 가운데 둔 가로 막대로 그린다.
     * 기본 공격(수백 회)과 쿨기(수 회)를 같은 눈금에 놓기 위해 횟수가 아니라 비율을 쓴다. 차이가 1회 미만이면 뺀다.
     * @param skills  ReplayComparison.SkillRow 목록
     * @param scale   기준 기록 시전 수를 내 전투 시간에 맞춘 배율
     * @param summary 뺀 스킬 수를 적을 요소
     */
    function renderCastGapChart(el, skills, scale, summary) {
        const rows = skills
            .filter((s) => s.baseCasts > 0 || s.targetCasts > 0)
            .map((s) => {
                const expected = s.targetCasts * scale;
                const gap = s.baseCasts - expected;
                const percent = expected > 0 ? (gap / expected) * 100 : 100;
                return { name: s.skillName, mine: s.baseCasts, expected, gap, percent };
            });
        const shown = rows.filter((r) => Math.abs(r.gap) >= 1).sort((a, b) => a.percent - b.percent);
        if (summary) {
            summary.textContent = `차이가 1회 미만인 스킬 ${rows.length - shown.length}개는 뺐습니다.`;
        }
        if (shown.length === 0) {
            el.style.height = '60px';
            el.textContent = '시전 횟수 차이가 1회 이상인 스킬이 없습니다.';
            return null;
        }
        const limit = Math.min(200, Math.ceil(Math.max(...shown.map((r) => Math.abs(r.percent))) / 25) * 25);

        el.style.height = `${shown.length * 30 + 50}px`;
        const chart = echarts.init(el, THEME);
        chart.setOption({
            grid: { left: 20, right: 90, top: 10, bottom: 24, containLabel: true },
            tooltip: {
                trigger: 'item',
                formatter: (p) => {
                    const r = p.data.row;
                    const word = r.gap < 0 ? '덜' : '더';
                    return [`<b>${escapeHtml(r.name)}</b>`,
                        `내 기록 ${r.mine}회 · 기준 ${r.expected.toFixed(1)}회(보정)`,
                        `${Math.abs(r.gap).toFixed(1)}회 ${word} 씀 (${r.percent > 0 ? '+' : ''}${r.percent.toFixed(0)}%)`].join('<br>');
                },
            },
            xAxis: {
                type: 'value',
                min: -Math.min(100, limit),
                max: limit,
                axisLabel: { formatter: (v) => `${v > 0 ? '+' : ''}${v}%` },
                splitLine: { lineStyle: { type: 'dashed' } },
            },
            yAxis: { type: 'category', inverse: true, data: shown.map((r) => r.name), axisTick: { show: false } },
            series: [{
                type: 'bar',
                barMaxWidth: 16,
                data: shown.map((r) => ({
                    value: Math.max(-100, Math.min(limit, r.percent)),
                    row: r,
                    itemStyle: {
                        color: r.gap < 0 ? color('--coral') : color('--accent'),
                        borderRadius: r.gap < 0 ? [4, 0, 0, 4] : [0, 4, 4, 0],
                    },
                })),
                label: {
                    show: true,
                    position: 'right',
                    color: color('--text'),
                    formatter: (p) => `${p.data.row.mine} / ${p.data.row.expected.toFixed(1)}회`,
                },
                markLine: {
                    silent: true,
                    symbol: 'none',
                    lineStyle: { color: color('--axis-line'), type: 'solid' },
                    label: { show: false },
                    data: [{ xAxis: 0 }],
                },
            }],
        });
        return chart;
    }

    /**
     * 딜 비중(초 환산) 차이의 원인. 차이를 시전 수 효과와 1회 효율 효과로 나눠 쌓은 가로 막대로 그린다.
     * 한쪽에 시전 기록이 없는 스킬(패시브·연동, 한쪽만 쓴 스킬)은 나눌 수 없어 차이 전체를 회색 막대 하나로 둔다. 차이가 큰 스킬부터 보여준다.
     */
    function renderShareGapChart(el, skills) {
        const MAX_ROWS = 12;
        const rows = skills
            .map((s) => {
                const total = s.baseSeconds - s.targetSecondsScaled;
                const decomposable = s.castEffectSeconds !== null && s.efficiencyEffectSeconds !== null;
                return {
                    name: s.skillName,
                    total,
                    cast: decomposable ? s.castEffectSeconds : 0,
                    efficiency: decomposable ? s.efficiencyEffectSeconds : 0,
                    passive: decomposable ? 0 : total,
                };
            })
            .filter((r) => Math.abs(r.total) >= 0.3)
            .sort((a, b) => Math.abs(b.total) - Math.abs(a.total))
            .slice(0, MAX_ROWS);
        if (rows.length === 0) {
            el.style.height = '60px';
            el.textContent = '딜 비중 차이가 0.3초 이상인 스킬이 없습니다.';
            return null;
        }

        const series = (name, key, fill) => ({
            name,
            type: 'bar',
            stack: 'gap',
            barMaxWidth: 16,
            itemStyle: { color: fill },
            emphasis: { focus: 'series' },
            data: rows.map((r) => ({ value: round2(r[key]), row: r })),
        });

        el.style.height = `${rows.length * 32 + 80}px`;
        const chart = echarts.init(el, THEME);
        chart.setOption({
            grid: { left: 20, right: 60, top: 36, bottom: 24, containLabel: true },
            legend: { top: 0, data: ['시전 수 효과', '1회 효율 효과', '나눌 수 없음'] },
            tooltip: {
                trigger: 'axis',
                axisPointer: { type: 'shadow' },
                formatter: (params) => {
                    const r = params[0].data.row;
                    const sign = (v) => `${v > 0 ? '+' : ''}${v.toFixed(1)}초`;
                    const lines = [`<b>${escapeHtml(r.name)}</b>`, `차이 ${sign(r.total)}`];
                    if (r.passive !== 0) {
                        lines.push('한쪽에 시전 기록이 없어(패시브·연동 스킬이나 한쪽만 쓴 스킬) 원인을 나눌 수 없음');
                    } else {
                        lines.push(`시전 수 효과 ${sign(r.cast)}`, `1회 효율 효과 ${sign(r.efficiency)}`);
                    }
                    return lines.join('<br>');
                },
            },
            xAxis: {
                type: 'value',
                axisLabel: { formatter: (v) => `${v > 0 ? '+' : ''}${v}초` },
                splitLine: { lineStyle: { type: 'dashed' } },
            },
            yAxis: { type: 'category', inverse: true, data: rows.map((r) => r.name), axisTick: { show: false } },
            series: [
                series('시전 수 효과', 'cast', color('--accent')),
                series('1회 효율 효과', 'efficiency', color('--ascent')),
                series('나눌 수 없음', 'passive', color('--muted')),
            ],
        });
        return chart;
    }

    /**
     * 랭커 분포 속 내 위치. 스킬마다 랭커 25~75% 띠, 중앙값 세로선, 내 값 점을 그린다.
     * 스킬마다 눈금이 달라 랭커 중앙값을 100%로 맞춘다. 내 값이 하위 25%보다 낮은 스킬이 위에 온다.
     * @param rows   RankerStanding.Row 목록
     * @param metric 'rate'(분당 시전 수) 또는 'seconds'(초 환산)
     * @return { chart, setMetric(metric) }
     */
    function renderRankerDistribution(el, rows, metric) {
        const chart = echarts.init(el, THEME);
        const setMetric = (m) => {
            const option = rankerDistributionOption(rows, m);
            el.style.height = `${Math.max(option.rowCount, 1) * 30 + 60}px`;
            chart.resize();
            chart.setOption(option.option, true);
        };
        setMetric(metric);
        return { chart, setMetric };
    }

    function rankerDistributionOption(rows, metric) {
        const unit = metric === 'rate' ? '회/분' : '초';
        const digits = metric === 'rate' ? 2 : 1;
        const items = rows
            .map((r) => {
                const q = metric === 'rate' ? r.castsPerMinute : r.seconds;
                let mine = metric === 'rate' ? r.myCastsPerMinute : r.mySeconds;
                if (mine === null && r.myCastsPerMinute === 0) {
                    mine = 0; // 쓰지 않은 스킬
                }
                if (!q || q.p50 <= 0 || mine === null) {
                    return null;
                }
                const percent = (v) => (v / q.p50) * 100;
                return {
                    name: r.skillName,
                    p25: percent(q.p25),
                    p75: percent(q.p75),
                    mine: percent(mine),
                    low: mine < q.p25,
                    raw: { q, mine },
                };
            })
            .filter(Boolean)
            .sort((a, b) => (b.low - a.low) || (a.mine - b.mine));

        const max = Math.min(300, Math.ceil(Math.max(150, ...items.map((i) => Math.max(i.p75, i.mine))) / 50) * 50);
        const clamp = (v) => Math.min(max, v);
        const fixed = (v) => v.toFixed(digits);

        return {
            rowCount: items.length,
            option: {
                grid: { left: 20, right: 30, top: 10, bottom: 28, containLabel: true },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' },
                    formatter: (params) => {
                        const item = items[params[0].dataIndex];
                        const { q, mine } = item.raw;
                        return [`<b>${escapeHtml(item.name)}</b>`,
                            `내 값 ${fixed(mine)}${unit} (랭커 중앙값의 ${item.mine.toFixed(0)}%)`,
                            `랭커 25 / 50 / 75%: ${fixed(q.p25)} / ${fixed(q.p50)} / ${fixed(q.p75)}${unit}`].join('<br>');
                    },
                },
                xAxis: {
                    type: 'value',
                    min: 0,
                    max,
                    axisLabel: { formatter: '{value}%' },
                    splitLine: { lineStyle: { type: 'dashed' } },
                },
                yAxis: { type: 'category', inverse: true, data: items.map((i) => i.name), axisTick: { show: false } },
                series: [
                    { // 띠 시작점까지는 보이지 않게 쌓는다
                        type: 'bar', stack: 'band', silent: true, barWidth: 12,
                        itemStyle: { color: 'transparent' },
                        data: items.map((i) => clamp(i.p25)),
                    },
                    {
                        name: '랭커 25~75%', type: 'bar', stack: 'band', barWidth: 12,
                        itemStyle: { color: color('--accent-weak'), borderColor: 'rgba(79, 214, 255, 0.45)', borderWidth: 1, borderRadius: 6 },
                        data: items.map((i) => Math.max(0.5, clamp(i.p75) - clamp(i.p25))),
                    },
                    {
                        name: '랭커 중앙값', type: 'scatter', symbol: 'rect', symbolSize: [2, 18],
                        itemStyle: { color: color('--text') },
                        data: items.map(() => 100),
                        z: 3,
                    },
                    {
                        name: '내 값', type: 'scatter', symbolSize: 11,
                        data: items.map((i) => ({
                            value: clamp(i.mine),
                            itemStyle: {
                                color: i.low ? color('--coral') : color('--accent'),
                                borderColor: 'rgba(10, 15, 36, 0.9)',
                                borderWidth: 2,
                            },
                        })),
                        z: 4,
                    },
                ],
            },
        };
    }

    /**
     * 쿨 대비 실제 사용 간격. 스킬마다 "중앙 사용 간격 ÷ 실효 쿨"을 막대로 그린다. 1.0배 = 쿨마다 사용.
     * 판단하지 않는 스킬(쿨 15초 미만, 쿨 변동)은 회색으로 아래에 둔다. 두 번 이상 쓴 스킬만 간격이 있다.
     * @param rows    CooldownReport.Row 목록
     * @param summary 뺀 스킬 수를 적을 요소
     */
    function renderCooldownUsage(el, rows, summary) {
        const items = rows
            .filter((r) => r.effectiveCooldownMs && r.medianIntervalMs)
            .map((r) => ({
                name: r.skillName,
                ratio: r.medianIntervalMs / r.effectiveCooldownMs,
                cooldown: r.effectiveCooldownMs / 1000,
                interval: r.medianIntervalMs / 1000,
                casts: r.castCount,
                exclusion: r.usageExclusion,
            }))
            .sort((a, b) => (Boolean(a.exclusion) - Boolean(b.exclusion)) || (b.ratio - a.ratio));
        if (summary) {
            summary.textContent = `· 한 번만 썼거나 쿨을 모르는 스킬 ${rows.length - items.length}개는 뺐습니다.`;
        }
        if (items.length === 0) {
            el.style.height = '60px';
            el.textContent = '두 번 이상 쓴 쿨 스킬이 없습니다.';
            return null;
        }

        const max = Math.min(5, Math.max(2, Math.ceil(Math.max(...items.map((i) => i.ratio)) * 2) / 2));
        const tone = (i) => {
            if (i.exclusion) {
                return color('--muted');
            }
            if (i.ratio <= 1.15) {
                return color('--accent');
            }
            return i.ratio <= 1.5 ? color('--gold') : color('--coral');
        };

        el.style.height = `${items.length * 28 + 50}px`;
        const chart = echarts.init(el, THEME);
        chart.setOption({
            grid: { left: 20, right: 150, top: 10, bottom: 28, containLabel: true },
            tooltip: {
                trigger: 'item',
                formatter: (p) => {
                    const i = items[p.dataIndex];
                    const lines = [`<b>${escapeHtml(i.name)}</b>`,
                        `실효 쿨 ${i.cooldown.toFixed(1)}초 · 사용 간격(중앙값) ${i.interval.toFixed(1)}초`,
                        `쿨의 ${i.ratio.toFixed(2)}배 · ${i.casts}회 사용`];
                    if (i.exclusion) {
                        lines.push(`판단 제외: ${escapeHtml(i.exclusion)}`);
                    }
                    return lines.join('<br>');
                },
            },
            xAxis: {
                type: 'value',
                min: 0,
                max,
                axisLabel: { formatter: '{value}배' },
                splitLine: { lineStyle: { type: 'dashed' } },
            },
            yAxis: { type: 'category', inverse: true, data: items.map((i) => i.name), axisTick: { show: false } },
            series: [{
                type: 'bar',
                barMaxWidth: 14,
                data: items.map((i) => ({
                    value: Math.min(max, i.ratio),
                    itemStyle: { color: tone(i), borderRadius: [0, 4, 4, 0], opacity: i.exclusion ? 0.55 : 1 },
                })),
                label: {
                    show: true,
                    position: 'right',
                    color: color('--muted'),
                    formatter: (p) => {
                        const i = items[p.dataIndex];
                        return i.exclusion
                            ? `${i.ratio.toFixed(2)}배 · 제외(${i.exclusion})`
                            : `${i.ratio.toFixed(2)}배 · 쿨 ${i.cooldown.toFixed(1)}초`;
                    },
                },
                markLine: {
                    silent: true,
                    symbol: 'none',
                    lineStyle: { color: color('--positive'), type: 'dashed' },
                    label: { formatter: '쿨마다', color: color('--positive'), position: 'end' },
                    data: [{ xAxis: 1 }],
                },
            }],
        });
        return chart;
    }

    /**
     * 확대하면 보이는 범위에 맞게 X축 눈금 간격을 바꾼다.
     * 범위는 이벤트 값에서 읽고(getOption 은 시전 점 전체를 복사해 느리다), 간격이 실제로 바뀔 때만 다시 그린다.
     */
    function adjustTicksOnZoom(chart, maxSec) {
        let current = tickInterval(maxSec);
        chart.on('datazoom', (event) => {
            const range = event.batch ? event.batch[0] : event;
            if (range.start === undefined || range.end === undefined) {
                return;
            }
            const next = tickInterval(maxSec * (range.end - range.start) / 100);
            if (next !== current) {
                current = next;
                chart.setOption({ xAxis: { interval: next } }, { lazyUpdate: true });
            }
        });
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

    window.BattleCoachCharts = {
        renderShareChart, renderTimelineChart, renderComparisonTimeline, renderCastGapChart, renderShareGapChart,
        renderRankerDistribution, renderCooldownUsage, resizeOnWindowChange, readJson,
    };
})();
