package javaiscoffee.polaroad.post.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 카드 생성시 사용되는 Dto
 */
@Data
@Schema(description = "카드 정보 넘겨주는 Dto")
public class CardSaveDto {
    @Schema(description = "카드 Id \n 없어도 됩니다.", example = "0")
    private Long cardId;
    @Schema(description = "카드 순서 \n 없어도 됩니다.", example = "0")
    private int cardIndex;      // 자동으로 지정되는 값
    @Schema(description = "세부 위치", example = "인천시 남동구")
    private String location;// 사진 세부 위치
    @Schema(description = "위도 좌표", example = "123851.134521")
    private String latitude;//위도
    @Schema(description = "경도 좌표 \n", example = "543512.874521")
    private String longtitude;  //경도
    @Schema(description = "이미지 url", example = "https://krampolineImage.com/java-is-coffee")
    private String image;
    @Schema(description = "카드 본문", example = "여기가 꽃놀이 명소입니다.")
    private String content;
}
