package com.MySelfAlgoWithLoan.prod.web;

import com.MySelfAlgoWithLoan.prod.web.model.CandidateSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CandidateController {

    private final CandidateStore candidateStore;

    @GetMapping("/api/candidates")
    public List<CandidateSnapshot> candidates() {
        return candidateStore.all();
    }
}