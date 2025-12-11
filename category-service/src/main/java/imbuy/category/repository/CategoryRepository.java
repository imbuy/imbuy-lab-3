package imbuy.category.repository;

import imbuy.category.domain.Category;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CategoryRepository extends ReactiveCrudRepository<Category, Long> {

    Mono<Boolean> existsByNameAndParentId(String name, Long parentId);

    @Query("SELECT * FROM categories WHERE parent_id IS NULL")
    Flux<Category> findRootCategories();

    Flux<Category> findByParentId(Long parentId);
}