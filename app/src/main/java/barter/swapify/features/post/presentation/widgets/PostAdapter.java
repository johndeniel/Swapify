package barter.swapify.features.post.presentation.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;
import barter.swapify.R;
import barter.swapify.core.widgets.shimmer.GlideShimmerHelper;
import barter.swapify.features.post.domain.entity.PostEntity;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private final List<PostEntity> postList;

    public PostAdapter(List<PostEntity> postEntityList) {
        this.postList = postEntityList;
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post_presentation_post_card_holder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PostEntity currentItem = postList.get(position);
        holder.bindItem(currentItem);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final ShimmerFrameLayout shimmerFrameLayout;

        public ViewHolder(@NonNull View view) {
            super(view);
            imageView = view.findViewById(R.id.post_image_viewer);
            shimmerFrameLayout = view.findViewById(R.id.view_post_shimmer);
        }

        public void bindItem(@NonNull PostEntity currentItem) {
            Glide.with(itemView.getContext()).
                    load(currentItem.getImageUrl())
                    .placeholder(R.drawable.rectangle)
                    .centerCrop()
                    .listener(new GlideShimmerHelper(shimmerFrameLayout))
                    .into(imageView);
        }
    }
}
