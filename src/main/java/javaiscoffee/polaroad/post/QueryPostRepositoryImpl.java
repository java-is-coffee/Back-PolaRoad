package javaiscoffee.polaroad.post;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ext.javaiscoffee.polaroad.member.QFollow;
import ext.javaiscoffee.polaroad.member.QMember;
import ext.javaiscoffee.polaroad.post.QPost;
import ext.javaiscoffee.polaroad.post.card.QCard;
import ext.javaiscoffee.polaroad.post.hashtag.QHashtag;
import ext.javaiscoffee.polaroad.post.hashtag.QPostHashtag;
import jakarta.persistence.EntityManager;
import javaiscoffee.polaroad.post.card.CardListRepositoryDto;
import javaiscoffee.polaroad.post.card.CardStatus;

import java.util.*;
import java.util.stream.Collectors;

public class QueryPostRepositoryImpl implements QueryPostRepository{

    private final JPAQueryFactory queryFactory;
    public QueryPostRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Post> findPostByEmail(String email) {
        QPost post = QPost.post; // Querydsl QClass
        return queryFactory.selectFrom(post)
                .where(post.member.email.eq(email))
                .orderBy(post.createdTime.desc())
                .fetch();
    }

    /**
     * 검색어로 포스트 목록 조회
     */
    @Override
    public PostListResponseDto searchPostByKeyword(int page, int pageSize, String searchKeyword, PostListSort sortBy, PostConcept concept, PostRegion region, PostStatus status) {
        QPost post = QPost.post;
        QCard card = QCard.card;
        QMember member = QMember.member; // 멤버와 관련된 쿼리를 위한 QClass

        BooleanBuilder builder = new BooleanBuilder();
        //검색어 조건 추가
        if(searchKeyword != null && !searchKeyword.isEmpty()) {
            BooleanBuilder searchBuilder = new BooleanBuilder(); // 새로운 BooleanBuilder로 검색 조건 생성
            searchBuilder.or(post.title.containsIgnoreCase(searchKeyword)); // 포스트 제목에 검색어가 포함되는 경우
            searchBuilder.or(card.content.containsIgnoreCase(searchKeyword)); // 카드 컨텐츠에 검색어가 포함되는 경우
            searchBuilder.or(post.member.nickname.containsIgnoreCase(searchKeyword)); // 멤버 닉네임에 검색어가 포함되는 경우
            builder.and(searchBuilder); // 검색어 조건 추가
        }
        //여행컨셉 조건 추가
        //인기 게시글 검색 아닐 때 조건 추가
        if(concept != null && !concept.equals(PostConcept.HOT)) {
            builder.and(post.concept.eq(concept));
        }
        //인기 게시글 검색 조건 추가
        else if(concept != null) {
            builder.and(post.goodNumber.goe(10));
        }
        //여행지역 조건 추가
        if(region != null) {
            builder.and(post.region.eq(region));
        }
        //게시글 상태 조건 추가
        builder.and(post.status.eq(status));

        //검색 결과 최대 개수 구하기
        Long totalPostsCount = queryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.cards, card)
                .leftJoin(post.member, member)
                .where(builder)
                .fetchOne();
        //검색 결과가 없으므로 빈 배열 반환
        if(totalPostsCount == null) return new PostListResponseDto(new ArrayList<>(),0);

        // 1. 포스트 목록 조회
        JPAQuery<PostListRepositoryDto> query = queryFactory
                .select(getPostListRepositoryDtoConstructor(post, member))
                .from(post)
                .leftJoin(post.cards, card)
                .leftJoin(post.member, member)
                .where(builder)
                .groupBy(post.postId)
                .offset(getOffset(page, pageSize))
                .limit(pageSize);
        //최신순 정렬
        if (sortBy.equals(PostListSort.RECENT)) {
            query.orderBy(post.createdTime.desc());
        }
        //인기순 정렬
        else {
            query.orderBy(post.goodNumber.desc(),post.createdTime.desc());
        }

        List<PostListRepositoryDto> posts = query.fetch();

        // 2. 포스트들의 카드 정보 조회
        List<Long> postIds = getPostIds(posts);
        // 카드들을 맵으로 변경
        Map<Long, List<CardListRepositoryDto>> cardsMap = getPostCardsMap(card, postIds);

        // 3. DTO에 카드 정보 추가
        setCardInfoToPostDto(posts, cardsMap);

        int maxPage = (int) Math.ceil((double) totalPostsCount / pageSize);

        // 포스트를 DTO로 변환하고 카드 이미지 처리
        return getPostListResponseDto(posts, maxPage);
    }

    private static ConstructorExpression<PostListRepositoryDto> getPostListRepositoryDtoConstructor(QPost post, QMember member) {
        return Projections.constructor(
                PostListRepositoryDto.class,
                post.title,
                post.postId,
                member.nickname,
                post.thumbnailIndex,
                post.goodNumber,
                post.concept,
                post.region,
                post.updatedTime
        );
    }

    /**
     * 해쉬 태그로 포스트 목록 조회
     */
    @Override
    public PostListResponseDto searchPostByHashtag(int page, int pageSize, Long hashtagId, PostListSort sortBy, PostConcept concept, PostRegion region, PostStatus status) {
        QPost post = QPost.post;
        QCard card = QCard.card;
        QMember member = QMember.member; // 멤버와 관련된 쿼리를 위한 QClass
        QPostHashtag postHashtag = QPostHashtag.postHashtag;

        BooleanBuilder builder = new BooleanBuilder();
        // 해쉬태그 검색 조건 추가
        if(hashtagId != null) {
            builder.and(postHashtag.hashtag.hashtagId.eq(hashtagId));
        }
        //여행컨셉 조건 추가
        //인기 게시글 검색 아닐 때 조건 추가
        if(concept != null && !concept.equals(PostConcept.HOT)) {
            builder.and(post.concept.eq(concept));
        }
        //인기 게시글 검색 조건 추가
        else if(concept != null) {
            builder.and(post.goodNumber.goe(10));
        }
        //여행지역 조건 추가
        if(region != null) {
            builder.and(post.region.eq(region));
        }
        //게시글 상태 조건 추가
        builder.and(post.status.eq(status));

        //검색 결과 최대 개수 구하기
        Long totalPostsCount = queryFactory
                .select(post.count())
                .from(post)
                .leftJoin(post.cards, card)
                .leftJoin(post.member, member)
                .leftJoin(post.postHashtags, postHashtag)
                .where(builder)
                .fetchOne();
        //검색 결과가 없으므로 빈 배열 반환
        if(totalPostsCount == null) return new PostListResponseDto(new ArrayList<>(),0);
        int maxPage = (int) Math.ceil((double) totalPostsCount / pageSize);

        JPAQuery<PostListRepositoryDto> query = queryFactory
                .select(getPostListRepositoryDtoConstructor(post, member))
                .from(post)
                .leftJoin(post.cards, card).fetchJoin()
                .leftJoin(post.member, member)
                .leftJoin(post.postHashtags, postHashtag)
                .where(builder)
                .groupBy(post.postId)
                .offset(getOffset(page, pageSize))
                .limit(pageSize);

        //최신순 정렬
        if (sortBy.equals(PostListSort.RECENT)) {
            query.orderBy(post.createdTime.desc());
        }
        //인기순 정렬
        else {
            query.orderBy(post.goodNumber.desc(),post.createdTime.desc());
        }

        List<PostListRepositoryDto> posts = query.fetch();

        // 2. 포스트들의 카드 정보 조회
        List<Long> postIds = getPostIds(posts);

        Map<Long, List<CardListRepositoryDto>> cardsMap = getPostCardsMap(card, postIds);

        // 3. DTO에 카드 정보 추가
        setCardInfoToPostDto(posts, cardsMap);

        // 포스트를 DTO로 변환하고 카드 이미지 처리
        return getPostListResponseDto(posts,maxPage);
    }

    private static void setCardInfoToPostDto(List<PostListRepositoryDto> posts, Map<Long, List<CardListRepositoryDto>> cardsMap) {
        posts.forEach(p -> {
            List<CardListRepositoryDto> cardsForPost = cardsMap.getOrDefault(p.getPostId(), Collections.emptyList());
            p.setCards(cardsForPost);
        });
    }


    //팔로잉하고 있는 멤버 포스트 목록 조회
    @Override
    public PostListResponseDto getFollowingMembersPostByMember(Long memberId,int page, int pageSize, PostStatus status) {
        QPost post = QPost.post;
        QCard card = QCard.card;
        QFollow follow = QFollow.follow;

        List<Long> followingMemberIds = queryFactory
                .select(follow.followedMember.memberId)
                .from(follow)
                .where(follow.followingMember.memberId.eq(memberId))
                .fetch();

        // 검색 결과의 총 개수 구하기
        Long totalPostsCount = queryFactory
                .select(post.count())
                .from(post)
                .where(
                        post.member.memberId.in(followingMemberIds),
                        post.status.eq(status)
                )
                .fetchOne();
        //검색 결과가 없으므로 빈 배열 반환
        if(totalPostsCount == null) return new PostListResponseDto(new ArrayList<>(),0);

        // 팔로잉하는 멤버의 포스트를 조회
        List<PostListRepositoryDto> posts = queryFactory
                .select(getPostListRepositoryDtoConstructor(post, post.member))
                .from(post)
                .where(
                        post.member.memberId.in(followingMemberIds),
                        post.status.eq(status)
                )
                .offset(getOffset(page, pageSize))
                .limit(pageSize)
                .fetch();

        // 2. 포스트들의 카드 정보 조회
        List<Long> postIds = getPostIds(posts);

        Map<Long, List<CardListRepositoryDto>> cardsMap = getPostCardsMap(card, postIds);

        // 3. DTO에 카드 정보 추가
        setCardInfoToPostDto(posts, cardsMap);

        int maxPage = (int) Math.ceil((double) totalPostsCount / pageSize);

        // 포스트를 DTO로 변환하고 카드 이미지 처리
        return getPostListResponseDto(posts, maxPage);
    }

    @Override
    public Post getPostInfoById(Long postId) {
        QPost post = QPost.post;
        QMember member = QMember.member;
        QCard card = QCard.card;
        QPostHashtag postHashtag = QPostHashtag.postHashtag;
        QHashtag hashtag = QHashtag.hashtag;

        return queryFactory
                .selectFrom(post)
                .leftJoin(post.member, member)
                .leftJoin(post.cards, card).fetchJoin()
                .leftJoin(post.postHashtags, postHashtag)
                .leftJoin(postHashtag.hashtag, hashtag)
                .where(post.postId.eq(postId))
                .fetchOne();
    }

    private static List<Long> getPostIds(List<PostListRepositoryDto> posts) {
        return posts.stream()
                .map(PostListRepositoryDto::getPostId)
                .collect(Collectors.toList());
    }
    // 포스트의 카드 리스트를 카드 맵으로 바꾸기
    private Map<Long, List<CardListRepositoryDto>> getPostCardsMap(QCard card, List<Long> postIds) {
        return queryFactory
                .select(Projections.constructor(CardListRepositoryDto.class,
                        card.post.postId,
                        card.cardIndex,
                        card.image))
                .from(card)
                .where(card.post.postId.in(postIds),
                        card.status.eq(CardStatus.ACTIVE))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(CardListRepositoryDto::getPostId));
    }

    //포스트 리스트를 DTO로 변환하고 카드 이미지에서 썸네일을 제일 앞으로 설정
    private PostListResponseDto getPostListResponseDto(List<PostListRepositoryDto> posts, int maxPage) {
        return new PostListResponseDto(posts.stream().map(p -> {
            List<String> images = p.getCards().stream()
                    .sorted(Comparator.comparingInt(CardListRepositoryDto::getCardIndex))
                    .map(CardListRepositoryDto::getImage)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.toList());

            // 썸네일 이미지가 없으면 맨 앞에 추가
            String thumbnailImage = p.getCards().get(p.getThumbnailIndex()).getImage();
            if (!images.contains(thumbnailImage)) {
                images.add(0, thumbnailImage); // 맨 앞에 썸네일 이미지 추가
                if (images.size() > 3) {
                    images = images.subList(0, 3); // 최대 3개 이미지 유지
                }
            }
            //썸네일 이미지가 있으면 맨 앞으로 옮기기
            else {
                images.remove(thumbnailImage);
                images.add(0, thumbnailImage);
            }

            return new PostListDto(
                    p.getTitle(),
                    p.getPostId(),
                    p.getNickname(),
                    p.getGoodNumber(),
                    p.getConcept(),
                    p.getRegion(),
                    images,
                    p.getUpdatedTime()
            );
        }).collect(Collectors.toList()), maxPage);
    }

    private int getOffset(int page, int pageSize) {
        return (page - 1) * pageSize;
    }
}
