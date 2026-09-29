(function () {
    'use strict';

    const {
        renderComparisonTimeline, renderCastGapChart, renderShareGapChart, resizeOnWindowChange, readJson,
    } = window.BattleCoachCharts;

    const data = readJson('compare-data');
    const maxSec = Math.ceil(Math.max(data.base.totalPlayTimeMs, data.target.totalPlayTimeMs) / 1000);

    resizeOnWindowChange([
        renderCastGapChart(document.getElementById('cast-gap-chart'), data.skills, data.playTimeScale,
            document.getElementById('cast-gap-summary')),
        renderShareGapChart(document.getElementById('share-gap-chart'), data.skills),
        renderComparisonTimeline(
            document.getElementById('compare-timeline'),
            { label: `내 기록 (${data.base.characterName})`, casts: data.base.casts, bursts: data.baseBursts },
            { label: `기준 기록 (${data.target.characterName})`, casts: data.target.casts, bursts: data.targetBursts },
            { maxSec }),
    ].filter(Boolean)); // 보여줄 데이터가 없는 차트는 null
})();
