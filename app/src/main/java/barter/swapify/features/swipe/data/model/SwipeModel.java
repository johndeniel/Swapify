package barter.swapify.features.swipe.data.model;

import java.util.ArrayList;
import java.util.List;

import barter.swapify.features.swipe.domain.entity.SwipeEntity;

public class SwipeModel extends SwipeEntity {

    public SwipeModel() {}

    public SwipeModel(String key, String userId, String avatar, String fullName, String title, String description, String fileName, String imageUrl) {
        super(key, userId, avatar, fullName, title, description, fileName, imageUrl);
    }

    public static List<SwipeEntity> toSwipeEntity(List<SwipeModel> items) {
        List<SwipeEntity> swipeEntity = new ArrayList<>();
        for (SwipeModel item : items) {
            SwipeEntity entity = new SwipeEntity(
                    item.getKey(),
                    item.getUserId(),
                    item.getAvatar(),
                    item.getFullName(),
                    item.getTitle(),
                    item.getDescription(),
                    item.getFileName(),
                    item.getImageUrl()
            );
            swipeEntity.add(entity);
        }

        return swipeEntity;
    }
}
