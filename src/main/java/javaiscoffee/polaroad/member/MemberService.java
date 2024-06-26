package javaiscoffee.polaroad.member;

import javaiscoffee.polaroad.exception.BadRequestException;
import javaiscoffee.polaroad.exception.NotFoundException;
import javaiscoffee.polaroad.post.PostStatus;
import javaiscoffee.polaroad.post.PostThumbnailDto;
import javaiscoffee.polaroad.post.card.CardRepository;
import javaiscoffee.polaroad.response.ResponseMessages;
import javaiscoffee.polaroad.security.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CardRepository cardRepository;

    /**
     * 현재 사용 용도 : 자기 이메일을 통해 자기 정보를 반환
     * 요구 데이터 : 정보를 조회할 이메일
     * 반환 데이터 : 이메일에 해당하는 MemberInformationDto
     */
    public MemberInformationResponseDto getMemberInformation(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new BaseException(ResponseMessages.NOT_FOUND.getMessage()));
        if (member.getStatus() != MemberStatus.DELETED) {
            log.info("정보 조회하려고 찾은 멤버 = {}", member);

            // MemberInformationDto의 내부 Data 객체를 생성하고 정보 복사
            MemberInformationResponseDto responseDto = new MemberInformationResponseDto();
            BeanUtils.copyProperties(member, responseDto);

            return responseDto;
        } else {
            log.error("해당 이메일을 가진 멤버가 없습니다: {}", email);
            return null;
        }
    }

    /**
     * 사용 용도 : 마이페이지에서 정보 수정
     * 요구 데이터 : MemberInformationDto
     * 반환 데이터 : 받은 정보로 수정된 MemberInformationDto
     */
    @Transactional
    public MemberInformationResponseDto updateMemberInformation(String email, MemberInformationRequestDto memberInformationRequestDto) {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new BaseException(ResponseMessages.NOT_FOUND.getMessage()));

        //멤버 조회 실패했을 경우 (포스트맨으로 다르게 보내는 경우 등), 멤버가 삭제된 멤버인 경우
        if(!Objects.equals(member.getMemberId(), memberInformationRequestDto.getMemberId())
                || !member.getEmail().equals(memberInformationRequestDto.getEmail()) || member.getStatus() == MemberStatus.DELETED) {
            throw new BadRequestException(ResponseMessages.BAD_REQUEST.getMessage());
        }

        //멤버 정보 수정
        member.setName(memberInformationRequestDto.getName());
        member.setNickname(memberInformationRequestDto.getNickname());
        member.setProfileImage(memberInformationRequestDto.getProfileImage());
        //수정일 업데이트
        member.setUpdatedTime(LocalDateTime.now());

        //새로운 멤버로 response 생성
        MemberInformationResponseDto updatedDto = new MemberInformationResponseDto();
        BeanUtils.copyProperties(member, updatedDto); // 멤버 정보를 Data 객체에 복사

        log.info("수정된 멤버 정보 = {}",updatedDto);

        return updatedDto;
    }

    /**
     * 용도 : 마이페이지에서 비밀번호 재설정
     * 요구 데이터 : 패스워드와 토큰에 있던 이메일
     * 반환 데이터 : 성공했다는 status만 가지고 있는 MyResponse
     */
    @Transactional
    public Boolean resetPassword(String email, String password) {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new BaseException(ResponseMessages.NOT_FOUND.getMessage()));
        if(member.getStatus() == MemberStatus.DELETED) {
            return null;
        }

        log.info("입력받은 비밀번호 = {}",password);
        member.setPassword(password);
        member.hashPassword(passwordEncoder);
        log.info("변경된 비밀번호를 담은 멤버 = {}",member);

        return true;
    }

    /**
     * 사용 용도 : 다른 멤버 팔로우
     * 요구 데이터 : followingMemberId, followedMemberId
     * 반환 데이터 : 없음
     */
    @Transactional
    public void toggleFollow(Long followingMemberId, Long followedMemberId) {
        if(!Objects.equals(followingMemberId, followedMemberId)) {
            Member followingMember = memberRepository.findByMemberId(followingMemberId).orElseThrow(() -> new NotFoundException(ResponseMessages.NOT_FOUND.getMessage()));
            Member followedMember = memberRepository.findByMemberId(followedMemberId).orElseThrow(() -> new NotFoundException(ResponseMessages.NOT_FOUND.getMessage()));
            FollowId followId = new FollowId(followingMember.getMemberId(), followedMember.getMemberId());
            Follow follow = memberRepository.findMemberFollow(followId);
            
            if (followingMember.getStatus() == MemberStatus.ACTIVE && followedMember.getStatus() == MemberStatus.ACTIVE && !Objects.equals(followedMemberId, followingMemberId)) {
                if (follow == null) {
                    follow = new Follow(followId, followingMember, followedMember);
                    memberRepository.saveMemberFollow(follow);
                    followingMember.setFollowingNumber(followingMember.getFollowingNumber() + 1);
                    followedMember.setFollowedNumber(followedMember.getFollowedNumber() + 1);
                    log.info("팔로잉 = {}",followingMember.getFollowingNumber());
                    log.info("팔로워 = {}",followedMember.getFollowedNumber());
                }
                else {
                    if (!(memberRepository.deleteMemberFollow(follow))) {
                        throw new BadRequestException(ResponseMessages.BAD_REQUEST.getMessage());
                    }

                    followingMember.setFollowingNumber(followingMember.getFollowingNumber() - 1);
                    followedMember.setFollowedNumber(followedMember.getFollowedNumber() - 1);
                    log.info("팔로잉 = {}",followingMember.getFollowingNumber());
                    log.info("팔로워 = {}",followedMember.getFollowedNumber());
                }
            }
            else {
                throw new NotFoundException(ResponseMessages.NOT_FOUND.getMessage());
            }
        } else {
            throw new BadRequestException(ResponseMessages.BAD_REQUEST.getMessage());
        }
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteAccount(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId).orElseThrow(() -> new NotFoundException(ResponseMessages.NOT_FOUND.getMessage()));
        if(!member.getStatus().equals(MemberStatus.ACTIVE)) throw new BadRequestException(ResponseMessages.BAD_REQUEST.getMessage());
        member.deleteMember();
    }

    /**
     * 팔로잉하고 있는 멤버들 정보 조회
     */
    public FollowingMemberResponseDto getFollowingMemberInfo (Long memberId,int page, int pageSize) {
        return memberRepository.getFollowingMemberInfo(memberId,page,pageSize);
    }

    /**
     * 멤버 미니프로필 조회
     */
    public MemberMiniProfileDto getMiniProfile (Long targetId, Long memberId) {
        MemberBasicInfoDto basicInfo = memberRepository.getMemberMiniProfileDto(targetId);
        if(!basicInfo.getStatus().equals(MemberStatus.ACTIVE)) throw new NotFoundException(ResponseMessages.MEMBER_NOT_FOUND.getMessage());
        List<PostThumbnailDto> thumbnails = cardRepository.getMiniProfileImages(targetId, PostStatus.ACTIVE);
        return new MemberMiniProfileDto(
                basicInfo.getName(),
                basicInfo.getNickname(),
                basicInfo.getProfileImage(),
                basicInfo.getPostNumber(),
                basicInfo.getFollowedNumber(),
                basicInfo.getFollowingNumber(),
                memberRepository.existsFollowing(memberId, targetId),
                thumbnails
        );
    }

    public boolean checkFollowing (Long memberId, Long targetId) {
        return memberRepository.existsFollowing(memberId, targetId);
    }
}
