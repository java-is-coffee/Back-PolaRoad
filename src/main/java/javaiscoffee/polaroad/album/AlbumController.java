package javaiscoffee.polaroad.album;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javaiscoffee.polaroad.album.albumCard.AlbumCardInfoDto;
import javaiscoffee.polaroad.album.albumCard.RequestToAddAlbumCardDto;
import javaiscoffee.polaroad.member.Member;
import javaiscoffee.polaroad.response.ResponseMessages;
import javaiscoffee.polaroad.response.Status;
import javaiscoffee.polaroad.security.CustomUserDetails;
import javaiscoffee.polaroad.wrapper.RequestWrapperDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * ---- ApiResponse 정리 필요 ----
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/album")
@Tag(name = "앨범 관련 API", description = "앨범에 관련된 API 모음")
public class AlbumController {
    private final AlbumService albumService;


    @Operation(summary = "앨범 생성 API", description = "앨범 생성할 때 사용하는 API \n 앨범 카드 리스트는 nullable")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "앨범 생성 성공한 경우"),
            @ApiResponse(responseCode = "400", description = "입력값이 잘못된 경우"),
            @ApiResponse(responseCode = "403", description = "권한이 없는 경우"),
            @ApiResponse(responseCode = "404", description = "멤버가 존재하지 않는 경우")
    })
    @PostMapping("/create")
    public ResponseEntity<?> createAlbum(@RequestBody RequestWrapperDto<AlbumDto> requestWrapperDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        AlbumDto albumDto = requestWrapperDto.getData();
        log.info("앨범 생성 요청 = {}", albumDto);
        ResponseAlbumDto responseAlbumDto = albumService.createAlbum(albumDto, memberId);
        if (responseAlbumDto == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Status(ResponseMessages.INPUT_ERROR));
        }
        return ResponseEntity.ok(responseAlbumDto);
    }

    @Operation(summary = "앨범 1개 조회 API", description = "앨범 1개 조회할 때 사용하는 API")
    @GetMapping("/{albumId}")
    public ResponseEntity<?> getAlbum(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable(name = "albumId") Long albumId) {
        log.info("댓글 조회 요청");
        Long memberId = userDetails.getMemberId();
        ResponseAlbumDto responseAlbumDto = albumService.getAlbum(memberId, albumId);
        return ResponseEntity.ok(responseAlbumDto);
    }


    @Operation(summary = "앨범 수정 API", description = "앨범 수정할 때 사용하는 API")
    @PatchMapping("/edit/{albumId}")
    public ResponseEntity<?> editAlbum(@RequestBody RequestWrapperDto<EditAlbumDto> requestWrapperDto, @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable(name = "albumId") Long albumId) {
        Long memberId = userDetails.getMemberId();
        EditAlbumDto editAlbumDto = requestWrapperDto.getData();
        log.info("앨범 수정 요청 = {}", editAlbumDto);
        ResponseAlbumDto responseAlbumDto = albumService.editAlbum(editAlbumDto, albumId, memberId);
        return ResponseEntity.ok(responseAlbumDto);
    }

    // 삭제 성공시 response body에 " can't parse JSON.  Raw result: 성공 " 이라고 나옴
    @Operation(summary = "앨범 삭제 API", description = "앨범 삭제할 때 사용하는 API")
    @DeleteMapping("/delete/{albumId}")
    public ResponseEntity<String> deleteAlbum(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable(name = "albumId") Long albumId) {
        log.info("앨범 삭제 요청");
        Long memberId = userDetails.getMemberId();
        return albumService.deleteAlbum(memberId, albumId);
    }

    // 미완성!!
    @Operation(summary = "앨범 카드 추가 API", description = "앨범에 카드 추가할 때 사용하는 API")
    @PostMapping("/add-card/{albumId}")
    public ResponseEntity<?> addCardToAlbum(@RequestBody RequestWrapperDto<RequestToAddAlbumCardDto> requestWrapperDto, @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable(name = "albumId") Long albumId) {
        Long memberId = userDetails.getMemberId();
        RequestToAddAlbumCardDto addAlbumCardDto = requestWrapperDto.getData();
        log.info("앨범 사진 추가 요청 = {}", addAlbumCardDto);
        ResponseAlbumDto responseAlbumDto = albumService.addCard(addAlbumCardDto, albumId, memberId);
        return ResponseEntity.ok(responseAlbumDto);
    }
}
