package javaiscoffee.polaroad.member;

import javaiscoffee.polaroad.post.PostMemberInfoDto;

import java.util.Optional;

public interface MemberRepository {
    public Optional<Member> findByEmail(String email);  //이메일로 Member 찾기
    boolean existsByEmail(String email);    //이메일로 Member 존재 여부 확인

    //이메일로 Member 찾기 spring security 위해 추가
    Optional<Member> findByMemberId(Long memberId);

    public Optional<Member> findById(Long id);

    public Follow findMemberFollow(FollowId id);
    public Follow saveMemberFollow(Follow follow);
    public boolean deleteMemberFollow(Follow follow);

    public void save(Member newMember);

    public void delete(Member member);
    public Member updateMember(Member updatedMember);

    //간략하게 정보 조회할 때 사용하는 메서드
    public Optional<MemberSimpleInfoDto> getMemberSimpleInfo(Long memberId);
    //포스트 멤버 정보 조회할 때 사용하는 메서드
    public PostMemberInfoDto getPostMemberInfoByMemberId(Long memberId);
    public void addMemberPostNumber(Long memberId, int changeNumber);
    //팔로잉하고 있는 멤버 리스트 조회
    FollowingMemberResponseDto getFollowingMemberInfo(Long followingMemberId, int page, int pageSize);
    //미니프로필 조회
    MemberBasicInfoDto getMemberMiniProfileDto(Long memberId);
    //팔로우 존재 조회
    boolean existsFollowing(Long memberId, Long targetId);
}


