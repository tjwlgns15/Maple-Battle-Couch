(function () {
    'use strict';

    const {
        renderShareChart, renderTimelineChart, renderPeerDistribution, renderCooldownUsage,
        attachIdleOverlay, resizeOnWindowChange, readJson,
    } = window.BattleCoachCharts;

    const data = readJson('replay-data');
    const bursts = readJson('burst-data');
    const analysis = readJson('analysis-data');

    const timeline = renderTimelineChart(document.getElementById('timeline-chart'), data.casts, data.sequenceSegments, bursts);
    const charts = [
        renderShareChart(document.getElementById('share-chart'), data.skillStats),
        timeline,
        renderCooldownUsage(document.getElementById('cooldown-chart'), analysis.cooldowns,
            document.getElementById('cooldown-summary')),
    ];

    // 쿨이 돈 뒤 쓰지 않은 구간. 기본은 진단에 나온 스킬만, 체크하면 대상 스킬 전체.
    const maxSec = Math.ceil(Math.max(0, ...data.casts.map((c) => c.elapseMs)) / 1000);
    const idle = attachIdleOverlay(timeline, analysis.idle, maxSec);
    const showAll = document.getElementById('idle-show-all');
    if (showAll) {
        showAll.addEventListener('change', () => idle.setShowAll(showAll.checked));
    }

    // 진단 카드의 "타임라인에서 보기": 타임라인으로 옮겨 그 스킬 줄을 강조하고 가장 긴 쉰 구간 주변을 확대한다.
    const timelineSection = document.getElementById('timeline');
    document.querySelectorAll('[data-focus-skill]').forEach((button) => {
        button.addEventListener('click', () => {
            idle.focusSkill(button.dataset.focusSkill);
            timelineSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });

    // 비교 대상 대비는 표본이 있을 때만 그린다. 탭으로 지표를 바꾼다.
    const standingEl = document.getElementById('standing-chart');
    if (standingEl && analysis.standing.length > 0) {
        const standing = renderPeerDistribution(standingEl, analysis.standing, 'rate');
        charts.push(standing.chart);
        document.querySelectorAll('.tabs .tab').forEach((tab) => {
            tab.addEventListener('click', () => {
                document.querySelectorAll('.tabs .tab').forEach((t) => {
                    t.classList.toggle('active', t === tab);
                    t.setAttribute('aria-selected', String(t === tab));
                });
                standing.setMetric(tab.dataset.metric);
            });
        });
    }

    resizeOnWindowChange(charts.filter(Boolean));
})();
