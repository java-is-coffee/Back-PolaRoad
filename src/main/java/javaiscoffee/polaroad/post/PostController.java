package javaiscoffee.polaroad.post;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javaiscoffee.polaroad.post.card.CardListResponseDto;
import javaiscoffee.polaroad.post.card.CardSaveDto;
import javaiscoffee.polaroad.post.card.CardService;
import javaiscoffee.polaroad.response.ResponseMessages;
import javaiscoffee.polaroad.security.CustomUserDetails;
import javaiscoffee.polaroad.wrapper.RequestWrapperDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Thread.sleep;

@Slf4j
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
@Tag(name = "포스트 관련 API",description = "포스트, 카드, 해쉬태그 관련 API들 모음 - 담당자 박상현")
public class PostController {
    private final PostService postService;
    private final CardService cardService;
    private final KoreanTextGenerator koreanTextGenerator;
    private static final Random random = new Random();

    @Operation(summary = "포스트 생성", description = "포스트 생성하는 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 생성에 성공했을 경우"),
            @ApiResponse(responseCode = "400", description = "입력값이 잘못된 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 존재하지 않는 경우")
    })
    @PostMapping("/write")
    public ResponseEntity<String> savePost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @Validated @RequestBody RequestWrapperDto<PostSaveDto> requestWrapperDto) {
        Long memberId = userDetails.getMemberId();
        PostSaveDto postSaveDto = requestWrapperDto.getData();
        log.info("저장하려는 포스트 Dto = {}",postSaveDto);
        postService.savePost(postSaveDto, memberId);
        return ResponseEntity.ok(ResponseMessages.SUCCESS.getMessage());
    }
    @Operation(summary = "포스트 생성 테스트", description = "생성 테스트 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 생성에 성공했을 경우"),
            @ApiResponse(responseCode = "400", description = "입력값이 잘못된 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 존재하지 않는 경우")
    })
    @PostMapping("/write2")
    public ResponseEntity<String> savePostTest(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @RequestBody RequestWrapperDto<PostSaveDto> requestWrapperDto) {
        Long memberId = userDetails.getMemberId();
        PostSaveDto postSaveDto = requestWrapperDto.getData();
        log.info("저장하려는 포스트 Dto = {}",postSaveDto);
        postService.savePostTest(postSaveDto, memberId);
        return ResponseEntity.ok("성공");
    }

    @Operation(summary = "포스트 수정", description = "포스트 수정하는 API")
    @Parameter(name = "postId", description = "Path Variable로 수정할 포스트 ID를 넘겨주면 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 수정에 성공했을 경우"),
            @ApiResponse(responseCode = "403", description = "생성자 본인이 아닌 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 존재하지 않는 경우")
    })
    @PatchMapping("/edit/{postId}")
    public ResponseEntity<Post> editPost(@AuthenticationPrincipal CustomUserDetails userDetails,
                                         @Validated @RequestBody RequestWrapperDto<PostSaveDto> requestWrapperDto,
                                         @PathVariable(name = "postId") Long postId) {
        Long memberId = userDetails.getMemberId();
        PostSaveDto postSaveDto = requestWrapperDto.getData();
        log.info("수정하려는 포스트 Dto = {}",postSaveDto);
        return postService.editPost(postSaveDto, memberId, postId);
    }

    @Operation(summary = "포스트 삭제", description = "포스트 삭제하는 API")
    @Parameter(name = "postId", description = "Path Variable로 삭제할 포스트 ID를 넘겨주면 됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 삭제에 성공했을 경우"),
            @ApiResponse(responseCode = "403", description = "생성자 본인이 아닌 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 존재하지 않는 경우")
    })
    @DeleteMapping("/delete/{postId}")
    public ResponseEntity<String> deletePost(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable(name = "postId") Long postId) {
        Long memberId = userDetails.getMemberId();
        return postService.deletePost(postId, memberId);
    }

    @Operation(summary = "포스트 검색", description = "키워드, 해쉬태그 검색하는 API")
    @Parameter(name = "page", description = "1부터 시작합니다. 몇 번째 페이지를 출력할 것인지",required = true, example = "1")
    @Parameter(name = "pageSize", description = "한 페이지에 몇 개의 결과를 표시할 것인지 정하는 수치", required = true, example = "8")
    @Parameter(name = "searchType", description = "검색 방식을 나타냅니다. \n - KEYWORD = 키워드검색 \n - HASHTAG = 해쉬태그검색", example = "KEYWORD")
    @Parameter(name = "keyword", description = "검색할 키워드나 해쉬태그 \n null값이면 나머지 조건으로 검색", example = "자바")
    @Parameter(name = "sortBy", description = "검색 결과를 정렬할 방식 \n - GOOD = 하트순 \n - RECENT = 최신순",  required = true, example = "RECENT")
    @Parameter(name = "concept", description = "게시글 카테고리 \n null값이면 나머지 조건으로 검색 \n- FOOD 식도락\n" +
            "- NATURE 자연\n" +
            "- CITY 도시관광\n" +
            "- PHOTO 사진&명소\n" +
            "- HOT 인기게시글\n" +
            "- WALK 도보여행\n" +
            "- CAR 자동차&대중교통 여행\n" +
            "- TRAIN 기차여행", example = "FOOD")
    @Parameter(name = "region", description = "게시글 지역 \n null값이면 나머지 조건으로 검색 \n" +
            "- SEOUL 서울, INCHEON 인천, BUSAN 부산, DAEGU 대구, \n" +
            "- GWANGJU 광주, DAEJEON 대전, ULSAN 울산, GYEONGGIDO 경기도, GANGWONDO 강원도,\n " +
            "- CHUNGCHEONGNAMDO 충남, CHUNGCHEONGBUKDO 충북,\n " +
            "- JEOLLANAMDO 전남, JEOLLABUKDO 전북,\n " +
            "- GYEONGSANGNAMDO 경남, GYEONGSANGBUKDO 경북,\n " +
            "- JEJUDO 제주도", example = "SEOUL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스트 조회에 성공한 경우")
    })
    @GetMapping("/list")
    public ResponseEntity<PostListResponseDto> getPostList(@RequestParam(name = "page") int page,
                                                         @RequestParam(name = "pageSize") int pageSize,
                                                         @RequestParam(name = "searchType") PostSearchType searchType,
                                                         @RequestParam(name = "keyword",required = false) String keyword,
                                                         @RequestParam(name = "sortBy") PostListSort sortBy,
                                                         @RequestParam(name = "concept", required = false) PostConcept concept,
                                                         @RequestParam(name = "region", required = false) PostRegion region) {
        return postService.getPostListByIndexMatch(page,pageSize,searchType,keyword,sortBy,concept,region,PostStatus.ACTIVE);
    }

    @Operation(summary = "포스트 검색", description = "키워드, 해쉬태그 검색하는 API")
    @Parameter(name = "page", description = "1부터 시작합니다. 몇 번째 페이지를 출력할 것인지",required = true, example = "1")
    @Parameter(name = "pageSize", description = "한 페이지에 몇 개의 결과를 표시할 것인지 정하는 수치", required = true, example = "8")
    @Parameter(name = "searchType", description = "검색 방식을 나타냅니다. \n - KEYWORD = 키워드검색 \n - HASHTAG = 해쉬태그검색", example = "KEYWORD")
    @Parameter(name = "keyword", description = "검색할 키워드나 해쉬태그 \n null값이면 나머지 조건으로 검색", example = "자바")
    @Parameter(name = "sortBy", description = "검색 결과를 정렬할 방식 \n - GOOD = 하트순 \n - RECENT = 최신순",  required = true, example = "RECENT")
    @Parameter(name = "concept", description = "게시글 카테고리 \n null값이면 나머지 조건으로 검색 \n- FOOD 식도락\n" +
            "- NATURE 자연\n" +
            "- CITY 도시관광\n" +
            "- PHOTO 사진&명소\n" +
            "- HOT 인기게시글\n" +
            "- WALK 도보여행\n" +
            "- CAR 자동차&대중교통 여행\n" +
            "- TRAIN 기차여행", example = "FOOD")
    @Parameter(name = "region", description = "게시글 지역 \n null값이면 나머지 조건으로 검색 \n" +
            "- SEOUL 서울, INCHEON 인천, BUSAN 부산, DAEGU 대구, \n" +
            "- GWANGJU 광주, DAEJEON 대전, ULSAN 울산, GYEONGGIDO 경기도, GANGWONDO 강원도,\n " +
            "- CHUNGCHEONGNAMDO 충남, CHUNGCHEONGBUKDO 충북,\n " +
            "- JEOLLANAMDO 전남, JEOLLABUKDO 전북,\n " +
            "- GYEONGSANGNAMDO 경남, GYEONGSANGBUKDO 경북,\n " +
            "- JEJUDO 제주도", example = "SEOUL")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스트 조회에 성공한 경우")
    })
    @GetMapping("/list-like")
    public ResponseEntity<PostListResponseDto> getPostListByIndexMatch(@RequestParam(name = "page") int page,
                                                           @RequestParam(name = "pageSize") int pageSize,
                                                           @RequestParam(name = "searchType") PostSearchType searchType,
                                                           @RequestParam(name = "keyword",required = false) String keyword,
                                                           @RequestParam(name = "sortBy") PostListSort sortBy,
                                                           @RequestParam(name = "concept", required = false) PostConcept concept,
                                                           @RequestParam(name = "region", required = false) PostRegion region) {
        return postService.getPostList(page,pageSize,searchType,keyword,sortBy,concept,region,PostStatus.ACTIVE);
    }

    @Operation(summary = "포스트 내용 조회", description = "포스트 내용 조회 페이지에서 사용하는 API")
    @Parameter(name = "postId", description = "내용 조회할 포스트 ID",required = true, example = "1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 조회에 성공했을 경우"),
            @ApiResponse(responseCode = "404", description = "포스트가 삭제되었거나 존재하지 않는 경우")
    })
    @GetMapping("/content/{postId}")
    public ResponseEntity<PostInfoDto> getPostInfo(@PathVariable(name = "postId") Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return postService.getPostInfoById(postId, userDetails.getMemberId());
    }

    @Operation(summary = "포스트 추천", description = "사용자가 포스트 추천하는 API \n ## 추천이 안되어 있으면 추천을 생성하고, 추천이 되어 있으면 추천을 삭제하는 토글 방식")
    @Parameter(name = "postId", description = "추천할 포스트 ID",required = true, example = "1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 추천에 성공했을 경우"),
            @ApiResponse(responseCode = "404", description = "멤버나 포스트가 삭제되었거나 존재하지 않는 경우")
    })
    @PatchMapping("/good/{postId}")
    public ResponseEntity<?> postGoodToggle(@PathVariable(name = "postId") Long postId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        postService.postGoodToggle(memberId, postId);
        return ResponseEntity.ok(null);
    }

    @Operation(summary = "마이페이지 카드 조회", description = "마이페이지에서 사용자가 업로드한 삭제되지 않은 카드들만 조회")
    @Parameter(name = "page", description = "현재 페이지 숫자 1부터 시작 1페이지이면 1을 주입", required = true, example = "1")
    @Parameter(name = "pageSize",description = "페이지 크기, 한 페이지에 표시할 카드 개수", required = true, example = "8")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 추천에 성공했을 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 삭제되었거나 존재하지 않는 경우")
    })
    @GetMapping("/mycard")
    public ResponseEntity<CardListResponseDto> getMyCardList(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             @RequestParam(name = "page") int page,
                                                             @RequestParam(name = "pageSize") int pageSize) {
        Long memberId = userDetails.getMemberId();
        return ResponseEntity.ok(cardService.getCardListByMember(memberId, page, pageSize));
    }

    @Operation(summary = "멤버 id로 포스트 조회", description = "사용자가 업로드한 삭제되지 않은 포스트 리스트 조회")
    @Parameter(name = "page", description = "현재 페이지 숫자 1부터 시작 1페이지이면 1을 주입", required = true, example = "1")
    @Parameter(name = "pageSize",description = "페이지 크기, 한 페이지에 표시할 카드 개수", required = true, example = "8")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포스트 추천에 성공했을 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 삭제되었거나 존재하지 않는 경우")
    })
    @GetMapping("/mypost")
    public ResponseEntity<PostListResponseDto> getMyPostList(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @RequestParam(name = "page") int page,
                                                           @RequestParam(name = "pageSize") int pageSize) {
        Long memberId = userDetails.getMemberId();
        return ResponseEntity.ok(postService.getMyPostList(memberId, page, pageSize, PostStatus.ACTIVE));
    }

    @Operation(summary = "팔로잉하고 있는 멤버들의 포스트 조회", description = "현재 멤버가 팔로잉하고 있는 멤버들이 올린 포스트 목록 조회")
    @Parameter(name = "page", description = "현재 페이지 숫자 1부터 시작 1페이지이면 1을 주입", required = true, example = "1")
    @Parameter(name = "pageSize",description = "페이지 크기, 한 페이지에 표시할 카드 개수", required = true, example = "8")
    @Parameter(name = "concept", description = "게시글 카테고리", example = "FOOD")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회에 성공했을 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 삭제되었거나 존재하지 않는 경우")
    })
    @GetMapping("/following")
    public ResponseEntity<PostListResponseDto> getFollowingMemberPosts(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                       @RequestParam(name = "page") int page,
                                                                       @RequestParam(name = "pageSize") int pageSize,
                                                                       @RequestParam(name = "concept", required = false) PostConcept concept) {
        Long memberId = userDetails.getMemberId();
        return postService.getFollowingMemberPosts(memberId,concept,page,pageSize,PostStatus.ACTIVE);
    }

    @Operation(summary = "시간별 조회수 랭킹으로 포스트 목록 조회", description = "1일,7일,30일 구간별 총 조회수 랭킹으로 포스트 목록을 조회하는 API")
    @Parameter(name = "page", description = "현재 페이지 숫자 1부터 시작 1페이지이면 1을 주입", required = true, example = "1")
    @Parameter(name = "pageSize",description = "페이지 크기, 한 페이지에 표시할 카드 개수", required = true, example = "8")
    @Parameter(name = "range", description = "랭킹 기간 범위 ex) 일간,주간,월간", required = true, example = "DAILY")
    @GetMapping("/view-rangking")
    public ResponseEntity<PostListResponseDto> getPostViewRangkingList(@RequestParam(name = "page") int page,
                                                                       @RequestParam(name = "pageSize") int pageSize,
                                                                       @RequestParam(name = "range") PostRankingRange range) {
        return postService.getPostRankingList(page, pageSize, range);
    }

    @Operation(summary = "포스트 더미데이터 1만개 생성")
    @GetMapping("/dummy-create")
    public ResponseEntity<Boolean> createDummyPost(@AuthenticationPrincipal CustomUserDetails userDetails) throws InterruptedException {
        Long memberId = userDetails.getMemberId();
        PostRegion[] regions = PostRegion.values();
        PostConcept[] concepts = PostConcept.values();
        for(int i = 0; i < 20000; i++) {
            PostSaveDto postSaveDto = new PostSaveDto();
            postSaveDto.setTitle(koreanTextGenerator.generateKoreanText(30));
            postSaveDto.setRoutePoint("");
            postSaveDto.setThumbnailIndex(0);
            postSaveDto.setConcept(concepts[random.nextInt(concepts.length)]);
            postSaveDto.setRegion(regions[random.nextInt(regions.length)]);
            postSaveDto.setCards(generateCardDummyList(random.nextInt(1,10)));
            postSaveDto.setHashtags(koreanTextGenerator.generateHashtags(random.nextInt(50),random.nextInt(10)));
            postService.savePost(postSaveDto, memberId);
            sleep(20);
        }
        return ResponseEntity.ok(true);
    }
    private List<CardSaveDto> generateCardDummyList(int num) {
        ArrayList<CardSaveDto> cardSaveDtos = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            CardSaveDto cardSaveDto = new CardSaveDto();
            cardSaveDto.setCardId(null); // 생성 시 null, 수정 시 실제 ID
            cardSaveDto.setCardIndex(i); // 카드 순서 설정
            cardSaveDto.setLocation("Some location"); // 임시 위치 데이터
            cardSaveDto.setLatitude(random.nextDouble() * 300000); // 0 ~ 300,000 사이의 랜덤 위도
            cardSaveDto.setLongitude(random.nextDouble() * 300000); // 0 ~ 300,000 사이의 랜덤 경도
            cardSaveDto.setImage(generateImage());
            cardSaveDto.setContent(koreanTextGenerator.generateKoreanText(800)); // 랜덤 한글 텍스트 생성
            cardSaveDtos.add(cardSaveDto);
        }
        return cardSaveDtos;
    }
    private static String[] images = {"blob:http://polaroad.s3-website.ap-northeast-2.amazonaws.com/8b35cc35-f7a3-4f1c-98ce-2d84a9419610",
    "blob:http://polaroad.s3-website.ap-northeast-2.amazonaws.com/ea999741-1d4a-4a06-9060-5227f2621400",
    "blob:http://polaroad.s3-website.ap-northeast-2.amazonaws.com/9e86f811-40c8-4655-9abc-34a50075c8e8",
    "blob:http://polaroad.s3-website.ap-northeast-2.amazonaws.com/e3996159-437b-4b91-8bcb-6f1cf64a3318",
    "http://polaroad.s3-website.ap-northeast-2.amazonaws.com/logo512.png"};
    private String generateImage() {
        return images[random.nextInt(images.length)];
    }
}
