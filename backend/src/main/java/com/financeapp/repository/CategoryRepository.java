package com.financeapp.repository;

import com.financeapp.entity.Category;
import com.financeapp.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // categorias padrão (user_id nulo) + as criadas pelo próprio usuário
    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user.id = :userId ORDER BY c.name")
    List<Category> findVisibleToUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.user IS NULL OR c.user.id = :userId) AND c.type = :type ORDER BY c.name")
    List<Category> findVisibleToUserByType(@Param("userId") Long userId, @Param("type") CategoryType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);
}
