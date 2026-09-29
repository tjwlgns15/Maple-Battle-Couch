(function () {
    'use strict';

    const {
        renderShareChart, renderTimelineChart, renderRankerDistribution, renderCooldownUsage,
        resizeOnWindowChange, readJson,
    } = window.BattleCoachCharts;

    const data = readJson('replay-data');
    const bursts = readJson('burst-data');
    const analysis = readJson('analysis-data');

    const charts = [
        renderShareChart(document.getElementById('share-chart'), data.skillStats),
        renderTimelineChart(document.getElementById('timeline-chart'), data.casts, data.sequenceSegments, bursts),
        renderCooldownUsage(document.getElementById('cooldown-chart'), analysis.cooldowns,
            document.getElementById('cooldown-summary')),
    ];

    // 랭커 대비는 표본이 있을 때만 그린다. 탭으로 지표를 바꾼다.
    const rankerEl = document.getElementById('ranker-chart');
    if (rankerEl && analysis.standing.length > 0) {
        const ranker = renderRankerDistribution(rankerEl, analysis.standing, 'rate');
        charts.push(ranker.chart);
        document.querySelectorAll('.tabs .tab').forEach((tab) => {
            tab.addEventListener('click', () => {
                document.querySelectorAll('.tabs .tab').forEach((t) => {
                    t.classList.toggle('active', t === tab);
                    t.setAttribute('aria-selected', String(t === tab));
                });
                ranker.setMetric(tab.dataset.metric);
            });
        });
    }

    resizeOnWindowChange(charts.filter(Boolean));
})();
