package javaiscoffee.polaroad.review;

import ext.javaiscoffee.polaroad.review.reviewPhoto.QReviewPhoto;
import javaiscoffee.polaroad.member.JpaMemberRepository;
import javaiscoffee.polaroad.member.Member;
import javaiscoffee.polaroad.post.Post;
import javaiscoffee.polaroad.post.PostRepository;
import javaiscoffee.polaroad.post.PostStatus;
import javaiscoffee.polaroad.review.reviewPhoto.JpaReviewPhotoRepository;
import javaiscoffee.polaroad.review.reviewPhoto.ReviewPhoto;
import javaiscoffee.polaroad.review.reviewPhoto.ReviewPhotoService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class ReviewService {
    private final JpaReviewRepository reviewRepository;
    private final JpaMemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ReviewPhotoService reviewPhotoService;
    private final JpaReviewPhotoRepository reviewPhotoRepository;

    public ResponseReviewDto createReview(ReviewDto reviewDto, Long memberId) {
        if (!memberId.equals(reviewDto.getMemberId())) {
            return null;
        }
        Review newReview = new Review();
        BeanUtils.copyProperties(reviewDto, newReview);
        Member creatorMember = memberRepository.findByMemberId(memberId).get();
        Post post = postRepository.findById(reviewDto.getPostId()).get();

        if (post.getStatus() == PostStatus.DELETED) {
            return null;
        }

        // 댓글 저장
        newReview.setMemberId(creatorMember);
        newReview.setPostId(post);
        Review savedReview = reviewRepository.save(newReview);

        List<String> savedReviewPhotos = new ArrayList<>();
        // 사진 저장
        reviewDto.getReviewPhotoList().forEach(reviewPhotoUrl ->{
            ReviewPhoto newReviewPhoto = reviewPhotoService.saveReviewPhoto(reviewPhotoUrl, savedReview);   // reviewDto에 리스트로 있는 사진 url들을 하나하나 saveReviewPhoto에 보냄
            savedReviewPhotos.add(newReviewPhoto.getImage());
        } );


        return toResponseReviewDto(savedReview, savedReviewPhotos);
    }

    public ResponseReviewDto getReviewById(Long reviewId, Long memberId) {
        log.info("댓글 조회 시작 = {}", reviewId);
        Review findedReview = reviewRepository.findByReviewId(reviewId);
        log.info("댓글 id 조회 후 = {}", findedReview);
        Post post = postRepository.findById(findedReview.getPostId().getPostId()).get();
        // 삭제된 댓글이거나 삭제된 포스트인 경우 null 반환
        if (findedReview == null || findedReview.getStatus() == ReviewStatus.DELETED || post == null || post.getStatus() == PostStatus.DELETED) {
            return null;
        }

        // 댓글 id로 해당 댓글의 모든 사진 조회
        log.info("해당 댓글의 모든 사진 조회 시작");
//        List<ReviewPhoto> reviewPhotosByReviewId = reviewPhotoRepository.findReviewPhotosByReviewId(findedReview);
        List<ReviewPhoto> reviewPhotosByReviewId = reviewRepository.findByReviewId(findedReview.getReviewId()).getReviewPhoto();
        log.info("findedReview.getReviewId() = {} ", findedReview.getReviewId());
        List<String> reviewPhotoUrls = new ArrayList<>();
        for (ReviewPhoto reviewPhotoUrl : reviewPhotosByReviewId) {
            // get 사진 url
            String image = reviewPhotoUrl.getImage();
            reviewPhotoUrls.add(image);
        }
        log.info("해당 댓글의 모든 사진 = {}", reviewPhotoUrls);
        // 찾은 댓글 + 해당 댓글에 속한 모든 사진 반환
        return toResponseReviewDto(findedReview, reviewPhotoUrls);
    }

    public ResponseReviewDto editReview(ReviewEditRequestDto editReviewDto, Long reviewId, Long memberId) {
        log.info("editReview 메서드에 들어온 reviewId = {}", reviewId);
        Review originalReview = reviewRepository.findByReviewId(reviewId);
        log.info("댓글 id 조회 후 = {}", originalReview);
        Post post = postRepository.findById(originalReview.getPostId().getPostId()).get();

        // 원본 댓글 & 포스트가 null 이거나, 삭제된 경우 null 반환
        if (originalReview == null || originalReview.getStatus() == ReviewStatus.DELETED || post == null || post.getStatus() == PostStatus.DELETED) {
            return null;
        }

        Member member = memberRepository.findByMemberId(memberId).get();
        // 댓글을 작성한 memberId와 수정을 요청한 맴버의 memberId가 다른 경우 null 반환
        if (!originalReview.getMemberId().getMemberId().equals(member.getMemberId())) {
            return null;
        }

        // 댓글 수정
        originalReview.setContent(editReviewDto.getContent());
        originalReview.setUpdatedTime(LocalDateTime.now());
        Review updatedReview = reviewRepository.update(originalReview);
        log.info("댓글 업데이트 = {}", updatedReview);

        // 사진 수정
        log.info("댓글 사진 수정 시작");
        reviewPhotoService.editReviewPhoto(editReviewDto.getReviewPhotoList(), updatedReview);

        return toResponseReviewDto(updatedReview, editReviewDto.getReviewPhotoList());
    }

    public Boolean deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findByReviewId(reviewId);
        // 댓글이 존재하지 않거나 삭제된 댓글인 경우 false 반환
        if (review == null || review.getStatus() == ReviewStatus.DELETED) {
            return false;
        }
        Post post = postRepository.findById(review.getPostId().getPostId()).get();
        // 포스트가 존재하지 않거나 삭제된 포스트인 경우 false 반환
        if (post == null || post.getStatus() == PostStatus.DELETED) {
            return false;
        }
        Member member = memberRepository.findByMemberId(memberId).get();
        // member가 댓글 작성자가 아닌 경우 false 반환
        if (!review.getMemberId().getMemberId().equals(member.getMemberId())) {
            return false;
        }

        // 해당 댓글에 속한 모든 사진
        List<ReviewPhoto> reviewPhotos = reviewRepository.findByReviewId(review.getReviewId()).getReviewPhoto();
        // 해당 댓글에 속한 모든 사진 삭제
        for (ReviewPhoto reviewPhoto : reviewPhotos) {
            Long reviewPhotoId = reviewPhoto.getReviewPhotoId();
            reviewPhotoRepository.delete(reviewPhotoId);
        }

        // 댓글 삭제
        reviewRepository.delete(reviewId);
        return true;
    }

    // 포스트에 딸린 ACTIVE 상태의 모든 댓글
    public List<ResponseReviewDto> getReviewByPostId(Long postId) {
        Post getPost = postRepository.findById(postId).get();
        // 가져온 post에 속한 ACTIVE 상태인 모든 댓글을 가져옴
        List<Review> reviewList = reviewRepository.findReviewByPostId(getPost, ReviewStatus.ACTIVE);
        // 포스트에 딸린 각 댓글에 해당하는 모든 사진들
        List<ReviewPhoto> reviewPhotos = new ArrayList<>();
        for (Review review : reviewList) {
            List<ReviewPhoto> reviewPhotosByReviewId = reviewRepository.findByReviewId(review.getReviewId()).getReviewPhoto();
            reviewPhotos.addAll(reviewPhotosByReviewId);
        }
        return toResponseReviewDtoList(reviewList, reviewPhotos);
    }

    // 맴버가 작성한 모든 댓글
    public List<ResponseReviewDto> getReviewByMemberId(Long memberId) {
        Member getMember = memberRepository.findByMemberId(memberId).get();
        List<Review> reviewList = reviewRepository.findReviewByMemberId(getMember, ReviewStatus.ACTIVE);
        // 맴버가 작성한 각 댓글에 해당하는 모든 사진들
        List<ReviewPhoto> reviewPhotos = new ArrayList<>();
        for (Review review : reviewList) {
            List<ReviewPhoto> reviewPhotosByReviewId = reviewRepository.findByReviewId(review.getReviewId()).getReviewPhoto();
            reviewPhotos.addAll(reviewPhotosByReviewId);
        }
        return toResponseReviewDtoList(reviewList, reviewPhotos);
    }


    // Review 객체를 ResponseReviewDto 객체로 매핑하는 메서드
    public static ResponseReviewDto toResponseReviewDto(Review review, List<String> reviewPhotos) {
        if (review == null) {
            return  null;   // 주어진 review가 null인 경우, null 반환
        }
        ResponseReviewDto responseReviewDto = new ResponseReviewDto();

        responseReviewDto.setPostId(review.getPostId() != null ? review.getPostId().getPostId() : null);
        responseReviewDto.setMemberId(review.getMemberId() != null ? review.getMemberId().getMemberId() : null);
        responseReviewDto.setNickname(review.getMemberId() != null ? review.getMemberId().getNickname() : null);
        responseReviewDto.setReviewId(review.getReviewId());
        responseReviewDto.setContent(review.getContent());
        responseReviewDto.setUpdatedTime(review.getUpdatedTime());
        responseReviewDto.setReviewPhotoList(reviewPhotos);
        //프로필 이미지

        return responseReviewDto;
    }

    // Review 객체 리스트를 ResponseReviewDto 객체 리스트로 매핑하는 메서드
    public static List<ResponseReviewDto> toResponseReviewDtoList(List<Review> reviews, List<ReviewPhoto> reviewPhotos) {
        if (reviews == null) {
            return null;
        }

        List<ResponseReviewDto> responseReviewDtoList = new ArrayList<>();
        for (Review review : reviews) {

            List<String> reviewPhotoList = new ArrayList<>();
            for (ReviewPhoto reviewPhoto : reviewPhotos) {
                if (reviewPhoto.getReviewId().equals(review.getReviewId())) {
                    String image = reviewPhoto.getImage();
                    reviewPhotoList.add(image);
                }
            }

            ResponseReviewDto responseReviewDto = toResponseReviewDto(review, reviewPhotoList);
            responseReviewDtoList.add(responseReviewDto);
        }

        return responseReviewDtoList;
    }
}
