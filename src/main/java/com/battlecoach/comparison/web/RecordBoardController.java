package com.battlecoach.comparison.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.battlecoach.comparison.application.RecordBoardService;

import lombok.RequiredArgsConstructor;

/** 저장된 연무장 기록을 기간·직업별로 보여주는 화면 */
@Controller
@RequiredArgsConstructor
public class RecordBoardController {

    private final RecordBoardService recordBoardService;

    /**
     * @param period 연무장 기간. 없으면 가장 최근 기간
     * @param job    직업. 없으면 기록이 가장 많은 직업
     * @param sort   dps(기본) 또는 efficiency(전투력 대비 DPS)
     */
    @GetMapping("/records")
    public String records(@RequestParam(required = false) Integer period,
                          @RequestParam(required = false) String job,
                          @RequestParam(required = false) String sort,
                          Model model) {
        model.addAttribute("board", recordBoardService.board(period, job, sort));
        return "records";
    }
}
