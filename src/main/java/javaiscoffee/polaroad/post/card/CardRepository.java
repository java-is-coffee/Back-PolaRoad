package javaiscoffee.polaroad.post.card;

import javaiscoffee.polaroad.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    //포스트의 카드 조회
    List<Card> findCardsByPostAndStatusOrderByCardIndexAsc(Post post, CardStatus status);

    //마이페이지에서 자기가 업로드한 카도 조회
    @Query("select new javaiscoffee.polaroad.post.card.CardListDto(c.cardId, c.location, c.image) from Card c where  c.member.memberId = :memberId and" +
            " c.status = :status order by c.createdTime desc")
    Page<CardListDto> findCardsByMemberAndStatusOrderByCreatedTimeDesc(@Param("memberId") Long memberId,@Param("status") CardStatus status, Pageable pageable);

    //지도에서 범위 안 카드 리스트 조회
    @Query("select new javaiscoffee.polaroad.post.card.MapCardListDto(c.post.postId, c.cardId, c.image, c.content, c.location, c.latitude, c.longitude) " +
            "from Card c " +
            "where c.latitude >= :swLatitude and c.latitude <= :neLatitude and c.longitude >= :swLongitude and c.longitude <= :neLongitude " +
            "order by c.post.goodNumber desc, c.post.postId desc ")
    Slice<MapCardListDto> getMapCardList(double swLatitude, double neLatitude, double swLongitude, double neLongitude, Pageable pageable);
}
