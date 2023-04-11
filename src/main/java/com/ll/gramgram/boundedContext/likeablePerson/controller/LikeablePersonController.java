package com.ll.gramgram.boundedContext.likeablePerson.controller;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.repository.InstaMemberRepository;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.service.LikeablePersonService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/likeablePerson")
@RequiredArgsConstructor
public class LikeablePersonController {
    private final Rq rq;
    private final LikeablePersonService likeablePersonService;
    private final InstaMemberRepository instaMemberRepository;

    @GetMapping("/add")
    public String showAdd() {
        return "usr/likeablePerson/add";
    }

    @AllArgsConstructor
    @Getter
    public static class AddForm {
        private final String username;
        private final int attractiveTypeCode;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/add")
    public String add(@Valid AddForm addForm) {
        //존재여부 확인
        RsData isPresentRsData = likeablePersonService.isPresent(rq.getMember(),addForm.getUsername(),addForm.getAttractiveTypeCode());
        //리스트 사이즈 확인
        RsData ifMaxSizeRsData = likeablePersonService.ifMaxSize(rq.getMember());
        //수정여부 확인
        RsData modifyRsData = likeablePersonService.modifyAttractiveType(rq.getMember(),addForm.getUsername(),addForm.getAttractiveTypeCode());
        //수정이 성공하면 리스트 출력
        if(modifyRsData.isSuccess()){
            return rq.redirectWithMsg("/likeablePerson/list",modifyRsData);
        }
        //이미 존재하는 지 검증.
        if(isPresentRsData.isFail()){
            return rq.historyBack(isPresentRsData);
        }
        //리스트 사이즈가 10이면 더이상 추가 안됨
        if(ifMaxSizeRsData.isFail()){
            return rq.historyBack(ifMaxSizeRsData);
        }
        //모든 검증이 끝나면 등록 시도
        RsData<LikeablePerson> createRsData = likeablePersonService.like(rq.getMember(), addForm.getUsername(), addForm.getAttractiveTypeCode());
        if (createRsData.isFail()) {
            return rq.historyBack(createRsData);
        }

        return rq.redirectWithMsg("/likeablePerson/list", createRsData);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public String showList(Model model) {
        InstaMember instaMember = rq.getMember().getInstaMember();

        // 인스타인증을 했는지 체크
        if (instaMember != null) {
            List<LikeablePerson> likeablePeople = instaMember.getFromLikeablePeople();
            model.addAttribute("likeablePeople", likeablePeople);
        }

        return "usr/likeablePerson/list";
    }
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping ("/{id}")
    public String delete (@PathVariable("id") Long id){
        LikeablePerson likeablePerson = this.likeablePersonService.findById(id).orElse(null);
        if(likeablePerson==null) return rq.historyBack("이미 취소된 호감입니다.");

        RsData ableToDeleteRs = likeablePersonService.ableToDelete(rq.getMember(),likeablePerson);

        if(ableToDeleteRs.isFail()) return rq.historyBack(ableToDeleteRs);

        RsData deleteRs= likeablePersonService.delete(likeablePerson);
        if (deleteRs.isFail()) return rq.historyBack(deleteRs);

        return rq.redirectWithMsg("/likeablePerson/list", deleteRs);
    }

}
