package javaiscoffee.polaroad.post.card;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javaiscoffee.polaroad.post.PostConcept;
import javaiscoffee.polaroad.post.PostSearchType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/card")
@RequiredArgsConstructor
@Tag(name = "카드 API",description = "카드 관련 API들 모음 - 담당자 박상현")
public class CardController {
    private final CardService cardService;

    @Operation(summary = "지도에서 보여줄 카드 목록 조회", description = "지도로 카드 조회할 때 보여줄 범위 좌표를 통해 카드 리스트 조회하는 API")
    @Parameter(name = "searchType", description = "검색 방식을 나타냅니다. \n - KEYWORD = 키워드검색 \n - HASHTAG = 해쉬태그검색", example = "KEYWORD")
    @Parameter(name = "keyword", description = "검색할 키워드나 해쉬태그 \n null값이면 나머지 조건으로 검색", example = "자바")
    @Parameter(name = "concept", description = "포스트 카테고리", example = "FOOD")
    @Parameter(name = "swLatitude", description = "남서쪽 위도 좌표", required = true, example = "33.44908352355448")
    @Parameter(name = "swLongitude", description = "남서쪽 경도 좌표", required = true, example = "126.55941920359227")
    @Parameter(name = "neLatitude", description = "북동쪽 위도 좌표", required = true, example = "33.45231701892223")
    @Parameter(name = "neLongitude", description = "북동쪽 경도 좌표", required = true, example = "126.58191440427356")
    @Parameter(name = "pageSize", description = "최대로 조회할 카드 개수", required = true, example = "40")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카드 조회에 성공했을 경우")
    })
    @GetMapping("/map/list")
    public ResponseEntity<List<MapCardListDto>> getMapCardList(@RequestParam(name = "searchType", required = false) PostSearchType searchType,
                                                               @RequestParam(name = "keyword",required = false) String keyword,
                                                               @RequestParam(name = "concept", required = false) PostConcept concept,
                                                               @RequestParam(name = "swLatitude") double swLatitude,
                                                               @RequestParam(name = "swLongitude") double swLongitude,
                                                               @RequestParam(name = "neLatitude") double neLatitude,
                                                               @RequestParam(name = "neLongitude") double neLongitude,
                                                               @RequestParam(name = "pageSize") int pageSize) {
        return ResponseEntity.ok(cardService.getMapCardList(searchType, keyword, concept, swLatitude, neLatitude, swLongitude, neLongitude, pageSize));
    }
}
