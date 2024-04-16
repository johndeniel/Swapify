    package barter.swapify.features.swipe.domain.usecases;

    import java.util.List;
    import java.util.concurrent.CompletableFuture;

    import barter.swapify.core.errors.Failure;
    import barter.swapify.core.typedef.Either;
    import barter.swapify.core.usecase.UseCase;
    import barter.swapify.features.swipe.domain.entity.SwipeEntity;
    import barter.swapify.features.swipe.domain.repository.SwipeRepository;

    public class FetchUseCases implements UseCase<List<SwipeEntity>, String> {
        private final SwipeRepository swipeRepository;

        public FetchUseCases(SwipeRepository swipeRepository) {
            this.swipeRepository = swipeRepository;
        }

        @Override
        public CompletableFuture<Either<Failure, List<SwipeEntity>>> invoke(String s) {
            return swipeRepository.fetch(s);
        }
    }
