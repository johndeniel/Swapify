package barter.swapify.features.story.presentation.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

import barter.swapify.R;
import barter.swapify.core.widgets.shimmer.GlideShimmerHelper;
import barter.swapify.features.story.domain.entity.StoryEntity;
import de.hdodenhof.circleimageview.CircleImageView;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.ViewHolder> {
    private final List<StoryEntity> postList;

    public StoryAdapter(List<StoryEntity> postEntityList) {
        this.postList = postEntityList;
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_presentation_story_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoryEntity currentItem = postList.get(position);
        holder.bindItem(currentItem);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout linearLayout;
        private final TextView name;
        private final ShimmerFrameLayout shimmerFrameLayout;
        private final CircleImageView avatar;

        public ViewHolder(@NonNull View view) {
            super(view);
            avatar = view.findViewById(R.id.avatarImageView);
            name = view.findViewById(R.id.titleTextView);
            linearLayout= view.findViewById(R.id.story_thumbnail);
            shimmerFrameLayout = view.findViewById(R.id.StoryShimmerFrameLayout);

        }

        public void bindItem(@NonNull StoryEntity currentItem) {
            name.setText(currentItem.getUsername());

            Glide.with(itemView.getContext()).
                    load(currentItem.getAvatarURL())
                    .placeholder(R.drawable.rectangle)
                    .centerCrop()
                    .listener(new GlideShimmerHelper(shimmerFrameLayout))
                    .into(avatar);

            Glide.with(itemView.getContext())
                    .load(currentItem.getPhotoURL())
                    .placeholder(R.drawable.rectangle)
                    .centerCrop()
                    .listener(new GlideShimmerHelper(shimmerFrameLayout))
                    .into(new CustomImageViewTarget(linearLayout));
        }
    }
}
