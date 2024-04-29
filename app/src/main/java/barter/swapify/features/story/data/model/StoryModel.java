package barter.swapify.features.story.data.model;

import java.util.ArrayList;
import java.util.List;

import barter.swapify.features.story.domain.entity.StoryEntity;
import barter.swapify.features.swipe.data.model.SwipeModel;
import barter.swapify.features.swipe.domain.entity.SwipeEntity;

public class StoryModel extends StoryEntity {
    public StoryModel(String username, String avatarURL, String photoURL) {
        super(username, avatarURL, photoURL);
    }


    public static List<StoryEntity> toStoryEntity(List<StoryModel> items) {
        List<StoryEntity> storyEntity = new ArrayList<>();
        for (StoryModel item : items) {
            StoryEntity entity = new StoryEntity(
                    item.getUsername(),
                    item.getAvatarURL(),
                    item.getPhotoURL()
            );
            storyEntity.add(entity);
        }

        return storyEntity;
    }
}
