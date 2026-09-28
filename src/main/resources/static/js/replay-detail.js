(function () {
    'use strict';

    const { renderShareChart, renderTimelineChart, resizeOnWindowChange, readJson } = window.BattleCoachCharts;

    const data = readJson('replay-data');
    const bursts = readJson('burst-data');

    resizeOnWindowChange([
        renderShareChart(document.getElementById('share-chart'), data.skillStats),
        renderTimelineChart(document.getElementById('timeline-chart'), data.casts, data.sequenceSegments, bursts),
    ]);
})();
