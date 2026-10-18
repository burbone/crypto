package com.MySelfAlgoWithLoan.prod.web;

import com.MySelfAlgoWithLoan.prod.paper.OpenPositionValuationService;
import com.MySelfAlgoWithLoan.prod.web.model.OpenPositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OpenPositionController {

    private final OpenPositionValuationService valuationService;

    @GetMapping("/api/positions")
    public List<OpenPositionSnapshot> positions() {
        return valuationService.snapshot();
    }
}