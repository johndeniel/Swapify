package barter.swapify.features.post.data.model;

import java.util.ArrayList;
import java.util.List;

import barter.swapify.features.post.domain.entity.PostEntity;

public class PostModel extends PostEntity {
    public PostModel() {};
    public PostModel(String key, String userId, String avatar, String fullName, String title, String description, String fileName, String imageUrl) {
        super(key, userId, avatar, fullName, title, description, fileName, imageUrl);

    }

    public static List<PostEntity> toPostEntity(List<PostModel> items) {
        List<PostEntity> postEntity = new ArrayList<>();
        for (PostModel item : items) {
            PostEntity entity = new PostEntity(
                    item.getKey(),
                    item.getUserId(),
                    item.getAvatar(),
                    item.getFullName(),
                    item.getTitle(),
                    item.getDescription(),
                    item.getFileName(),
                    item.getImageUrl()
            );
            postEntity.add(entity);
        }

        return postEntity;
    }
}
