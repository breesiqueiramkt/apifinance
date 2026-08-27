package com.financeapp.service;

import com.financeapp.dto.CategoryDtos.CategoryRequest;
import com.financeapp.dto.CategoryDtos.CategoryResponse;
import com.financeapp.entity.Category;
import com.financeapp.entity.CategoryType;
import com.financeapp.entity.User;
import com.financeapp.exception.ResourceNotFoundException;
import com.financeapp.repository.CategoryRepository;
import com.financeapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    // categorias padrão descritas na seção 3 do briefing
    private static final List<Object[]> DEFAULTS = List.of(
            new Object[]{"Moradia", CategoryType.EXPENSE, "🏠"},
            new Object[]{"Alimentação", CategoryType.EXPENSE, "🍔"},
            new Object[]{"Transporte", CategoryType.EXPENSE, "🚗"},
            new Object[]{"Contas", CategoryType.EXPENSE, "💡"},
            new Object[]{"Internet/Telefone", CategoryType.EXPENSE, "📱"},
            new Object[]{"Compras", CategoryType.EXPENSE, "🛒"},
            new Object[]{"Educação", CategoryType.EXPENSE, "🎓"},
            new Object[]{"Saúde", CategoryType.EXPENSE, "🏥"},
            new Object[]{"Lazer", CategoryType.EXPENSE, "🎮"},
            new Object[]{"Vestuário", CategoryType.EXPENSE, "👕"},
            new Object[]{"Cartão de crédito", CategoryType.EXPENSE, "💳"},
            new Object[]{"Investimentos", CategoryType.EXPENSE, "💰"},
            new Object[]{"Outros", CategoryType.EXPENSE, "📦"},
            new Object[]{"Salário", CategoryType.INCOME, "💵"},
            new Object[]{"Freelance", CategoryType.INCOME, "💻"},
            new Object[]{"Comissão", CategoryType.INCOME, "🤝"},
            new Object[]{"Investimentos", CategoryType.INCOME, "📈"},
            new Object[]{"Negócio", CategoryType.INCOME, "🏢"},
            new Object[]{"Aluguel recebido", CategoryType.INCOME, "🏘️"},
            new Object[]{"Outros", CategoryType.INCOME, "📦"}
    );

    /** Seed idempotente: roda a cada registro de usuário, mas só insere o que faltar. */
    @Transactional
    public void ensureDefaultCategoriesExist() {
        for (Object[] def : DEFAULTS) {
            String name = (String) def[0];
            if (!categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
                categoryRepository.save(Category.builder()
                        .user(null)
                        .name(name)
                        .type((CategoryType) def[1])
                        .icon((String) def[2])
                        .isDefault(true)
                        .build());
            }
        }
    }

    public List<CategoryResponse> listForUser(Long userId) {
        return categoryRepository.findVisibleToUser(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        User userRef = userRepository.getReferenceById(userId);
        Category category = Category.builder()
                .user(userRef)
                .name(request.name())
                .type(request.type())
                .icon(request.icon())
                .isDefault(false)
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence a você"));
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getType(), c.getIcon(), c.isDefault());
    }
}
