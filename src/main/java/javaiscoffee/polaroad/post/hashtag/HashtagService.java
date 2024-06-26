package javaiscoffee.polaroad.post.hashtag;

import javaiscoffee.polaroad.exception.BadRequestException;
import javaiscoffee.polaroad.post.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 포스트 생성, 수정할 때 사용되는 서비스
 */
@Service
@Slf4j
public class HashtagService {
    private final HashtagRepository hashtagRepository;  //해쉬태그 정보만 저장
    private final PostHashtagRepository postHashtagRepository;  //포스트의 해쉬태그 정보 저장

    @Autowired
    public HashtagService(HashtagRepository hashtagRepository, PostHashtagRepository postHashtagRepository) {
        this.hashtagRepository = hashtagRepository;
        this.postHashtagRepository = postHashtagRepository;
    }

    /**
     * 해쉬태그 종류를 생성하는 메서드
     * 새로운 해쉬태그를 저장할 때 사용
     */
    public Hashtag saveHashtag(String name) {
        Hashtag hashtag = new Hashtag();
        hashtag.setName(name);
        hashtagRepository.save(hashtag);
        return hashtag;
    }


    /**
     * 포스트 생성시 사용하는 해쉬태그 저장 메서드
     */
    public PostHashtag savePostHashtag(String tagName, Post post) {
        Hashtag hashtag = hashtagRepository.findByName(tagName)
                .orElseGet(() -> hashtagRepository.save(new Hashtag(tagName)));
        Optional<PostHashtag> existingPostHashtag = postHashtagRepository.findById(new PostHashtagId(hashtag.getHashtagId(), post.getPostId()));
        // 중복된 PostHashtag가 존재한다면 에러 처리
        if (existingPostHashtag.isPresent()) {
            log.info("중복된 PostHashtag 발견: {}", existingPostHashtag.get());
            throw new BadRequestException("해당 포스트에 이미 같은 해쉬태그가 존재합니다.");
        }
        PostHashtag postHashtag = new PostHashtag(new PostHashtagId(hashtag.getHashtagId(), post.getPostId()), hashtag, post);
        log.info("저장된 postHashtag = {}",postHashtag);
        return postHashtagRepository.save(postHashtag);
    }

    public List<PostHashtag> savePostHashtags(List<String> tagNames, Post post) {
        // 데이터베이스에서 기존에 존재하는 모든 해시태그 조회
        List<Hashtag> existingHashtags = hashtagRepository.findHashtagByNameIn(tagNames);
        Map<String, Hashtag> existingTagsMap = existingHashtags.stream()
                .collect(Collectors.toMap(Hashtag::getName, Function.identity()));

        List<PostHashtag> newPostHashtags = new ArrayList<>();
        List<Hashtag> newHashtags = new ArrayList<>();

        for (String tagName : tagNames) {
            // Map에서 해시태그를 가져오거나 새로 생성
            Hashtag hashtag = existingTagsMap.computeIfAbsent(tagName, name -> {
                Hashtag newHashtag = new Hashtag(name);
                newHashtags.add(newHashtag);
                return newHashtag;
            });

            // 새 PostHashtag 객체 생성
            PostHashtag postHashtag = new PostHashtag(new PostHashtagId(hashtag.getHashtagId(), post.getPostId()), hashtag, post);
            newPostHashtags.add(postHashtag);
        }

        // 새로운 해시태그를 데이터베이스에 일괄 저장
        if (!newHashtags.isEmpty()) {
            hashtagRepository.saveAll(newHashtags);
        }

        // 새로운 PostHashtag 객체들을 데이터베이스에 일괄 저장
        return postHashtagRepository.saveAll(newPostHashtags);
    }

    /**
     * 포스트 수정할 때 해쉬태그 수정하는 메서드
     */
    public List<PostHashtag> editPostHashtags(List<String> updatedHashtags, Post post) {
        List<PostHashtag> oldPostHashtag = postHashtagRepository.findByPost_PostId(post.getPostId());

        //기존 해쉬태그와 수정된 해쉬태그 리스트 비교해서 추가해야 할 해쉬태그만 골라내기
        Set<String> updatedTagNames = new HashSet<>(updatedHashtags);
        List<PostHashtag> toRemove = new ArrayList<>();
        oldPostHashtag.forEach(tag -> {
            if (!updatedTagNames.contains(tag.getHashtag().getName())) {
                toRemove.add(tag);
            } else {
                updatedHashtags.remove(tag.getHashtag().getName());
            }
        });
        oldPostHashtag.removeAll(toRemove);
        postHashtagRepository.deleteAll(toRemove);

        //새로 추가해야 할 해쉬태그 추가
        updatedHashtags.forEach(tagName -> {
            Hashtag hashtag = hashtagRepository.findByName(tagName)
                    .orElseGet(() -> hashtagRepository.save(new Hashtag(tagName)));
            PostHashtag postHashtag = new PostHashtag(new PostHashtagId(hashtag.getHashtagId(), post.getPostId()), hashtag, post);
            oldPostHashtag.add(postHashtagRepository.save(postHashtag));
        });
        return oldPostHashtag;
    }

    public Long getHashtagIdByName(String tagName) {
        Hashtag hashtag = hashtagRepository.findByName(tagName).orElse(null);
        if(hashtag==null) return null;
        return hashtag.getHashtagId();
    }
}
