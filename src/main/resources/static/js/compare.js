(function () {
    'use strict';

    const { renderComparisonTimeline, resizeOnWindowChange, readJson } = window.BattleCoachCharts;

    const data = readJson('compare-data');
    const maxSec = Math.ceil(Math.max(data.base.totalPlayTimeMs, data.target.totalPlayTimeMs) / 1000);

    resizeOnWindowChange([
        renderComparisonTimeline(
            document.getElementById('compare-timeline'),
            { label: `내 기록 (${data.base.characterName})`, casts: data.base.casts, bursts: data.baseBursts },
            { label: `기준 기록 (${data.target.characterName})`, casts: data.target.casts, bursts: data.targetBursts },
            { maxSec }),
    ]);
})();
