package javaiscoffee.polaroad.post.wishlist;

import java.util.List;

public interface QueryWishListRepository {
    WishListPostListResponseDto getWishListPostDtos(Long wishListId,int paging, int pagingNumber);
}
