package javaiscoffee.polaroad.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javaiscoffee.polaroad.exception.NotFoundException;
import javaiscoffee.polaroad.response.ResponseMessages;
import javaiscoffee.polaroad.response.Status;
import javaiscoffee.polaroad.security.CustomUserDetails;
import javaiscoffee.polaroad.wrapper.RequestWrapperDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/review")
@Tag(name = "댓글 관련 API", description = "댓글에 관련된 API 모음")
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(summary = "댓글 작성 API", description = "댓글 작성할 때 사용하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 작성을 성공한 경우"),
            @ApiResponse(responseCode = "400", description = "댓글의 입력값이 잘못된 경우"),
            @ApiResponse(responseCode = "400", description = "포스트가 없거나 삭제되어서 댓글 삭제 실패한 경우"),
            @ApiResponse(responseCode = "403", description = "권한이 없는 경우")
    })
    @PostMapping("/write/{postId}")
    public ResponseEntity<?> writeReview(@RequestBody RequestWrapperDto<ReviewDto> requestDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        ReviewDto reviewDto = requestDto.getData();
        log.info("입력 받은 댓글 정보 = {}", reviewDto);
        ResponseReviewDto savedReview = reviewService.createReview(reviewDto, memberId);
        // 댓글 = null 에러 반환
        if (savedReview == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Status(ResponseMessages.INPUT_ERROR));
        } else {
            return ResponseEntity.ok(savedReview);
        }
    }

    @Operation(summary = "댓글 조회 API", description = "댓글 조회할 때 사용하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 조회 성공한 경우"),
            @ApiResponse(responseCode = "404", description = "댓글 조회 실패한 경우")
    })
    @GetMapping("/{reviewId}")
    public ResponseEntity<?> getReviewById(@PathVariable(name = "reviewId") Long reviewId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("댓글 조회 요청");
        Long memberId = userDetails.getMemberId();
        ResponseReviewDto findedReview = reviewService.getReviewById(reviewId, memberId);
        if (findedReview == null) throw new NotFoundException(ResponseMessages.READ_FAILED.getMessage());
        return ResponseEntity.ok(findedReview);
    }

    @Operation(summary = "댓글 수정 API", description = "댓글 수정할 때 사용하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공한 경우"),
            @ApiResponse(responseCode = "400", description = "댓글 수정 실패한 경우")
    })
    @PatchMapping("/edit/{reviewId}")
    public ResponseEntity<?> editReview(@RequestBody RequestWrapperDto<ReviewEditRequestDto> requestDto, @PathVariable(name = "reviewId") Long reviewId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("댓글 수정 요청");
        Long memberId = userDetails.getMemberId();
        ReviewEditRequestDto editReviewDto = requestDto.getData();
        log.info("수정된 댓글 정보 = {}", editReviewDto);
        ResponseReviewDto editedReview = reviewService.editReview(editReviewDto, reviewId, memberId);
        return ResponseEntity.ok(editedReview);
    }

    @Operation(summary = "댓글 삭제 API", description = "댓글 삭제할 때 사용하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공한 경우"),
            @ApiResponse(responseCode = "400", description = "댓글 or 포스트가 없거나 삭제되어서 댓글 삭제 실패한 경우"),
            @ApiResponse(responseCode = "403", description = "권한이 없는 경우")
    })
    @DeleteMapping("/delete/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable(name = "reviewId") Long reviewId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        reviewService.deleteReview(reviewId, memberId);
        return ResponseEntity.ok(null);
    }

    // 신고
//    @PostMapping("/report/{reviewId}")
//    public ResponseEntity<?> reportReview(@PathVariable Long reviewId, @AuthenticationPrincipal CustomUserDetails userDetails) {
//        Long memberId = userDetails.getMemberId();
//    }

    @Operation(summary = "포스트에 딸린 모든 댓글 조회 API", description = "포스트id로 해당 포스트의 모든 댓글 조회할 때 사용하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "postId로 댓글 조회에 성공한 경우"),
            @ApiResponse(responseCode = "404", description = "postId로 댓글 조회에 실패한 경우")
    })
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getReviewsByPostId(@PathVariable Long postId) {
        log.info("해당 포스트의 모든 댓글 조회 요청 = {}", postId);
        return ResponseEntity.ok(reviewService.getReviewByPostId(postId));
    }

    // 포스트에 딸린 모든 댓글 페이징
    @Operation(summary = "포스트 댓글 페이징 API", description = "포스트의 댓글들을 페이징 할 때 사용하는 API")
    @GetMapping("/post/{postId}/paging")
    public ResponseEntity<SliceResponseDto<?>> getPostReviewsPaged(
            @PathVariable(name = "postId") Long postId,
            @Parameter(name = "page", description = "## 댓글 페이지 번호", required = true, example = "1") @RequestParam int page) {
        log.info("포스트 댓글 페이징 요청");
        SliceResponseDto<?> reviewPage = reviewService.getReviewsPagedByPostId(postId, page);
        return ResponseEntity.ok(reviewPage);
    }


    @Operation(summary = "맴버가 작성한 모든 댓글 조회 API", description = "맴버id로 해당 맴버가 작성한 모든 댓글 조회할 때 사용하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "memberId로 댓글 조회에 성공한 경우"),
            @ApiResponse(responseCode = "404", description = "memberId로 댓글 조회에 실패한 경우")
    })
    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> getReviewsByMemberId(@PathVariable Long memberId) {
        log.info("맴버가 작성한 모든 댓글 조회 요청");
        return ResponseEntity.ok(reviewService.getReviewByMemberId(memberId));
    }

    @Operation(summary = "유저의 댓글 페이징 API", description = "유저가 작성한 모든 댓글들을 페이징 할 때 사용하는 API")
    @GetMapping("/member/{memberId}/paging")
    public ResponseEntity<SliceResponseDto<?>> getMyReviewsPaged(
            @PathVariable(name = "memberId") Long memberId,
            @Parameter(name = "page", description = "## 댓글 페이지 번호", required = true, example = "1") @RequestParam int page) {
        log.info("맴버가 작성한 모든 댓글들 페이징 요청");
        SliceResponseDto<?> reviewPage = reviewService.getReviewsPagedByMemberId(memberId, page);
        return ResponseEntity.ok(reviewPage);
    }

}
