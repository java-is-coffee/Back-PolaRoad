package javaiscoffee.polaroad.post;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import javaiscoffee.polaroad.post.card.CardSaveDto;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "포스트 생성 정보를 받는 requestDto")
public class PostSaveDto {

    @NotNull
    @NotBlank
    @Schema(description = "포스트 제목", example = "꽃놀이 명당 추천")
    private String title;

    @Schema(description = "전체 경로 직렬화", example = "좌표-좌표;좌표-좌표")
    private String routePoint;

    @Schema(description = "썸네일 번호", example = "0")
    private int thumbnailIndex;

    @NotNull
    @Schema(description = "포스트 메인 카테고리", example = "FOOD")
    private PostConcept concept;
    @NotNull
    @Schema(description = "여행 지역", example = "SEOUL")
    private PostRegion region;

    @Size(min = 1,max = 10)
    @NotNull
    @Schema(description = "포스트 카드 리스트")
    private List<CardSaveDto> cards;
    @Size(max = 10)
    @NotNull
    @Schema(description = "포스트 해쉬태그 리스트")
    private List<String> hashtags;
}
